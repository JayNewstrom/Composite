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
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class CompositeProcessor : AbstractProcessor() {
    private val GENERATED_INDEXER_PACKAGE_NAME: String = "com.jaynewstrom.composite.generated"

    val appModules = mutableListOf<Element>()

    val messager: Messager
        get() = processingEnv.messager
    val filer: Filer
        get() = processingEnv.filer
    val typeUtils: Types
        get() = processingEnv.typeUtils
    val elementUtils: Elements
        get() = processingEnv.elementUtils

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
                AppModule::class.java.canonicalName,
                LibraryModule::class.java.canonicalName,
                LibraryModuleIndexer::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        storeAppModules(roundEnv)
        if (processLibraryModules(roundEnv)) {
            return true
        }
        if (appModules.isEmpty()) {
            return false
        }
        processAppModules()
        return true
    }

    private fun processLibraryModules(roundEnv: RoundEnvironment): Boolean {
        var wroteIndexer = false
        for (element in roundEnv.getElementsAnnotatedWith(LibraryModule::class.java)) {
            if (element.kind !== ElementKind.CLASS) {
                error(element, "%s annotations can only be applied to classes!", LibraryModule::class.java.simpleName)
                return false
            }
            val libraryModuleType = element.asType()
            val annotation = element.getAnnotation(LibraryModule::class.java)
            val contributingToElement = elementUtils.getTypeElement(annotation.value)
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
                .addAnnotation(AnnotationSpec.builder(ClassName.get(LibraryModuleIndexer::class.java))
                        .addMember("value", "\$S", contributingToType.reflectionName())
                        .addMember("libraryModule", "\$S", libraryModuleType.reflectionName())
                        .build())
        val file = JavaFile.builder(GENERATED_INDEXER_PACKAGE_NAME, builder.build()).build()
        try {
            file.writeTo(filer)
        } catch (exception: IOException) {
            error(element, "Failed to write file. \n%s", exception)
        }
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
        val typeToIndexersMap = mutableMapOf<String, MutableSet<String>>()
        val indexerPackage: PackageElement? = elementUtils.getPackageElement(GENERATED_INDEXER_PACKAGE_NAME)
        indexerPackage?.enclosedElements?.forEach { indexerElement ->
            val indexerAnnotation = indexerElement.getAnnotation(LibraryModuleIndexer::class.java)
            if (typeToIndexersMap.containsKey(indexerAnnotation.value)) {
                typeToIndexersMap[indexerAnnotation.value]!!.add(indexerAnnotation.libraryModule)
            } else {
                typeToIndexersMap.put(indexerAnnotation.value, mutableSetOf(indexerAnnotation.libraryModule))
            }
        }
        appModules.forEach { appModule ->
            val appModuleAnnotation = appModule.getAnnotation(AppModule::class.java)
            val indexerModuleNames = typeToIndexersMap.getOrDefault(appModuleAnnotation.value, mutableSetOf())
            generateAppModule(appModule, appModuleAnnotation, indexerModuleNames)
        }
        appModules.clear()
    }

    private fun generateAppModule(appModuleElement: Element, appModuleAnnotation: AppModule, indexerModuleNames: Set<String>) {
        val contributingToClassName = ClassName.get(elementUtils.getTypeElement(appModuleAnnotation.value))
        val builder = TypeSpec.classBuilder("Generated${contributingToClassName.simpleName()}Module")
                .addModifiers(Modifier.FINAL)
                .addMethod(modulesMethod(contributingToClassName, indexerModuleNames))
        val appModuleClassName = ClassName.get(appModuleElement.asType()) as ClassName
        val file = JavaFile.builder(appModuleClassName.packageName(), builder.build()).build()
        try {
            file.writeTo(filer)
        } catch (exception: IOException) {
            error(appModuleElement, "Failed to write file. \n%s", exception)
        }
    }

    private fun modulesMethod(contributingToClassName: ClassName, indexerModuleNames: Set<String>): MethodSpec {
        val builder = MethodSpec.methodBuilder("modules")
                .returns(ParameterizedTypeName.get(ClassName.get(Set::class.java), contributingToClassName))
        builder.addStatement("\$T<\$T> modules = new \$T<>(\$L)",
                Set::class.java, contributingToClassName, LinkedHashSet::class.java, indexerModuleNames.size)
        indexerModuleNames.forEach { indexerModuleName ->
            builder.addStatement("modules.add(new \$T())", elementUtils.getTypeElement(indexerModuleName))
        }
        builder.addStatement("return modules")
        return builder.build()
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args), e)
    }
}
