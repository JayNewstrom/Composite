package sample.library

import com.jaynewstrom.composite.runtime.LibraryModule

@LibraryModule(NavigationRegistrable::class)
class NavigationFooLibraryModule : NavigationRegistrable {
    override fun name(): String {
        return "Foo"
    }

    override fun action(): Runnable {
        return Runnable { System.out.println("Foo!") }
    }
}
