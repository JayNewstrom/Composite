package com.jaynewstrom.composite.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface LibraryModule {
    /**
     * The fully qualified type name of the contributingToType.
     */
    String value();
}
