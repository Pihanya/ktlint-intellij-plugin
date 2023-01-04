//package com.pihanya.ktlint.ui.util
//
//import com.intellij.openapi.fileChooser.FileChooserDescriptor
//import com.intellij.openapi.vfs.VirtualFile
//import java.util.*
//
///**
// * Custom FileChooser Descriptor that allows the specification of a file extension.
// *
// * Construct a file chooser descriptor for the given file extension.
// * @param title          the dialog title.
// * @param description    the dialog description.
// * @param allowFilesInJars may files within JARs be selected?
// * @param fileExtensions the file extension(s).
// */
//class ExtensionFileChooserDescriptor(
//    title: String?,
//    description: String?,
//    allowFilesInJars: Boolean,
//    fileExtensions: Array<String>
//) : FileChooserDescriptor(
//    /*       chooseFiles = */ true,
//    /*     chooseFolders = */ false,
//    /*        chooseJars = */ containsJar(fileExtensions),
//    /* chooseJarsAsFiles = */ containsJar(fileExtensions),
//    /* chooseJarContents = */ allowFilesInJars,
//    /*    chooseMultiple = */ false,
//) {
//
//    private val allowFilesInJars: Boolean = allowFilesInJars
//
//    private val fileExtensions: Array<String> = sortAndMakeLowercase(fileExtensions)
//
//    init {
//        setTitle(title)
//        setDescription(description)
//    }
//
//    private fun sortAndMakeLowercase(strings: Array<String>): Array<String> {
//        Arrays.sort(strings)
//        for (i in strings.indices) {
//            strings[i] = strings[i].lowercase(Locale.getDefault())
//        }
//        return strings
//    }
//
//    override fun isFileSelectable(file: VirtualFile?): Boolean = fileExtensionMatches(file)
//
//    override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean =
//        file.isDirectory || fileExtensionMatches(file) || isJar(file) && allowFilesInJars
//
//    private fun isJar(file: VirtualFile): Boolean {
//        val currentExtension = file.extension
//        return JAR_EXTENSION.equals(currentExtension, ignoreCase = true)
//    }
//
//    private fun fileExtensionMatches(file: VirtualFile?): Boolean {
//        val currentExtension = file!!.extension
//        return (currentExtension != null) && Arrays.binarySearch(
//            fileExtensions,
//            currentExtension.lowercase(Locale.getDefault()),
//        ) >= 0
//    }
//
//    companion object {
//
//        private const val JAR_EXTENSION = "jar"
//
//        private fun containsJar(extensions: Array<String>): Boolean {
//            for (extension in extensions) {
//                if ("jar".equals(extension, ignoreCase = true)) {
//                    return true
//                }
//            }
//            return false
//        }
//    }
//}
