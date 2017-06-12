package com.jaynewstrom.composite.compiler

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory.javaSources
import org.junit.Test
import java.util.Arrays

class CompositeProcessorTest {
    @Test fun testMultipleAppModules() {
        val runnableLibraryModule = JavaFileObjects.forSourceString("com.example.FooRunnable", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |import java.lang.Runnable;
                |@LibraryModule(Runnable.class)
                |public final class FooRunnable implements Runnable {
                |    @Override public void run() {
                |    }
                |}
                |""".trimMargin())

        val runnableAppModule = JavaFileObjects.forSourceString("com.example.RunnableAppModule", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.AppModule;
                |import java.lang.Runnable;
                |@AppModule(Runnable.class)
                |public final class RunnableAppModule {
                |}
                |""".trimMargin())

        val expectedRunnableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_com_example_FooRunnable", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "java.lang.Runnable", libraryModule = "com.example.FooRunnable")
                |public final class LibraryModuleIndexer_com_example_FooRunnable {
                |}
                |""".trimMargin())

        val expectedGeneratedRunnableModule = JavaFileObjects.forSourceLines("com.example.GeneratedRunnableModule", """
                |package com.example;
                |
                |import java.lang.Runnable;
                |import java.util.LinkedHashSet;
                |import java.util.Set;
                |
                |final class GeneratedRunnableModule {
                |  Set<Runnable> modules() {
                |    Set<Runnable> modules = new LinkedHashSet<>(1);
                |    modules.add(new FooRunnable());
                |    return modules;
                |  }
                |}
                |""".trimMargin())

        val testRegistrable = JavaFileObjects.forSourceString("example.TestRegistrable", """
                |package example;
                |public interface TestRegistrable {
                |    void test();
                |}
                |""".trimMargin())

        val testRegistrableLibraryModule = JavaFileObjects.forSourceString("example.TestRegistrableLibraryModule", """
                |package example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |@LibraryModule(TestRegistrable.class)
                |public final class TestRegistrableLibraryModule implements TestRegistrable {
                |    @Override public void test() {
                |    }
                |}
                |""".trimMargin())

        val testRegistrableAppModule = JavaFileObjects.forSourceString("example.TestRegistrableAppModule", """
                |package example;
                |import com.jaynewstrom.composite.runtime.AppModule;
                |@AppModule(TestRegistrable.class)
                |public final class TestRegistrableAppModule {
                |}
                |""".trimMargin())

        val expectedTestRegistrableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_example_TestRegistrableLibraryModule", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "example.TestRegistrable", libraryModule = "example.TestRegistrableLibraryModule")
                |public final class LibraryModuleIndexer_example_TestRegistrableLibraryModule {
                |}
                |""".trimMargin())

        val expectedGeneratedTestRegistrableModule = JavaFileObjects.forSourceLines("example.GeneratedTestRegistrableModule", """
                |package example;
                |
                |import java.util.LinkedHashSet;
                |import java.util.Set;
                |
                |final class GeneratedTestRegistrableModule {
                |  Set<TestRegistrable> modules() {
                |    Set<TestRegistrable> modules = new LinkedHashSet<>(1);
                |    modules.add(new TestRegistrableLibraryModule());
                |    return modules;
                |  }
                |}
                |""".trimMargin())

        assertAbout(javaSources())
                .that(Arrays.asList(runnableLibraryModule, runnableAppModule, testRegistrable, testRegistrableLibraryModule,
                        testRegistrableAppModule))
                .processedWith(CompositeProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedRunnableIndexer, expectedGeneratedRunnableModule, expectedTestRegistrableIndexer,
                        expectedGeneratedTestRegistrableModule)
    }

    @Test fun testExcludes() {
        val fooRunnableModule = JavaFileObjects.forSourceString("com.example.FooRunnable", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |import java.lang.Runnable;
                |@LibraryModule(Runnable.class)
                |public final class FooRunnable implements Runnable {
                |    @Override public void run() {
                |    }
                |}
                |""".trimMargin())

        val barRunnableModule = JavaFileObjects.forSourceString("com.example.BarRunnable", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |import java.lang.Runnable;
                |@LibraryModule(Runnable.class)
                |public final class BarRunnable implements Runnable {
                |    @Override public void run() {
                |    }
                |}
                |""".trimMargin())

        val runnableAppModule = JavaFileObjects.forSourceString("com.example.RunnableAppModule", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.AppModule;
                |import java.lang.Runnable;
                |@AppModule(value = Runnable.class, excludes = "com.example.BarRunnable")
                |public final class RunnableAppModule {
                |}
                |""".trimMargin())

        val expectedRunnableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_com_example_FooRunnable", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "java.lang.Runnable", libraryModule = "com.example.FooRunnable")
                |public final class LibraryModuleIndexer_com_example_FooRunnable {
                |}
                |""".trimMargin())

        val expectedGeneratedRunnableModule = JavaFileObjects.forSourceLines("com.example.GeneratedRunnableModule", """
                |package com.example;
                |
                |import java.lang.Runnable;
                |import java.util.LinkedHashSet;
                |import java.util.Set;
                |
                |final class GeneratedRunnableModule {
                |  Set<Runnable> modules() {
                |    Set<Runnable> modules = new LinkedHashSet<>(1);
                |    modules.add(new FooRunnable());
                |    return modules;
                |  }
                |}
                |""".trimMargin())

        assertAbout(javaSources())
                .that(Arrays.asList(fooRunnableModule, barRunnableModule, runnableAppModule))
                .processedWith(CompositeProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedRunnableIndexer, expectedGeneratedRunnableModule)
    }

    @Test fun testSingle() {
        val fooRunnableModule = JavaFileObjects.forSourceString("com.example.FooRunnable", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |import java.lang.Runnable;
                |@LibraryModule(Runnable.class)
                |public final class FooRunnable implements Runnable {
                |    @Override public void run() {
                |    }
                |}
                |""".trimMargin())

        val runnableAppModule = JavaFileObjects.forSourceString("com.example.RunnableAppModule", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.AppModule;
                |import java.lang.Runnable;
                |@AppModule(value = Runnable.class, single = true)
                |public final class RunnableAppModule {
                |}
                |""".trimMargin())

        val expectedRunnableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_com_example_FooRunnable", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "java.lang.Runnable", libraryModule = "com.example.FooRunnable")
                |public final class LibraryModuleIndexer_com_example_FooRunnable {
                |}
                |""".trimMargin())

        val expectedGeneratedRunnableModule = JavaFileObjects.forSourceLines("com.example.GeneratedRunnableModule", """
                |package com.example;
                |
                |import java.lang.Runnable;
                |
                |final class GeneratedRunnableModule {
                |  Runnable module() {
                |    return new FooRunnable();
                |  }
                |}
                |""".trimMargin())

        assertAbout(javaSources())
                .that(Arrays.asList(fooRunnableModule, runnableAppModule))
                .processedWith(CompositeProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedRunnableIndexer, expectedGeneratedRunnableModule)
    }

    @Test fun testAppModuleTriggersMultipleGeneratedAppModules() {
        val runnableLibraryModule = JavaFileObjects.forSourceString("com.example.FooRunnable", """
                |package com.example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |@LibraryModule(Runnable.class)
                |public final class FooRunnable implements Runnable {
                |    @Override public void run() {
                |    }
                |}
                |""".trimMargin())

        val expectedRunnableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_com_example_FooRunnable", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "java.lang.Runnable", libraryModule = "com.example.FooRunnable")
                |public final class LibraryModuleIndexer_com_example_FooRunnable {
                |}
                |""".trimMargin())

        val expectedGeneratedRunnableModule = JavaFileObjects.forSourceLines("foo.example.GeneratedRunnableModule", """
                |package foo.example;
                |
                |import com.example.FooRunnable;
                |import java.lang.Runnable;
                |import java.util.LinkedHashSet;
                |import java.util.Set;
                |
                |final class GeneratedRunnableModule {
                |  Set<Runnable> modules() {
                |    Set<Runnable> modules = new LinkedHashSet<>(1);
                |    modules.add(new FooRunnable());
                |    return modules;
                |  }
                |}
                |""".trimMargin())

        val testRegistrable = JavaFileObjects.forSourceString("example.TestRegistrable", """
                |package example;
                |public interface TestRegistrable {
                |    void test();
                |}
                |""".trimMargin())

        val testRegistrableLibraryModule = JavaFileObjects.forSourceString("example.TestRegistrableLibraryModule", """
                |package example;
                |import com.jaynewstrom.composite.runtime.LibraryModule;
                |@LibraryModule(TestRegistrable.class)
                |public final class TestRegistrableLibraryModule implements TestRegistrable {
                |    @Override public void test() {
                |    }
                |}
                |""".trimMargin())

        val expectedTestRegistrableIndexer = JavaFileObjects.forSourceString(
                "com.jaynewstrom.composite.generated.LibraryModuleIndexer_example_TestRegistrableLibraryModule", """
                |package com.jaynewstrom.composite.generated;
                |
                |import com.jaynewstrom.composite.runtime.LibraryModuleIndexer;
                |
                |@LibraryModuleIndexer(value = "example.TestRegistrable", libraryModule = "example.TestRegistrableLibraryModule")
                |public final class LibraryModuleIndexer_example_TestRegistrableLibraryModule {
                |}
                |""".trimMargin())

        val expectedGeneratedTestRegistrableModule = JavaFileObjects.forSourceLines("foo.example.GeneratedTestRegistrableModule", """
                |package foo.example;
                |
                |import example.TestRegistrable;
                |import example.TestRegistrableLibraryModule;
                |import java.util.LinkedHashSet;
                |import java.util.Set;
                |
                |final class GeneratedTestRegistrableModule {
                |  Set<TestRegistrable> modules() {
                |    Set<TestRegistrable> modules = new LinkedHashSet<>(1);
                |    modules.add(new TestRegistrableLibraryModule());
                |    return modules;
                |  }
                |}
                |""".trimMargin())

        val appModule = JavaFileObjects.forSourceString("foo.example.RunnableAppModule", """
                |package foo.example;
                |import com.jaynewstrom.composite.runtime.AppModule;
                |import java.lang.Runnable;
                |import example.TestRegistrable;
                |@AppModule({Runnable.class, TestRegistrable.class})
                |public final class RunnableAppModule {
                |}
                |""".trimMargin())

        assertAbout(javaSources())
                .that(Arrays.asList(runnableLibraryModule, appModule, testRegistrable, testRegistrableLibraryModule))
                .processedWith(CompositeProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedRunnableIndexer, expectedGeneratedRunnableModule, expectedTestRegistrableIndexer,
                        expectedGeneratedTestRegistrableModule)
    }
}
