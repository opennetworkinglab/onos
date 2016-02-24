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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType;
import org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen;
import org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator;
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
    private GeneratedFileType genFileTypes;

    /**
     * Java package in which the class/interface needs to be generated.
     */
    private String pkg;

    /**
     * Java package in which the child class/interface needs to be generated.
     */
    private String childsPkg;

    /**
     * Name of the object in YANG file.
     */
    private String yangName;

    /**
     * Sorted set of import info, to be used to maintain the set of classes to
     * be imported in the generated class.
     */
    private SortedSet<String> importSet;

    /**
     * Cached list of attribute info.
     */
    private List<AttributeInfo> attributeList;

    /**
     * File generation directory path.
     */
    private String filePath;

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
     * @param pcg package in which class/interface need to be generated.
     * @param yangName name of the attribute in YANG file.
     * @param types the types of files that needs to be generated.
     * @throws IOException file IO exception.
     */
    public CachedJavaFileHandle(String pcg, String yangName, GeneratedFileType types) throws IOException {
        setGeneratedFileTypes(types);
        setPackage(pcg);
        setYangName(yangName);
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
     * Get the java package.
     *
     * @return the java package.
     */
    public String getChildsPackage() {
        return childsPkg;
    }

    @Override
    public void setChildsPackage(String pcg) {
        childsPkg = pcg;
    }

    /**
     * Get the set containing the imported class/interface info.
     *
     * @return the set containing the imported class/interface info.
     */
    public SortedSet<String> getImportSet() {
        return importSet;
    }

    /**
     * Assign the set containing the imported class/interface info.
     *
     * @param importSet the set containing the imported class/interface info.
     */
    private void setImportSet(SortedSet<String> importSet) {
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
            setImportSet(new TreeSet<String>());
        }
        return getImportSet().add(JavaCodeSnippetGen.getImportText(importInfo));
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

    @Override
    public void setFilePath(String path) {
        filePath = path;
    }

    /**
     * Set the cached attribute list.
     *
     * @param attrList attribute list.
     */
    private String getFilePath() {
        return filePath;
    }

    /**
     * Flush the cached attribute list to the serialized file.
     */
    private void flushCacheAttrToSerFile(String className) {

        for (AttributeInfo attr : getCachedAttributeList()) {
            JavaFileGenerator.parseAttributeInfo(attr, getGeneratedFileTypes(), className);
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
        if (attrType != null) {
            newAttr.setAttributeType(attrType);
        } else {
            ImportInfo importInfo = new ImportInfo();
            importInfo.setPkgInfo(getChildsPackage());
            importInfo.setClassInfo(JavaIdentifierSyntax.getCaptialCase(name));
            if (getImportSet() != null) {
                getImportSet().add(JavaCodeSnippetGen.getImportText(importInfo));
            } else {
                SortedSet<String> newImportInfo = new TreeSet<>();
                newImportInfo.add(JavaCodeSnippetGen.getImportText(importInfo));
                setImportSet(newImportInfo);
            }

            newAttr.setQualifiedName(getQualifiedFlag(JavaCodeSnippetGen.getImportText(importInfo)));
        }
        newAttr.setAttributeName(name);
        newAttr.setListAttr(isListAttr);

        if (newAttr.isListAttr()) {
            newAttr.setAttributeType(AttributesJavaDataType.getListString(newAttr));
        }

        if (isListAttr) {
            String listImport = UtilConstants.COLLECTION_IMPORTS + UtilConstants.LIST + UtilConstants.SEMI_COLAN
                    + UtilConstants.NEW_LINE + UtilConstants.NEW_LINE;
            if (getImportSet() != null) {
                getImportSet().add(listImport);
            } else {
                SortedSet<String> newImportInfo = new TreeSet<>();
                newImportInfo.add(listImport);
                setImportSet(newImportInfo);
            }

            newAttr.setQualifiedName(getQualifiedFlag(listImport));
        }

        if (getCachedAttributeList() != null) {
            if (getCachedAttributeList().size() == MAX_CACHABLE_ATTR) {
                flushCacheAttrToSerFile(getYangName());
            }
            getCachedAttributeList().add(newAttr);
        } else {
            List<AttributeInfo> newAttributeInfo = new LinkedList<>();
            newAttributeInfo.add(newAttr);
            setCachedAttributeList(newAttributeInfo);
        }
    }

    /**
     * Check if the import set does not have a class info same as the new class
     * info, if so the new class info be added to the import set. Otherwise
     * check if the corresponding package info is same as the new package info,
     * if so no need to qualified access, otherwise, it needs qualified access.
     *
     * @param newImportInfo new import info to be check for qualified access or
     *            not and updated in the import set accordingly.
     * @return if the new attribute needs to be accessed in a qualified manner.
     */
    private boolean getQualifiedFlag(String newImportInfo) {
        for (String curImportInfo : getImportSet()) {
            if (curImportInfo.equals(newImportInfo)) {
                /*
                 * If import is already existing import with same package, we
                 * don't need qualified access, otherwise it needs to be
                 * qualified access.
                 */
                return !curImportInfo.equals(newImportInfo);
            }
        }

        getImportSet().add(newImportInfo);
        return false;
    }

    /**
     * Flushes the cached contents to the target file, frees used resources.
     */
    @Override
    public void close() throws IOException {

        String className = getYangName();
        className = JavaIdentifierSyntax.getCaptialCase(className);
        String filePath = getFilePath();
        GeneratedFileType fileType = getGeneratedFileTypes();

        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */

        List<String> imports = new LinkedList<>();

        if (getCachedAttributeList() != null) {
            MethodsGenerator.setAttrInfo(getCachedAttributeList());
            for (AttributeInfo attr : getCachedAttributeList()) {

                if (getImportSet() != null) {
                    imports = new ArrayList<>(getImportSet());
                }
            }
        }

        /**
         * Start generation of files.
         */
        if (fileType.equals(GeneratedFileType.INTERFACE) || fileType.equals(GeneratedFileType.ALL)) {

            /**
             * Create interface file.
             */
            String interfaceFileName = className;
            File interfaceFile = new File(filePath + File.separator + interfaceFileName + JAVA_FILE_EXTENSION);
            interfaceFile = JavaFileGenerator.generateInterfaceFile(interfaceFile, className, imports,
                    getCachedAttributeList(), getPackage());

            /**
             * Create temp builder interface file.
             */
            String builderInterfaceFileName = className + UtilConstants.BUILDER + UtilConstants.INTERFACE;
            File builderInterfaceFile = new File(
                    filePath + File.separator + builderInterfaceFileName + TEMP_FILE_EXTENSION);
            builderInterfaceFile = JavaFileGenerator.generateBuilderInterfaceFile(builderInterfaceFile, className,
                    getPackage(), getCachedAttributeList());

            /**
             * Append builder interface file to interface file and close it.
             */
            JavaFileGenerator.appendFileContents(builderInterfaceFile, interfaceFile);
            JavaFileGenerator.insert(interfaceFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.INTERFACE, interfaceFileName));

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(builderInterfaceFile);
        }

        if (fileType.equals(GeneratedFileType.BUILDER_CLASS) || fileType.equals(GeneratedFileType.ALL)) {

            /**
             * Create builder class file.
             */
            String builderFileName = className + UtilConstants.BUILDER;
            File builderFile = new File(filePath + File.separator + builderFileName + JAVA_FILE_EXTENSION);
            MethodsGenerator.setBuilderClassName(className + UtilConstants.BUILDER);

            builderFile = JavaFileGenerator.generateBuilderClassFile(builderFile, className, imports, getPackage(),
                    getCachedAttributeList());

            /**
             * Create temp impl class file.
             */

            String implFileName = className + UtilConstants.IMPL;
            File implTempFile = new File(filePath + File.separator + implFileName + TEMP_FILE_EXTENSION);
            implTempFile = JavaFileGenerator.generateImplClassFile(implTempFile, className, getPackage(),
                    getCachedAttributeList());

            /**
             * Append impl class to builder class and close it.
             */
            JavaFileGenerator.appendFileContents(implTempFile, builderFile);
            JavaFileGenerator.insert(builderFile,
                    JavaFileGenerator.closeFile(GeneratedFileType.BUILDER_CLASS, builderFileName));

            /**
             * Remove temp files.
             */
            JavaFileGenerator.clean(implTempFile);
        }
    }
}
