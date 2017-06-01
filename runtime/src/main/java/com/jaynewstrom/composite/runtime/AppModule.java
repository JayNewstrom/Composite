package com.jaynewstrom.composite.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AppModule {
    // The fully qualified type name the app module should generate.
    String value();

    // The fully qualified type names of the library modules the app module should exclude.
    String[] excludes() default {};
}
