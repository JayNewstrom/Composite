package com.jaynewstrom.composite.reflection;

import android.content.Context;

import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import dalvik.system.DexFile;

public final class CompositeReflection {
    public static <M> Set<M> modules(Context context, Class<M> moduleClass) {
        String moduleClassName = moduleClass.getName();
        Set<M> modules = new LinkedHashSet<M>();
        for (Class<?> libraryModuleIndexerClass : libraryModuleIndexers(context)) {
            for (Annotation annotation : libraryModuleIndexerClass.getDeclaredAnnotations()) {
                M module = moduleFromAnnotation(annotation, moduleClassName);
                if (module != null) {
                    modules.add(module);
                }
            }
        }
        return modules;
    }

    public static <M> M module(Context context, Class<M> moduleClass) {
        String moduleClassName = moduleClass.getName();
        for (Class<?> libraryModuleIndexerClass : libraryModuleIndexers(context)) {
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

    private static Set<Class<?>> libraryModuleIndexers(Context context) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        try {
            String packageCodePath = context.getPackageCodePath();
            DexFile dexFile = new DexFile(packageCodePath);
            Enumeration<String> enumeration = dexFile.entries();
            while (enumeration.hasMoreElements()) {
                String className = enumeration.nextElement();
                if (className.startsWith("com.jaynewstrom.composite.generated.")) {
                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return classes;
    }
}
