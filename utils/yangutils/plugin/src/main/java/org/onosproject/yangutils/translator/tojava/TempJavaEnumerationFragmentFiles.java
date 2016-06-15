/*
 * Copyright 2016-present Open Networking Laboratory
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangEnum;
import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaType;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ENUM_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.generateEnumAttributeString;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEnumClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getEnumJavaAttribute;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPrefixForIdentifier;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_FIRST_DIGIT;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUTO_PREFIX;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;

/**
 * Represents implementation of java code fragments temporary implementations.
 * Maintains the temp files required specific for enumeration java snippet generation.
 */
public class TempJavaEnumerationFragmentFiles extends TempJavaFragmentFiles {

    /**
     * File name for temporary enum class.
     */
    private static final String ENUM_CLASS_TEMP_FILE_NAME = "EnumClass";

    /**
     * File name for enum class file name suffix.
     */
    private static final String ENUM_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * Current enum's value.
     */
    private int enumValue;

    /**
     * Contains data of enumSet.
     */
    private Map<String, Integer> enumStringMap = new HashMap<>();

    /**
     * Contains data of enumSet.
     */
    private List<String> enumStringList;

    /**
     * Temporary file handle for enum class file.
     */
    private File enumClassTempFileHandle;

    /**
     * Java file handle for enum class.
     */
    private File enumClassJavaFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file info
     * @throws IOException when fails to create new file handle
     */
    public TempJavaEnumerationFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {

        super(javaFileInfo);
        setEnumSetJavaMap(new HashMap<>());
        setEnumStringList(new ArrayList<>());
        /*
         * Initialize enum when generation file type matches to enum class mask.
         */
        addGeneratedTempFile(ENUM_IMPL_MASK);
        setEnumClassTempFileHandle(getTemporaryFileHandle(ENUM_CLASS_TEMP_FILE_NAME));
    }

    /**
     * Returns enum class java file handle.
     *
     * @return enum class java file handle
     */
    public File getEnumClassJavaFileHandle() {
        return enumClassJavaFileHandle;
    }

    /**
     * Sets enum class java file handle.
     *
     * @param enumClassJavaFileHandle enum class java file handle
     */
    private void setEnumClassJavaFileHandle(File enumClassJavaFileHandle) {
        this.enumClassJavaFileHandle = enumClassJavaFileHandle;
    }

    /**
     * Returns enum's value.
     *
     * @return enum's value
     */
    private int getEnumValue() {
        return enumValue;
    }

    /**
     * Sets enum's value.
     *
     * @param enumValue enum's value
     */
    private void setEnumValue(int enumValue) {
        this.enumValue = enumValue;
    }

    /**
     * Returns enum set java map.
     *
     * @return the enum set java map
     */
    public Map<String, Integer> getEnumSetJavaMap() {
        return enumStringMap;
    }

    /**
     * Sets enum set java map.
     *
     * @param map the enum set java map to set
     */
    private void setEnumSetJavaMap(Map<String, Integer> map) {
        this.enumStringMap = map;
    }

    /**
     * Returns temporary file handle for enum class file.
     *
     * @return temporary file handle for enum class file
     */
    public File getEnumClassTempFileHandle() {
        return enumClassTempFileHandle;
    }

    /**
     * Sets temporary file handle for enum class file.
     *
     * @param enumClassTempFileHandle temporary file handle for enum class file
     */
    private void setEnumClassTempFileHandle(File enumClassTempFileHandle) {
        this.enumClassTempFileHandle = enumClassTempFileHandle;
    }

    /**
     * Adds enum class attributes to temporary file.
     *
     * @param curEnumName current YANG enum
     * @throws IOException when fails to do IO operations.
     */
    private void addAttributesForEnumClass(String curEnumName, YangPluginConfig pluginConfig) throws IOException {
        appendToFile(getEnumClassTempFileHandle(),
                generateEnumAttributeString(curEnumName, getEnumValue(), pluginConfig));
    }

    /**
     * Adds enum attributes to temporary files.
     *
     * @param curNode current YANG node
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to do IO operations
     */
    public void addEnumAttributeToTempFiles(YangNode curNode, YangPluginConfig pluginConfig) throws IOException {

        super.addJavaSnippetInfoToApplicableTempFiles(getJavaAttributeForEnum(pluginConfig), pluginConfig);
        if (curNode instanceof YangEnumeration) {
            YangEnumeration enumeration = (YangEnumeration) curNode;
            for (YangEnum curEnum : enumeration.getEnumSet()) {
                String enumName = curEnum.getNamedValue();
                String prefixForIdentifier = null;
                if (enumName.matches(REGEX_FOR_FIRST_DIGIT)) {
                    prefixForIdentifier = getPrefixForIdentifier(pluginConfig.getConflictResolver());
                    if (prefixForIdentifier != null) {
                        curEnum.setNamedValue(prefixForIdentifier + enumName);
                    } else {
                        curEnum.setNamedValue(YANG_AUTO_PREFIX + enumName);
                    }
                }
                setEnumValue(curEnum.getValue());
                addToEnumStringList(curEnum.getNamedValue());
                addToEnumSetJavaMap(curEnum.getNamedValue(), curEnum.getValue());
                addJavaSnippetInfoToApplicableTempFiles(curEnum.getNamedValue(), pluginConfig);
            }
        } else {
            throw new TranslatorException("current node should be of enumeration type.");
        }
    }

    /**
    * Returns java attribute for enum class.
    *
    * @param pluginConfig plugin configurations
    * @return java attribute
    */
    public JavaAttributeInfo getJavaAttributeForEnum(YangPluginConfig pluginConfig) {
        YangJavaType<?> javaType = new YangJavaType<>();
        javaType.setDataType(YangDataTypes.INT32);
        javaType.setDataTypeName("int");
        javaType.updateJavaQualifiedInfo(pluginConfig.getConflictResolver());
        return getAttributeInfoForTheData(
                javaType.getJavaQualifiedInfo(),
                javaType.getDataTypeName(), javaType,
                getIsQualifiedAccessOrAddToImportList(javaType.getJavaQualifiedInfo()),
                false);
    }

    /**
     * Adds current enum name to java list.
     *
     * @param curEnumName current enum name
     */
    private void addToEnumSetJavaMap(String curEnumName, int value) {
        getEnumSetJavaMap().put(getEnumJavaAttribute(curEnumName).toUpperCase(), value);
    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param curEnumName the attribute name that needs to be added to temporary
     * files
     * @throws IOException IO operation fail
     */
    void addJavaSnippetInfoToApplicableTempFiles(String curEnumName, YangPluginConfig pluginConfig)
            throws IOException {
        addAttributesForEnumClass(getEnumJavaAttribute(curEnumName), pluginConfig);
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    @Override
    public void generateJavaFile(int fileType, YangNode curNode) throws IOException {
        createPackage(curNode);
        setEnumClassJavaFileHandle(getJavaFileHandle(getJavaClassName(ENUM_CLASS_FILE_NAME_SUFFIX)));
        setEnumClassJavaFileHandle(generateEnumClassFile(getEnumClassJavaFileHandle(), curNode));
        freeTemporaryResources(false);
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we
     * need to close all open file handles include temporary files
     * and java files.
     * @throws IOException when failed to delete the temporary files
     */
    @Override
    public void freeTemporaryResources(boolean isErrorOccurred) throws IOException {
        closeFile(getEnumClassJavaFileHandle(), isErrorOccurred);
        closeFile(getEnumClassTempFileHandle(), true);
        super.freeTemporaryResources(isErrorOccurred);
    }

    /**
     * Adds  to enum string list.
     *
     * @param curEnumValue current enum value
     */
    private void addToEnumStringList(String curEnumValue) {
        getEnumStringList().add(getEnumJavaAttribute(curEnumValue).toUpperCase());
    }

    /**
     * Returns enum string list.
     *
     * @return the enumStringList
     */
    public List<String> getEnumStringList() {
        return enumStringList;
    }

    /**
     * Sets enum string list.
     *
     * @param enumStringList the enumStringList to set
     */
    public void setEnumStringList(List<String> enumStringList) {
        this.enumStringList = enumStringList;
    }
}
