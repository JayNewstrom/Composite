Composite
=========

Composite is an annotation processor that allows defining conformance to an interface/abstract class in child projects (jar/aar) and
automatic compositing of them in an app project.

Defined in messages gradle module:
```java
@LibraryModule("com.example.NavigationRegistrable")
public final class MessagesNavigationRegistrable implements NavigationRegistrable {
    @Override public String menuType() {
        return "Messages";
    }

    @Override public void performMenuAction() {
        // TODO
    }
}
```

Defined in app gradle module:
```java
// Triggers the annotation processor to generate the composite of all NavigationRegistrable library modules.
@AppModule("com.example.NavigationRegistrable")
final class AppNavigationRegistrable {
}
```

This will generate a class for you to use in the app gradle module called `GeneratedNavigationRegistrableModule` which will have a
method called `modules` returning a `Set<NavigationRegistrable>` which will allow you to handle navigation in a generic way where
the navigation modules included at build time are automically handled at runtime.

Setup
------------
```groovy
dependencies {
    annotationProcessor 'com.jaynewstrom.composite:compiler:0.4.0'
    implementation 'com.jaynewstrom.composite:runtime:0.4.0'
}
```

License
-------

    Copyright 2017 Jay Newstrom

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
