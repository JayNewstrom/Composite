package com.jaynewstrom.composite.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AppModule {
    /**
     * The fully qualified type names of the contributingToTypes the app module should generate.
     */
    String[] value();

    /**
     * Whether the generated module should only include a single module of the contributingToTypes, checked at compile time.
     */
    boolean single() default false;

    /**
     * The fully qualified type names of the libraryModules the app module should exclude.
     */
    String[] excludes() default {};
}
