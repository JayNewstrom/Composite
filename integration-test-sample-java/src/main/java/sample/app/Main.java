package sample.app;

import com.jaynewstrom.composite.reflection.CompositeReflection;

import java.util.Set;

import sample.library.NavigationRegistrable;

public final class Main {
    public static void main(String[] args) {
        for (NavigationRegistrable navigationRegistrable : GeneratedNavigationRegistrableModule.modules()) {
            System.out.println("Compile Time! " + navigationRegistrable.getClass().getName());
        }
        Set<NavigationRegistrable> registrableSet = CompositeReflection.modules(NavigationRegistrable.class);
        for (NavigationRegistrable navigationRegistrable : registrableSet) {
            System.out.println("Reflection! " + navigationRegistrable.getClass().getName());
        }
    }
}
