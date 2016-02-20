/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.yangutils.translator.tojava;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.SerializedDataStore;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * Maintain the information about the java file to be generated.
 */
public class CachedJavaFileHandle implements CachedFileHandle {

    private static final Logger log = getLogger(CachedJavaFileHandle.class);

    private static final int MAX_CACHABLE_ATTR = 64;
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEMP_FILE_EXTENSION = ".tmp";

    /**
     * The type(s) of java source file(s) to be generated when the cached file
     * handle is closed.
     */
    private GeneratedFileType genFileTypes;

    /**
     * The type(s) of java method to be generated when the cached file handle is
     * closed.
     */
    private GeneratedMethodTypes genMethodTypes;

    /**
     * Java package in which the class/interface needs to be generated.
     */
    private String pkg;

    /**
     * Name of the object in YANG file.
     */
    private String yangName;

    /**
     * Sorted set of import info, to be used to maintain the set of classes to
     * be imported in the generated class.
     */
    private SortedSet<ImportInfo> importSet;

    /**
     * Cached list of attribute info.
     */
    private List<AttributeInfo> attributeList;

    /**
     * Prevent invoking default constructor.
     */
    private CachedJavaFileHandle() {
        setCachedAttributeList(new LinkedList<AttributeInfo>());
    }

    /**
     * Create a cached file handle which takes care of adding attributes to the
     * generated java file.
     *
     * @param pcg package in which class/interface need to be generated.
     * @param yangName name of the attribute in YANG file.
     * @param types the types of files that needs to be generated.
     * @throws IOException file IO exception.
     */
    public CachedJavaFileHandle(String pcg, String yangName, GeneratedFileType types) throws IOException {
        if ((new File(pcg).exists())) {
            setGeneratedFileTypes(types);
            setPackage(pcg);
            setYangName(yangName);
        } else {
            FileSystemUtil.createPackage(pcg, yangName);
            setGeneratedFileTypes(types);
            setPackage(pcg);
            setYangName(yangName);
        }
    }

    /**
     * Get the types of files being generated corresponding to the YANG
     * definition.
     *
     * @return the types of files being generated corresponding to the YANG
     *         definition.
     */
    public GeneratedFileType getGeneratedFileTypes() {
        return genFileTypes;
    }

    /**
     * Set the types of files being generated corresponding to the YANG
     * definition.
     *
     * @param fileTypes the types of files being generated corresponding to the
     *            YANG definition.
     */
    public void setGeneratedFileTypes(GeneratedFileType fileTypes) {
        genFileTypes = fileTypes;
    }

    /**
     * Get the corresponding name defined in YANG.
     *
     * @return the corresponding name defined in YANG.
     */
    public String getYangName() {
        return yangName;
    }

    /**
     * Set the corresponding name defined in YANG.
     *
     * @param yangName the corresponding name defined in YANG.
     */
    public void setYangName(String yangName) {
        this.yangName = yangName;
    }

    /**
     * Get the java package.
     *
     * @return the java package.
     */
    public String getPackage() {
        return pkg;
    }

    /**
     * Set the java package.
     *
     * @param pcg the package to set
     */
    public void setPackage(String pcg) {
        pkg = pcg;
    }

    /**
     * Get the set containing the imported class/interface info.
     *
     * @return the set containing the imported class/interface info.
     */
    public SortedSet<ImportInfo> getImportSet() {
        return importSet;
    }

    /**
     * Assign the set containing the imported class/interface info.
     *
     * @param importSet the set containing the imported class/interface info.
     */
    private void setImportSet(SortedSet<ImportInfo> importSet) {
        this.importSet = importSet;
    }

    /**
     * Add an imported class/interface info is it is not already part of the
     * set. If already part of the set, return false, else add to set and return
     * true.
     *
     * @param importInfo class/interface info being imported.
     * @return status of new addition of class/interface to the import set
     */
    public boolean addImportInfo(ImportInfo importInfo) {
        /*
         * implement the import info adding. The return value will be used to
         * check if the qualified name will be used or class/interface name will
         * be used in the generated class.
         */
        if (getImportSet() == null) {
            setImportSet(new TreeSet<ImportInfo>());
        }
        return getImportSet().add(importInfo);
    }

    /**
     * Get the list of cached attribute list.
     *
     * @return the set containing the imported class/interface info.
     */
    public List<AttributeInfo> getCachedAttributeList() {
        return attributeList;
    }

    /**
     * Set the cached attribute list.
     *
     * @param attrList attribute list.
     */
    private void setCachedAttributeList(List<AttributeInfo> attrList) {
        attributeList = attrList;
    }

    /**
     * Flush the cached attribute list to the serialized file.
     */
    private void flushCacheAttrToSerFile() {

        for (AttributeInfo attr : getCachedAttributeList()) {
            parseAttributeInfo(attr);
        }

        /*
         * clear the contents from the cached attribute list.
         */
        getCachedAttributeList().clear();
    }

    /**
     * Add a new attribute to the file(s).
     *
     * @param attrType data type of the added attribute.
     * @param name name of the attribute.
     * @param isListAttr if the current added attribute needs to be maintained
     *            in a list.
     */
    @Override
    public void addAttributeInfo(YangType<?> attrType, String name, boolean isListAttr) {

        AttributeInfo newAttr = new AttributeInfo();
        attrType.setDataTypeName(attrType.getDataTypeName().replace("\"", ""));
        if (attrType.getDataTypeName().equals("string")) {
            attrType.setDataTypeName(
                    attrType.getDataTypeName().substring(0, 1).toUpperCase() + attrType.getDataTypeName().substring(1));
        }
        newAttr.setAttributeType(attrType);
        newAttr.setAttributeName(name);
        newAttr.setListAttr(isListAttr);

        /*
         * TODO: get the prefix and name of data type from attrType and
         * initialize in importInfo.
         */

        /**
         * TODO: Handle QualifiedFlag for imports.
         */

        if (getCachedAttributeList() != null) {
            if (getCachedAttributeList().size() == MAX_CACHABLE_ATTR) {
                flushCacheAttrToSerFile();
            }
            getCachedAttributeList().add(newAttr);
        } else {
            List<AttributeInfo> newAttributeInfo = new LinkedList<>();
            newAttributeInfo.add(newAttr);
            setCachedAttributeList(newAttributeInfo);
        }
        name = JavaIdentifierSyntax.getCamelCase(name);
    }

    /**
     * Flushes the cached contents to the target file, frees used resources.
     */
    @Override
    public void close() throws IOException {

        String className = getYangName();
        className = (className.substring(0, 1).toUpperCase() + className.substring(1));
        String packagePath = getPackage();
        String filePath = UtilConstants.YANG_GEN_DIR + packagePath.replace(".", "/");
        GeneratedFileType fileType = getGeneratedFileTypes();

        /**
         * Create interface file.
         */
        String interfaceFileName = className + JAVA_FILE_EXTENSION;
        File interfaceFile = new File(filePath + File.separator + interfaceFileName);

        /**
         * Create temp builder interface file.
         */
        String builderInterfaceFileName = interfaceFileName + TEMP_FILE_EXTENSION;
        File builderInterfaceFile = new File(filePath + File.separator + builderInterfaceFileName);

        /**
         * Create builder class file.
         */
        String builderFileName = className + UtilConstants.BUILDER + JAVA_FILE_EXTENSION;
        File builderFile = new File(filePath + File.separator + builderFileName);
        MethodsGenerator.setBuilderClassName(className + UtilConstants.BUILDER);

        /**
         * Create temp impl class file.
         */

        String implFileName = className + UtilConstants.IMPL + TEMP_FILE_EXTENSION;
        File implTempFile = new File(filePath + File.separator + implFileName);

        if (fileType.equals(GeneratedFileType.INTERFACE) || fileType.equals(GeneratedFileType.ALL)) {

            try {
                interfaceFile.createNewFile();
                appendContents(interfaceFile, className, GeneratedFileType.INTERFACE);
            } catch (IOException e) {
                throw new IOException("Failed to create interface file.");
            }
        }

        if (fileType.equals(GeneratedFileType.BUILDER_CLASS) || fileType.equals(GeneratedFileType.ALL)) {

            try {
                builderFile.createNewFile();
                appendContents(builderFile, className, GeneratedFileType.BUILDER_CLASS);
            } catch (IOException e) {
                throw new IOException("Failed to create builder class file.");
            }
        }

        if (fileType.equals(GeneratedFileType.IMPL) || fileType.equals(GeneratedFileType.ALL)) {

            try {
                implTempFile.createNewFile();
                appendContents(implTempFile, className, GeneratedFileType.IMPL);
            } catch (IOException e) {
                throw new IOException("Failed to create impl class file.");
            }
        }

        if (fileType.equals(GeneratedFileType.BUILDER_INTERFACE) || fileType.equals(GeneratedFileType.ALL)) {

            try {
                builderInterfaceFile.createNewFile();
                appendContents(builderInterfaceFile, className, GeneratedFileType.BUILDER_INTERFACE);
            } catch (IOException e) {
                throw new IOException("Failed to create builder interface class file.");
            }
        }
        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */
        /*
         * TODO: get the import list using getImportText and add to the
         * generated java file using JavaCodeSnippetGen.getImportText
         */

        List<String> attributes = new LinkedList<>();
        List<String> interfaceMethods = new LinkedList<>();
        List<String> builderInterfaceMethods = new LinkedList<>();
        List<String> builderClassMethods = new LinkedList<>();
        List<String> implClassMethods = new LinkedList<>();
        //TODO: Handle imports for the attributes.
        try {
            attributes = SerializedDataStore.getSerializeData(SerializedDataStore.SerializedDataStoreType.ATTRIBUTE);

            interfaceMethods = SerializedDataStore
                    .getSerializeData(SerializedDataStore.SerializedDataStoreType.INTERFACE_METHODS);

            builderInterfaceMethods = SerializedDataStore
                    .getSerializeData(SerializedDataStore.SerializedDataStoreType.BUILDER_INTERFACE_METHODS);

            builderClassMethods = SerializedDataStore
                    .getSerializeData(SerializedDataStore.SerializedDataStoreType.BUILDER_METHODS);

            implClassMethods = SerializedDataStore
                    .getSerializeData(SerializedDataStore.SerializedDataStoreType.IMPL_METHODS);

            //TODO:imports = SerializedDataStore.getSerializeData(SerializedDataStore.SerializedDataStoreType.IMPORT);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the serialized files.");
        }

        if (getCachedAttributeList() != null) {
            MethodsGenerator.setAttrInfo(getCachedAttributeList());
            for (AttributeInfo attr : getCachedAttributeList()) {
                attributes.add(getAttributeString(attr));

                interfaceMethods.add(MethodsGenerator.getMethodString(attr, GeneratedFileType.INTERFACE));

                builderClassMethods.add(MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_CLASS));

                builderInterfaceMethods
                .add(MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_INTERFACE));

                implClassMethods.add(MethodsGenerator.getMethodString(attr, GeneratedFileType.IMPL));
            }
        }

        builderInterfaceMethods.add(MethodsGenerator.parseBuilderInterfaceBuildMethodString(className));
        builderClassMethods.add(UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(GeneratedFileType.BUILDER_CLASS, className));
        builderClassMethods.add(MethodsGenerator.getBuildString(className));

        implClassMethods.add(UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(GeneratedFileType.IMPL, className));
        implClassMethods.add(MethodsGenerator.getConstructorString(className));

        /**
         * Add attributes to the file.
         */
        for (String attribute : attributes) {
            insert(builderFile, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
            insert(implTempFile, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
        }

        insert(builderFile, UtilConstants.NEW_LINE);
        insert(implTempFile, UtilConstants.NEW_LINE);

        /**
         * Add getter methods to interface file.
         */
        for (String method : interfaceMethods) {
            appendMethod(interfaceFile, method + UtilConstants.NEW_LINE);
        }

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : builderInterfaceMethods) {
            appendMethod(builderInterfaceFile, UtilConstants.FOUR_SPACE_INDENTATION + method + UtilConstants.NEW_LINE);
        }

        insert(builderInterfaceFile, UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE);
        /**
         * Add methods in builder class.
         */
        for (String method : builderClassMethods) {
            appendMethod(builderFile, method + UtilConstants.NEW_LINE);
        }

        /**
         * Add methods in impl class.
         */
        for (String method : implClassMethods) {
            appendMethod(implTempFile, UtilConstants.FOUR_SPACE_INDENTATION + method + UtilConstants.NEW_LINE);
        }

        insert(implTempFile, UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE);

        /**
         * Append builder interface file to interface file and close it.
         */
        appendFileContents(builderInterfaceFile, interfaceFile);
        insert(interfaceFile, closeFile(GeneratedFileType.INTERFACE, interfaceFileName));

        /**
         * Append impl class to builder class and close it.
         */
        appendFileContents(implTempFile, builderFile);
        insert(builderFile, closeFile(GeneratedFileType.BUILDER_CLASS, builderFileName));

        /**
         * Remove temp files.
         */
        clean(implTempFile);
        clean(builderInterfaceFile);
    }

    /**
     * Appends the temp files to main files.
     *
     * @param appendFile temp file
     * @param srcFile main file
     */
    private static void appendFileContents(File appendFile, File srcFile) throws IOException {
        try {
            FileSystemUtil.appendFileContents(appendFile, srcFile);
        } catch (IOException e) {
            throw new IOException("Failed to append " + appendFile + " in " + srcFile);
        }
    }

    /**
     * Append methods to the generated files.
     *
     * @param file file in which method needs to be appended.
     * @param method method which needs to be appended.
     */
    private static void appendMethod(File file, String method) throws IOException {
        insert(file, method);
    }

    /**
     * Closes the current generated file.
     *
     * @param fileType generate file type
     * @param yangName file name
     * @return end of class definition string.
     */
    private static String closeFile(GeneratedFileType fileType, String yangName) {
        return JavaCodeSnippetGen.getJavaClassDefClose(fileType, yangName);
    }

    /**
     * Parses attribute info and fetch specific data and creates serialized
     * files of it.
     *
     * @param attr attribute info.
     */
    private void parseAttributeInfo(AttributeInfo attr) {

        String attrString = "";
        String methodString = "";
        String getterString = "";

        try {
            /*
             * Serialize attributes.
             */
            attrString = getAttributeString(attr);
            attrString = attrString.replace("\"", "");
            SerializedDataStore.setSerializeData(attrString, SerializedDataStore.SerializedDataStoreType.ATTRIBUTE);

            if (getGeneratedFileTypes().equals(GeneratedFileType.ALL)) {

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.INTERFACE);
                SerializedDataStore.setSerializeData(methodString,
                        SerializedDataStore.SerializedDataStoreType.INTERFACE_METHODS);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_CLASS);
                SerializedDataStore.setSerializeData(methodString,
                        SerializedDataStore.SerializedDataStoreType.BUILDER_METHODS);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_INTERFACE);
                SerializedDataStore.setSerializeData(methodString,
                        SerializedDataStore.SerializedDataStoreType.BUILDER_INTERFACE_METHODS);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.IMPL);
                SerializedDataStore.setSerializeData(methodString,
                        SerializedDataStore.SerializedDataStoreType.IMPL_METHODS);

            } else if (getGeneratedFileTypes().equals(GeneratedFileType.INTERFACE)) {

                getterString = MethodsGenerator.getGetterString(attr);
                SerializedDataStore.setSerializeData(methodString,
                        SerializedDataStore.SerializedDataStoreType.INTERFACE_METHODS);
            }
        } catch (IOException e) {
            log.info("Failed to get data for " + attr.getAttributeName() + " from serialized files.");
        }
    }

    /**
     * Returns attribute string.
     *
     * @param attr attribute info
     * @return attribute string
     */
    private String getAttributeString(AttributeInfo attr) {
        return JavaCodeSnippetGen.getJavaAttributeInfo(getGeneratedFileTypes(), attr.getAttributeName(),
                attr.getAttributeType());
    }

    /**
     * Appends all the contents into a generated java file.
     *
     * @param file generated file
     * @param fileName generated file name
     * @param type generated file type
     */
    private void appendContents(File file, String fileName, GeneratedFileType type) throws IOException {

        if (type.equals(GeneratedFileType.IMPL)) {

            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if (type.equals(GeneratedFileType.BUILDER_INTERFACE)) {

            write(file, fileName, type, JavaDocType.BUILDER_INTERFACE);
        } else {

            // TODO: handle imports for attributes.

            if (type.equals(GeneratedFileType.INTERFACE)) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + getPackage() + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE);
                write(file, fileName, type, JavaDocType.INTERFACE);
            } else if (type.equals(GeneratedFileType.BUILDER_CLASS)) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + getPackage() + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE);
                write(file, fileName, type, JavaDocType.BUILDER_CLASS);
            }
        }
    }

    /**
     * Write data to the specific generated file.
     *
     * @param file generated file
     * @param fileName file name
     * @param genType generated file type
     * @param javaDocType java doc type
     */
    private static void write(File file, String fileName, GeneratedFileType genType, JavaDocGen.JavaDocType javaDocType)
            throws IOException {

        insert(file, JavaDocGen.getJavaDoc(javaDocType, fileName));
        insert(file, JavaCodeSnippetGen.getJavaClassDefStart(genType, fileName));
    }

    /**
     * Insert in the generated file.
     *
     * @param file file in which need to be inserted.
     * @param data data which need to be inserted.
     */
    private static void insert(File file, String data) throws IOException {
        try {
            FileSystemUtil.insertStringInFile(file, data);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }

    /**
     * Removes temp files.
     *
     * @param file file to be removed.
     */
    private static void clean(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}
