/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Taken from dagger.
 */

package com.jaynewstrom.composite.compiler;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;

final class Util {
    public static Map<String, Object> getAnnotation(Class<?> annotationType, Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (!rawTypeToString(annotation.getAnnotationType()).equals(annotationType.getName())) {
                continue;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            for (Method m : annotationType.getMethods()) {
                result.put(m.getName(), m.getDefaultValue());
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : annotation.getElementValues().entrySet()) {
                String name = e.getKey().getSimpleName().toString();
                Object value = e.getValue().accept(VALUE_EXTRACTOR, null);
                result.put(name, value);
            }
            return result;
        }
        return null; // Annotation not found.
    }

    private static final AnnotationValueVisitor<Object, Void> VALUE_EXTRACTOR =
            new SimpleAnnotationValueVisitor6<Object, Void>() {
                @Override public Object visitString(String s, Void p) {
                    if ("<error>".equals(s)) {
                        throw new CodeGenerationIncompleteException("Unknown type returned as <error>.");
                    } else if ("<any>".equals(s)) {
                        throw new CodeGenerationIncompleteException("Unknown type returned as <any>.");
                    }
                    return s;
                }

                @Override public Object visitType(TypeMirror t, Void p) {
                    return t;
                }

                @Override protected Object defaultAction(Object o, Void v) {
                    return o;
                }

                @Override public Object visitArray(List<? extends AnnotationValue> values, Void v) {
                    Object[] result = new Object[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        result[i] = values.get(i).accept(this, null);
                    }
                    return result;
                }
            };

    private static final class CodeGenerationIncompleteException extends IllegalStateException {
        CodeGenerationIncompleteException(String s) {
            super(s);
        }
    }

    private static String rawTypeToString(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            throw new IllegalArgumentException("Unexpected type: " + type);
        }
        StringBuilder result = new StringBuilder();
        DeclaredType declaredType = (DeclaredType) type;
        rawTypeToString(result, (TypeElement) declaredType.asElement());
        return result.toString();
    }

    private static void rawTypeToString(StringBuilder result, TypeElement type) {
        String packageName = getPackage(type).getQualifiedName().toString();
        String qualifiedName = type.getQualifiedName().toString();
        if (packageName.isEmpty()) {
            result.append(qualifiedName.replace('.', '$'));
        } else {
            result.append(packageName);
            result.append('.');
            result.append(
                    qualifiedName.substring(packageName.length() + 1).replace('.', '$'));
        }
    }

    private static PackageElement getPackage(Element type) {
        while (type.getKind() != ElementKind.PACKAGE) {
            type = type.getEnclosingElement();
        }
        return (PackageElement) type;
    }
}
