package com.jaynewstrom.composite.compiler

import com.jaynewstrom.composite.runtime.AppModule
import com.jaynewstrom.composite.runtime.LibraryModule
import com.jaynewstrom.composite.runtime.LibraryModuleIndexer
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

private const val GENERATED_INDEXER_PACKAGE_NAME: String = "com.jaynewstrom.composite.generated"

class CompositeAppProcessor : AbstractProcessor() {
    private val appModules = mutableSetOf<Element>()

    private val messager: Messager
        get() = processingEnv.messager
    private val filer: Filer
        get() = processingEnv.filer
    private val elementUtils: Elements
        get() = processingEnv.elementUtils

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            AppModule::class.java.canonicalName,
            LibraryModuleIndexer::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        storeAppModules(roundEnv)

        // We need to let the other processor do it's thing. We'll do ours in another round.
        if (roundEnv.getElementsAnnotatedWith(LibraryModule::class.java).isNotEmpty()) {
            return false
        }

        if (appModules.isEmpty()) {
            return false
        }

        processAppModules()

        return true
    }

    private fun storeAppModules(roundEnv: RoundEnvironment) {
        for (element in roundEnv.getElementsAnnotatedWith(AppModule::class.java)) {
            if (element.kind !== ElementKind.CLASS) {
                error(element, "%s annotations can only be applied to classes!", AppModule::class.java.simpleName)
                return
            }
            appModules.add(element)
        }
    }

    private fun processAppModules() {
        val typeToLibraryModulesMap = mutableMapOf<String, MutableSet<String>>()
        val indexerPackage: PackageElement? = elementUtils.getPackageElement(GENERATED_INDEXER_PACKAGE_NAME)
        indexerPackage?.enclosedElements?.forEach { indexerElement ->
            val indexerAnnotation = indexerElement.getAnnotation(LibraryModuleIndexer::class.java)
            if (typeToLibraryModulesMap.containsKey(indexerAnnotation.value)) {
                typeToLibraryModulesMap[indexerAnnotation.value]!!.add(indexerAnnotation.libraryModule)
            } else {
                typeToLibraryModulesMap[indexerAnnotation.value] = mutableSetOf(indexerAnnotation.libraryModule)
            }
        }
        appModules.forEach { appModule ->
            val annotation = Util.getAnnotation(AppModule::class.java, appModule)
            val appModuleAnnotation = appModule.getAnnotation(AppModule::class.java)
            val excludedLibraryModuleNames = appModuleAnnotation.excludes.toSet()
            val annotationValue = annotation["value"]
            if (annotationValue !is Array<*>) {
                error(appModule, "AppModule#value() is not an array")
                return
            }
            annotationValue.forEach { contributingToClass ->
                if (contributingToClass !is TypeMirror) {
                    error(appModule, "All values must be a TypeMirror, including: s", contributingToClass.toString())
                    return
                }
                val contributingToClassName = ClassName.get(contributingToClass)
                if (contributingToClassName !is ClassName) {
                    error(appModule, "contributingToType must be a ClassName")
                    return
                }
                val libraryModuleNames = typeToLibraryModulesMap.getOrDefault(contributingToClassName.reflectionName(), mutableSetOf())
                val filteredLibraryModuleNames = libraryModuleNames.filter { it !in excludedLibraryModuleNames }
                generateAppModule(appModule, contributingToClassName, appModuleAnnotation, filteredLibraryModuleNames)
            }
        }
        appModules.clear()
    }

    private fun generateAppModule(
        appModuleElement: Element,
        contributingToClassName: ClassName,
        appModuleAnnotation: AppModule,
        libraryModuleNames: List<String>
    ) {
        val builder = TypeSpec.classBuilder("Generated${contributingToClassName.simpleName()}Module")
            .addModifiers(Modifier.FINAL)
            .addOriginatingElement(appModuleElement)
        builder.addMethod(appModuleConstructor())

        val libraryModuleTypeElements = libraryModuleNames.map { elementUtils.getTypeElement(it) }

        libraryModuleTypeElements.forEach { builder.addOriginatingElement(it) }

        if (appModuleAnnotation.single) {
            if (libraryModuleNames.size != 1) {
                error(
                    appModuleElement,
                    "Library modules included must be exactly one.\nActual library modules included are: %s",
                    libraryModuleNames
                )
                return
            }
            builder.addMethod(moduleMethod(contributingToClassName, libraryModuleTypeElements.first()))
        } else {
            builder.addMethod(modulesMethod(contributingToClassName, libraryModuleTypeElements))
        }

        val appModuleClassName = ClassName.get(appModuleElement.asType()) as ClassName
        val file = JavaFile.builder(appModuleClassName.packageName(), builder.build()).build()
        try {
            file.writeTo(filer)
        } catch (exception: IOException) {
            error(appModuleElement, "Failed to write file. \n%s", exception)
        }
    }

    private fun appModuleConstructor(): MethodSpec {
        val builder = MethodSpec.constructorBuilder()
        builder.addModifiers(Modifier.PRIVATE)
        builder.addStatement("throw new \$T(\"No instances.\")", AssertionError::class.java)
        return builder.build()
    }

    private fun moduleMethod(contributingToClassName: ClassName, libraryModuleTypeElement: TypeElement): MethodSpec {
        val builder = MethodSpec.methodBuilder("module").returns(contributingToClassName)
        builder.addModifiers(Modifier.STATIC)
        builder.addStatement("return new \$T()", libraryModuleTypeElement)
        return builder.build()
    }

    private fun modulesMethod(contributingToClassName: ClassName, libraryModuleTypeElements: List<TypeElement>): MethodSpec {
        val builder = MethodSpec.methodBuilder("modules")
            .returns(ParameterizedTypeName.get(ClassName.get(Set::class.java), contributingToClassName))
        builder.addModifiers(Modifier.STATIC)
        builder.addStatement(
            "\$T<\$T> modules = new \$T<>(\$L)",
            Set::class.java, contributingToClassName, LinkedHashSet::class.java, libraryModuleTypeElements.size
        )
        libraryModuleTypeElements.forEach { libraryModuleTypeElement ->
            builder.addStatement("modules.add(new \$T())", libraryModuleTypeElement)
        }
        builder.addStatement("return modules")
        return builder.build()
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args), e)
    }
}

class CompositeLibraryProcessor : AbstractProcessor() {
    private val messager: Messager
        get() = processingEnv.messager
    private val filer: Filer
        get() = processingEnv.filer
    private val typeUtils: Types
        get() = processingEnv.typeUtils

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            LibraryModule::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return processLibraryModules(roundEnv)
    }

    private fun processLibraryModules(roundEnv: RoundEnvironment): Boolean {
        var wroteIndexer = false
        for (element in roundEnv.getElementsAnnotatedWith(LibraryModule::class.java)) {
            if (element.kind !== ElementKind.CLASS) {
                error(element, "%s annotations can only be applied to classes!", LibraryModule::class.java.simpleName)
                return false
            }
            val libraryModuleType = element.asType()
            val annotation = Util.getAnnotation(LibraryModule::class.java, element)
            val value = annotation["value"]
            if (value !is TypeMirror) {
                error(element, "value must be an instance of a TypeMirror")
                return false
            }
            val contributingToElement = typeUtils.asElement(value)
            val contributingToType = contributingToElement.asType()
            if (!typeUtils.isAssignable(libraryModuleType, contributingToType)) {
                error(element, "libraryModule must be of type contributingToType")
                return false
            }
            if (!contributingToElement.modifiers.contains(Modifier.PUBLIC)) {
                error(element, "contributingTo must be public")
                return false
            }
            if (!element.modifiers.contains(Modifier.PUBLIC)) {
                error(element, "libraryModule must be public")
                return false
            }
            writeIndexer(element, TypeName.get(contributingToType), TypeName.get(libraryModuleType))
            wroteIndexer = true
        }
        return wroteIndexer
    }

    private fun writeIndexer(element: Element, contributingToType: TypeName, libraryModuleType: TypeName) {
        if (contributingToType !is ClassName || libraryModuleType !is ClassName) {
            error(element, "contributingToType and libraryModuleType must be a ClassName")
            return
        }
        val builder = TypeSpec.classBuilder("LibraryModuleIndexer_" + libraryModuleType.reflectionName().replace('.', '_'))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addOriginatingElement(element)
            .addAnnotation(
                AnnotationSpec.builder(ClassName.get(LibraryModuleIndexer::class.java))
                    .addMember("value", "\$S", contributingToType.reflectionName())
                    .addMember("libraryModule", "\$S", libraryModuleType.reflectionName())
                    .build()
            )
        val file = JavaFile.builder(GENERATED_INDEXER_PACKAGE_NAME, builder.build()).build()
        try {
            file.writeTo(filer)
        } catch (exception: IOException) {
            error(element, "Failed to write file. \n%s", exception)
        }
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args), e)
    }
}
