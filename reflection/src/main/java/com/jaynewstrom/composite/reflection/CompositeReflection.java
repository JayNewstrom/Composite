package com.jaynewstrom.composite.reflection;

import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;

import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CompositeReflection {
    private static final Reflections REFLECTIONS = new Reflections("com.jaynewstrom.composite.generated");

    public static <M> Set<M> modules(Class<M> moduleClass) {
        String moduleClassName = moduleClass.getName();
        Set<M> modules = new LinkedHashSet<M>();
        Set<Class<?>> libraryModuleIndexers = REFLECTIONS.getTypesAnnotatedWith(LibraryModuleIndexer.class);
        for (Class<?> libraryModuleIndexerClass : libraryModuleIndexers) {
            for (Annotation annotation : libraryModuleIndexerClass.getDeclaredAnnotations()) {
                M module = moduleFromAnnotation(annotation, moduleClassName);
                if (module != null) {
                    modules.add(module);
                }
            }
        }
        return modules;
    }

    public static <M> M module(Class<M> moduleClass) {
        String moduleClassName = moduleClass.getName();
        Set<Class<?>> libraryModuleIndexers = REFLECTIONS.getTypesAnnotatedWith(LibraryModuleIndexer.class);
        for (Class<?> libraryModuleIndexerClass : libraryModuleIndexers) {
            for (Annotation annotation : libraryModuleIndexerClass.getDeclaredAnnotations()) {
                M module = moduleFromAnnotation(annotation, moduleClassName);
                if (module != null) {
                    return module;
                }
            }
        }
        throw new IllegalStateException("Module not found.");
    }

    @SuppressWarnings("unchecked")
    private static <M> M moduleFromAnnotation(Annotation annotation, String moduleClassName) {
        if (annotation instanceof LibraryModuleIndexer) {
            LibraryModuleIndexer libraryModuleIndexer = (LibraryModuleIndexer) annotation;
            if (libraryModuleIndexer.value().equals(moduleClassName)) {
                try {
                    return (M) Class.forName(libraryModuleIndexer.libraryModule()).newInstance();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
