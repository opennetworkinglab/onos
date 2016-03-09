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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType;
import org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.translator.tojava.utils.TempDataStoreTypes;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;

/**
 * Maintain the information about the java file to be generated.
 */
public class CachedJavaFileHandle implements CachedFileHandle {

    private static final int MAX_CACHABLE_ATTR = 64;
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEMP_FILE_EXTENSION = ".tmp";
    private static final String GETTER_METHOD_FILE_NAME = "GetterMethod";
    private static final String SETTER_METHOD_FILE_NAME = "SetterMethod";
    private static final String GETTER_METHOD_IMPL_FILE_NAME = "GetterMethodImpl";
    private static final String SETTER_METHOD_IMPL_FILE_NAME = "SetterMethodImpl";
    private static final String CONSTRUCTOR_FILE_NAME = "Constructor";
    private static final String ATTRIBUTE_FILE_NAME = "Attributes";
    private static final String TO_STRING_METHOD_FILE_NAME = "ToString";
    private static final String HASH_CODE_METHOD_FILE_NAME = "HashCode";
    private static final String EQUALS_METHOD_FILE_NAME = "Equals";
    private static final String TYPE_DEF_FILE_NAME = "TypeDef";
    private static final String TEMP_FOLDER_NAME_SUFIX = "-Temp";

    /**
     * The type(s) of java source file(s) to be generated when the cached file
     * handle is closed.
     */
    private int genFileTypes;

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
     * File generation directory path.
     */
    private String relativeFilePath;

    /**
     * File generation base directory path.
     */
    private String codeGenDirFilePath;

    /**
     * Prevent invoking default constructor.
     */
    public CachedJavaFileHandle() {
        setCachedAttributeList(new LinkedList<AttributeInfo>());
    }

    /**
     * Create a cached file handle which takes care of adding attributes to the
     * generated java file.
     *
     * @param pcg package in which class/interface need to be generated
     * @param yangName name of the attribute in YANG file
     * @param types the types of files that needs to be generated
     * @throws IOException file IO exception
     */
    public CachedJavaFileHandle(String pcg, String yangName, int types) throws IOException {
        setCachedAttributeList(new LinkedList<AttributeInfo>());
        setImportSet(new TreeSet<ImportInfo>());
        setRelativeFilePath(pcg.replace(".", "/"));
        setGeneratedFileTypes(types);
        setYangName(yangName);
    }

    /**
     * Get the types of files being generated corresponding to the YANG
     * definition.
     *
     * @return the types of files being generated corresponding to the YANG
     *         definition
     */
    public int getGeneratedFileTypes() {
        return genFileTypes;
    }

    /**
     * Set the types of files being generated corresponding to the YANG
     * definition.
     *
     * @param fileTypes the types of files being generated corresponding to the
     *            YANG definition
     */
    public void setGeneratedFileTypes(int fileTypes) {
        genFileTypes = fileTypes;
    }

    /**
     * Get the corresponding name defined in YANG.
     *
     * @return the corresponding name defined in YANG
     */
    public String getYangName() {
        return yangName;
    }

    /**
     * Set the corresponding name defined in YANG.
     *
     * @param yangName the corresponding name defined in YANG
     */
    public void setYangName(String yangName) {
        this.yangName = yangName;
    }

    /**
     * Get the set containing the imported class/interface info.
     *
     * @return the set containing the imported class/interface info
     */
    public SortedSet<ImportInfo> getImportSet() {
        return importSet;
    }

    /**
     * Assign the set containing the imported class/interface info.
     *
     * @param importSet the set containing the imported class/interface info
     */
    private void setImportSet(SortedSet<ImportInfo> importSet) {
        this.importSet = importSet;
    }

    /**
     * Add an imported class/interface info is it is not already part of the
     * set. If already part of the set, return false, else add to set and return
     * true.
     *
     * @param importInfo class/interface info being imported
     * @return status of new addition of class/interface to the import set
     */
    public boolean addImportInfo(ImportInfo importInfo) {
        return getImportSet().add(importInfo);
    }

    /**
     * Get the list of cached attribute list.
     *
     * @return the set containing the imported class/interface info
     */
    public List<AttributeInfo> getCachedAttributeList() {
        return attributeList;
    }

    /**
     * Set the cached attribute list.
     *
     * @param attrList attribute list
     */
    private void setCachedAttributeList(List<AttributeInfo> attrList) {
        attributeList = attrList;
    }

    @Override
    public void setRelativeFilePath(String path) {
        relativeFilePath = path;
    }

    @Override
    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    @Override
    public String getCodeGenFilePath() {
        return codeGenDirFilePath;
    }

    @Override
    public void setCodeGenFilePath(String path) {
        codeGenDirFilePath = path;
    }

    /**
     * Flush the cached attribute list to the corresponding temporary file.
     */
    private void flushCacheAttrToTempFile() {

        for (AttributeInfo attr : getCachedAttributeList()) {
            JavaFileGenerator.parseAttributeInfo(attr, getGeneratedFileTypes(), getYangName(), getCodeGenFilePath() +
                    getRelativeFilePath().replace(UtilConstants.PERIOD, UtilConstants.SLASH), this);
        }

        /*
         * clear the contents from the cached attribute list.
         */
        getCachedAttributeList().clear();
    }

    @Override
    public void addAttributeInfo(YangType<?> attrType, String name, boolean isListAttr) {
        /* YANG name is mapped to java name */
        name = JavaIdentifierSyntax.getCamelCase(name);

        ImportInfo importInfo = new ImportInfo();
        boolean isImport = false;

        AttributeInfo newAttr = new AttributeInfo();
        if (attrType != null) {
            newAttr.setAttributeType(attrType);
            String importStr = AttributesJavaDataType.getJavaImportClass(attrType, isListAttr);
            if (importStr != null) {
                importInfo.setClassInfo(importStr);
                importStr = AttributesJavaDataType.getJavaImportPackage(attrType, isListAttr);
                importInfo.setPkgInfo(importStr);
                isImport = true;
            } else {
                importStr = AttributesJavaDataType.getJavaDataType(attrType);
                if (importStr == null) {
                    throw new RuntimeException("not supported data type");
                    //TODO: need to change to translator exception.
                }
                importInfo.setClassInfo(importStr);
            }

        } else {
            importInfo.setClassInfo(JavaIdentifierSyntax.getCaptialCase(name));
            importInfo.setPkgInfo(getRelativeFilePath().replace('/', '.')
                    + "." + getYangName().toLowerCase());
            isImport = true;
        }

        newAttr.setQualifiedName(false);
        if (isImport) {
            addImportInfo(importInfo);
        }

        if (isListAttr) {
            ImportInfo listImportInfo = new ImportInfo();
            listImportInfo.setPkgInfo(UtilConstants.COLLECTION_IMPORTS);
            listImportInfo.setClassInfo(UtilConstants.LIST);
            addImportInfo(listImportInfo);
        }

        /**
         * If two classes with different packages have same class info for import than use qualified name.
         */
        for (ImportInfo imports : getImportSet()) {
            if (imports.getClassInfo().equals(importInfo.getClassInfo())
                    && !imports.getPkgInfo().equals(importInfo.getPkgInfo())) {
                newAttr.setQualifiedName(true);
            }
        }

        newAttr.setAttributeName(name);
        newAttr.setListAttr(isListAttr);
        newAttr.setImportInfo(importInfo);

        if (getCachedAttributeList().size() == MAX_CACHABLE_ATTR) {
            flushCacheAttrToTempFile();
        }
        getCachedAttributeList().add(newAttr);
    }

    @Override
    public void close() throws IOException {

        List<AttributeInfo> attrList = getCachedAttributeList();
        flushCacheAttrToTempFile();
        String className = getYangName();
        className = JavaIdentifierSyntax.getCaptialCase(className);
        String path = getRelativeFilePath();
        int fileType = getGeneratedFileTypes();

        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */

        List<String> imports = new ArrayList<>();
        String importString;

        for (ImportInfo importInfo : new ArrayList<ImportInfo>(getImportSet())) {
            importString = UtilConstants.IMPORT;
            if (importInfo.getPkgInfo() != "" && importInfo.getClassInfo() != null
                    && importInfo.getPkgInfo() != UtilConstants.JAVA_LANG) {
                importString = importString + importInfo.getPkgInfo() + ".";
                importString = importString + importInfo.getClassInfo() + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE;

                imports.add(importString);
            }
        }
        java.util.Collections.sort(imports);

        /**
         * Start generation of files.
         */
        if ((fileType & GeneratedFileType.INTERFACE_MASK) != 0
                || fileType == GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create interface file.
             */
            String interfaceFileName = className;
            File interfaceFile = JavaFileGenerator.getFileObject(path, interfaceFileName, JAVA_FILE_EXTENSION, this);
            interfaceFile = JavaFileGenerator.generateInterfaceFile(interfaceFile, className, imports,
                    attrList, path.replace('/', '.'), this);
            /**
             * Create temp builder interface file.
             */
            String builderInterfaceFileName = className + UtilConstants.BUILDER + UtilConstants.INTERFACE;
            File builderInterfaceFile = JavaFileGenerator.getFileObject(path, builderInterfaceFileName,
                    TEMP_FILE_EXTENSION, this);
            builderInterfaceFile = JavaFileGenerator.generateBuilderInterfaceFile(builderInterfaceFile, className,
                    path.replace('/', '.'), attrList, this);
            /**
             * Append builder interface file to interface file and close it.
             */
            JavaFileGenerator.appendFileContents(builderInterfaceFile, interfaceFile);
            JavaFileGenerator.insert(interfaceFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.INTERFACE_MASK, interfaceFileName));
            /**
             * Close file handle for interface files.
             */
            JavaFileGenerator.closeFileHandles(builderInterfaceFile);
            JavaFileGenerator.closeFileHandles(interfaceFile);

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(builderInterfaceFile);
        }

        if (!attrList.isEmpty()) {
            imports.add(UtilConstants.MORE_OBJECT_IMPORT);
            imports.add(UtilConstants.JAVA_UTIL_OBJECTS_IMPORT);
            java.util.Collections.sort(imports);
        }

        if ((fileType & GeneratedFileType.BUILDER_CLASS_MASK) != 0
                || fileType == GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create builder class file.
             */
            String builderFileName = className + UtilConstants.BUILDER;
            File builderFile = JavaFileGenerator.getFileObject(path, builderFileName, JAVA_FILE_EXTENSION, this);
            builderFile = JavaFileGenerator.generateBuilderClassFile(builderFile, className, imports,
                    path.replace('/', '.'), attrList, this);
            /**
             * Create temp impl class file.
             */

            String implFileName = className + UtilConstants.IMPL;
            File implTempFile = JavaFileGenerator.getFileObject(path, implFileName, TEMP_FILE_EXTENSION, this);
            implTempFile = JavaFileGenerator.generateImplClassFile(implTempFile, className,
                    path.replace('/', '.'), attrList, this);
            /**
             * Append impl class to builder class and close it.
             */
            JavaFileGenerator.appendFileContents(implTempFile, builderFile);
            JavaFileGenerator.insert(builderFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.BUILDER_CLASS_MASK, builderFileName));

            /**
             * Close file handle for classes files.
             */
            JavaFileGenerator.closeFileHandles(implTempFile);
            JavaFileGenerator.closeFileHandles(builderFile);

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(implTempFile);
        }

        if ((fileType & GeneratedFileType.GENERATE_TYPEDEF_CLASS) != 0) {

            /**
             * Create builder class file.
             */
            String typeDefFileName = className;
            File typeDefFile = JavaFileGenerator.getFileObject(path, typeDefFileName, JAVA_FILE_EXTENSION, this);
            typeDefFile = JavaFileGenerator.generateTypeDefClassFile(typeDefFile, className, imports,
                    path.replace('/', '.'), attrList, this);
            JavaFileGenerator.insert(typeDefFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.GENERATE_TYPEDEF_CLASS, typeDefFileName));

            /**
             * Close file handle for classes files.
             */
            JavaFileGenerator.closeFileHandles(typeDefFile);
        }

        if (!getCachedAttributeList().isEmpty()) {
            closeTempDataFileHandles(className, getCodeGenFilePath() + getRelativeFilePath());
            JavaFileGenerator
                    .cleanTempFiles(new File(getCodeGenFilePath() + getRelativeFilePath() + File.separator + className
                            + TEMP_FOLDER_NAME_SUFIX));
        }

        /*
         * clear the contents from the cached attribute list.
         */
        getCachedAttributeList().clear();
    }

    @Override
    public void setTempData(String data, TempDataStoreTypes type, String className, String genDir)
            throws IOException {

        String fileName = "";
        if (type.equals(TempDataStoreTypes.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.GETTER_METHODS)) {
            fileName = GETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.GETTER_METHODS_IMPL)) {
            fileName = GETTER_METHOD_IMPL_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.SETTER_METHODS)) {
            fileName = SETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.SETTER_METHODS_IMPL)) {
            fileName = SETTER_METHOD_IMPL_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.TYPE_DEF)) {
            fileName = TYPE_DEF_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.TO_STRING)) {
            fileName = TO_STRING_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.HASH_CODE)) {
            fileName = HASH_CODE_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.EQUALS)) {
            fileName = EQUALS_METHOD_FILE_NAME;
        } else {
            fileName = CONSTRUCTOR_FILE_NAME;
        }

        String path = genDir.replace(UtilConstants.PERIOD, UtilConstants.SLASH)
                + File.separator + className
                + TEMP_FOLDER_NAME_SUFIX + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path + fileName + TEMP_FILE_EXTENSION);
        try {
            if (!file.exists()) {
                file.createNewFile();
                JavaFileGenerator.insert(file, data);
            } else {
                JavaFileGenerator.insert(file, data);
            }
        } catch (IOException ex) {
            throw new IOException("failed to write in temp file.");
        }
    }

    @Override
    public String getTempData(TempDataStoreTypes type, String className, String genDir)
            throws IOException, FileNotFoundException, ClassNotFoundException {

        String fileName = "";
        if (type.equals(TempDataStoreTypes.ATTRIBUTE)) {
            fileName = ATTRIBUTE_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.GETTER_METHODS)) {
            fileName = GETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.GETTER_METHODS_IMPL)) {
            fileName = GETTER_METHOD_IMPL_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.SETTER_METHODS)) {
            fileName = SETTER_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.SETTER_METHODS_IMPL)) {
            fileName = SETTER_METHOD_IMPL_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.TYPE_DEF)) {
            fileName = TYPE_DEF_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.TO_STRING)) {
            fileName = TO_STRING_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.HASH_CODE)) {
            fileName = HASH_CODE_METHOD_FILE_NAME;
        } else if (type.equals(TempDataStoreTypes.EQUALS)) {
            fileName = EQUALS_METHOD_FILE_NAME;
        } else {
            fileName = CONSTRUCTOR_FILE_NAME;
        }

        String path = genDir.replace(UtilConstants.PERIOD, UtilConstants.SLASH)
                + File.separator + className + TEMP_FOLDER_NAME_SUFIX + File.separator;

        try {
            String file = path + fileName + TEMP_FILE_EXTENSION;
            if (new File(file).exists()) {
                return readFile(path + fileName + TEMP_FILE_EXTENSION);
            } else {
                return "";
            }

        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("No such file or directory.");
        }
    }

    /**
     * Reads file and convert it to string.
     *
     * @param toAppend file to be converted
     * @return string of file
     * @throws IOException when fails to convert to string
     */
    private static String readFile(String toAppend) throws IOException {
        BufferedReader bufferReader = new BufferedReader(new FileReader(toAppend));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferReader.readLine();

            while (line != null) {
                if (line.equals(UtilConstants.FOUR_SPACE_INDENTATION)
                        || line.equals(UtilConstants.EIGHT_SPACE_INDENTATION)
                        || line.equals(UtilConstants.SPACE) || line.equals("") || line.equals(UtilConstants.NEW_LINE)) {
                    stringBuilder.append("\n");
                } else {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                line = bufferReader.readLine();
            }
            return stringBuilder.toString();
        } finally {
            bufferReader.close();
        }
    }

    /**
     * Closes the temp file handles.
     *
     * @param className class name
     * @param genDir generated directory
     * @throws IOException when failes to close file handle
     */
    private void closeTempDataFileHandles(String className, String genDir)
            throws IOException {

        String path = genDir.replace(UtilConstants.PERIOD, UtilConstants.SLASH) + File.separator + className
                + TEMP_FOLDER_NAME_SUFIX + File.separator;

        String fileName = "";
        fileName = ATTRIBUTE_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = GETTER_METHOD_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = GETTER_METHOD_IMPL_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = SETTER_METHOD_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = SETTER_METHOD_IMPL_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = TYPE_DEF_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = TO_STRING_METHOD_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = HASH_CODE_METHOD_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = EQUALS_METHOD_FILE_NAME;
        closeTempFile(fileName, path);

        fileName = CONSTRUCTOR_FILE_NAME;
        closeTempFile(fileName, path);
    }

    /**
     * Closes the specific temp file.
     *
     * @param fileName temp file name
     * @param path path
     * @throws IOException when failed to close file handle
     */
    private void closeTempFile(String fileName, String path) throws IOException {
        File file = new File(path + fileName + TEMP_FILE_EXTENSION);
        try {
            if (!file.exists()) {
                FileSystemUtil.updateFileHandle(file, null, true);
            }
        } catch (IOException ex) {
            throw new IOException("failed to close the temp file handle.");
        }
    }
}
