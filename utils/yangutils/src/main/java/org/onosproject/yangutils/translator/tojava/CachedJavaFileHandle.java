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
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType;
import org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Maintain the information about the java file to be generated.
 */
public class CachedJavaFileHandle implements CachedFileHandle {

    private static final int MAX_CACHABLE_ATTR = 64;
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String TEMP_FILE_EXTENSION = ".tmp";

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
     * Typedef Info.
     */
    private YangTypeDef typedefInfo;

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

    /**
     * Set the package relative path.
     *
     * @param path package relative path
     */
    @Override
    public void setRelativeFilePath(String path) {
        relativeFilePath = path;
    }

    /**
     * Get the package relative path.
     *
     * @return package relative path
     */
    @Override
    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    /**
     * Flush the cached attribute list to the corresponding temporary file.
     */
    private void flushCacheAttrToTempFile() {

        for (AttributeInfo attr : getCachedAttributeList()) {
            JavaFileGenerator.parseAttributeInfo(attr, getGeneratedFileTypes(), getYangName());
        }

        /*
         * clear the contents from the cached attribute list.
         */
        getCachedAttributeList().clear();
    }

    /**
     * Add a new attribute to the file(s).
     *
     * @param attrType data type of the added attribute
     * @param name name of the attribute
     * @param isListAttr if the current added attribute needs to be maintained
     *            in a list
     */
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
                    + "." + getYangName());
            isImport = true;
        }

        newAttr.setQualifiedName(false);
        if (isImport) {
            boolean isNewImport = addImportInfo(importInfo);
            if (!isNewImport) {
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

    /**
     * Flushes the cached contents to the target file, frees used resources.
     */
    @Override
    public void close() throws IOException {

        flushCacheAttrToTempFile();

        String className = getYangName();
        className = JavaIdentifierSyntax.getCaptialCase(className);
        String path = getRelativeFilePath();
        int fileType = getGeneratedFileTypes();

        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */

        List<String> imports = new LinkedList<>();
        String importString;

        for (ImportInfo importInfo : getImportSet()) {
            importString = "";
            if (importInfo.getPkgInfo() != null) {
                importString = importString + importInfo.getPkgInfo() + ".";
            }
            importString = importString + importInfo.getClassInfo();
            imports.add(importString);
        }

        /**
         * Start generation of files.
         */
        if ((fileType & GeneratedFileType.INTERFACE_MASK) != 0
                || fileType == GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create interface file.
             */
            String interfaceFileName = className;
            File interfaceFile = JavaFileGenerator.getFileObject(path, interfaceFileName, JAVA_FILE_EXTENSION);
            interfaceFile = JavaFileGenerator.generateInterfaceFile(interfaceFile, className, imports,
                    getCachedAttributeList(), path.replace('/', '.'));

            /**
             * Create temp builder interface file.
             */
            String builderInterfaceFileName = className + UtilConstants.BUILDER + UtilConstants.INTERFACE;
            File builderInterfaceFile = JavaFileGenerator.getFileObject(path, builderInterfaceFileName,
                    TEMP_FILE_EXTENSION);
            builderInterfaceFile = JavaFileGenerator.generateBuilderInterfaceFile(builderInterfaceFile, className,
                    path.replace('/', '.'), getCachedAttributeList());

            /**
             * Append builder interface file to interface file and close it.
             */
            JavaFileGenerator.appendFileContents(builderInterfaceFile, interfaceFile);
            JavaFileGenerator.insert(interfaceFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.INTERFACE_MASK, interfaceFileName));

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(builderInterfaceFile);
        }

        if ((fileType & GeneratedFileType.BUILDER_CLASS_MASK) != 0
                || fileType == GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create builder class file.
             */
            String builderFileName = className + UtilConstants.BUILDER;
            File builderFile = JavaFileGenerator.getFileObject(path, builderFileName, JAVA_FILE_EXTENSION);
            builderFile = JavaFileGenerator.generateBuilderClassFile(builderFile, className, imports,
                    path.replace('/', '.'), getCachedAttributeList());

            /**
             * Create temp impl class file.
             */

            String implFileName = className + UtilConstants.IMPL;
            File implTempFile = JavaFileGenerator.getFileObject(path, implFileName, TEMP_FILE_EXTENSION);
            implTempFile = JavaFileGenerator.generateImplClassFile(implTempFile, className,
                    path.replace('/', '.'), getCachedAttributeList());

            /**
             * Append impl class to builder class and close it.
             */
            JavaFileGenerator.appendFileContents(implTempFile, builderFile);
            JavaFileGenerator.insert(builderFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.BUILDER_CLASS_MASK, builderFileName));

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(implTempFile);
        }
    }

    public YangTypeDef getTypedefInfo() {
        return typedefInfo;
    }

    public void setTypedefInfo(YangTypeDef typedefInfo) {
        this.typedefInfo = typedefInfo;
    }
}
