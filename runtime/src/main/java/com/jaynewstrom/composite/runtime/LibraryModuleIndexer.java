package com.jaynewstrom.composite.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Should only be used by generated code.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LibraryModuleIndexer {
    /**
     * The fully qualified type name for the contributingToType of the library module.
     */
    String value();

    /**
     * The fully qualified type name of the library module.
     */
    String libraryModule();
}
