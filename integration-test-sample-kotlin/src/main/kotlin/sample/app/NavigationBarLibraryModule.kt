package sample.app

import com.jaynewstrom.composite.runtime.LibraryModule
import sample.library.NavigationRegistrable

@LibraryModule("sample.library.NavigationRegistrable")
class NavigationBarLibraryModule : NavigationRegistrable {
    override fun name(): String {
        return "Bar"
    }

    override fun action(): Runnable {
        return Runnable { System.out.println("Bar!") }
    }
}
