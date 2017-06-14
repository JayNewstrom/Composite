package sample.library

interface NavigationRegistrable {
    fun name(): String
    fun action(): () -> Unit
}
