package sample.app

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            for (navigationRegistrable in GeneratedNavigationRegistrableModule.modules()) {
                navigationRegistrable.action().invoke()
            }
        }
    }
}
