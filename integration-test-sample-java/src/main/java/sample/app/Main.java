package sample.app;

import sample.library.NavigationRegistrable;

public final class Main {
    public static void main(String[] args) {
        for (NavigationRegistrable navigationRegistrable : new GeneratedNavigationRegistrableModule().modules()) {
            System.out.println(navigationRegistrable.getClass().getName());
        }
    }
}
