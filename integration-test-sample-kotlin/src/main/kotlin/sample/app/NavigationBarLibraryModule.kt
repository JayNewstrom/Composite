package sample.app

import com.jaynewstrom.composite.runtime.LibraryModule
import sample.library.NavigationRegistrable

@LibraryModule(NavigationRegistrable::class)
class NavigationBarLibraryModule : NavigationRegistrable {
    override fun name(): String {
        return "Bar"
    }

    override fun action(): () -> Unit {
        return { System.out.println("Bar!") }
    }
}
