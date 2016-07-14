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
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaGrouping;
import org.onosproject.yangutils.translator.tojava.utils.JavaExtendsListHolder;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_ENUM_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_METHOD_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_ATTRIBUTE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_GETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_SETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventListenerFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventSubjectFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getEnumJavaAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_ATTRIBUTE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.MANAGER_SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;

/**
 * Represent temporary java fragments for event files.
 */
public class TempJavaEventFragmentFiles
        extends TempJavaFragmentFiles {
    /**
     * File name for generated class file for special type like union, typedef suffix.
     */
    public static final String EVENT_SUBJECT_NAME_SUFFIX = "EventSubject";

    /**
     * File name for event enum temp file.
     */

    private static final String EVENT_ENUM_FILE_NAME = "EventEnum";

    /**
     * File name for event method temp file.
     */
    private static final String EVENT_METHOD_FILE_NAME = "EventMethod";

    /**
     * File name for event subject attribute temp file.
     */
    private static final String EVENT_SUBJECT_ATTRIBUTE_FILE_NAME = "EventSubjectAttribute";

    /**
     * File name for event subject getter temp file.
     */
    private static final String EVENT_SUBJECT_GETTER_FILE_NAME = "EventSubjectGetter";

    /**
     * File name for event subject setter temp file.
     */
    private static final String EVENT_SUBJECT_SETTER_FILE_NAME = "EventSubjectSetter";

    /**
     * File name for generated class file for special type like union, typedef suffix.
     */
    private static final String EVENT_FILE_NAME_SUFFIX = "Event";

    /**
     * File name for generated class file for special type like union, typedef suffix.
     */
    private static final String EVENT_LISTENER_FILE_NAME_SUFFIX = "Listener";

    private static final String JAVA_FILE_EXTENSION = ".java";

    /**
     * Java file handle for event subject file.
     */
    private File eventSubjectJavaFileHandle;

    /**
     * Java file handle for event listener file.
     */
    private File eventListenerJavaFileHandle;

    /**
     * Java file handle for event file.
     */
    private File eventJavaFileHandle;

    /**
     * Java file handle for event enum impl file.
     */
    private File eventEnumTempFileHandle;

    /**
     * Java file handle for event method impl file.
     */
    private File eventMethodTempFileHandle;

    /**
     * Java file handle for event subject attribute file.
     */
    private File eventSubjectAttributeTempFileHandle;

    /**
     * Java file handle for event subject getter impl file.
     */
    private File eventSubjectGetterTempFileHandle;

    /**
     * Java file handle for event subject setter impl file.
     */
    private File eventSubjectSetterTempFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated file information
     * @throws IOException when fails to create new file handle
     */
    public TempJavaEventFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {
        setJavaExtendsListHolder(new JavaExtendsListHolder());
        setJavaImportData(new JavaImportData());
        setJavaFileInfo(javaFileInfo);
        setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath()));

        addGeneratedTempFile(EVENT_ENUM_MASK);
        addGeneratedTempFile(EVENT_METHOD_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_ATTRIBUTE_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_GETTER_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_SETTER_MASK);

        setEventEnumTempFileHandle(getTemporaryFileHandle(EVENT_ENUM_FILE_NAME));
        setEventMethodTempFileHandle(getTemporaryFileHandle(EVENT_METHOD_FILE_NAME));
        setEventSubjectAttributeTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_ATTRIBUTE_FILE_NAME));
        setEventSubjectGetterTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_GETTER_FILE_NAME));
        setEventSubjectSetterTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_SETTER_FILE_NAME));
    }

    /*Adds event method contents to event file.*/
    private static String getEventFileContents(String eventClassname, String classname) {
        return "\n" +
                "    /**\n" +
                "     * Creates " + classname + " event with type and subject.\n" +
                "     *\n" +
                "     * @param type event type\n" +
                "     * @param subject subject " + classname + "\n" +
                "     */\n" +
                "    public " + eventClassname + "(Type type, " + getCapitalCase(classname) + " subject) {\n" +
                "        super(type, subject);\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Creates " + classname + " event with type, subject and time.\n" +
                "     *\n" +
                "     * @param type event type\n" +
                "     * @param subject subject " + classname + "\n" +
                "     * @param time time of event\n" +
                "     */\n" +
                "    public " + eventClassname + "(Type type, " + getCapitalCase(classname)
                + " subject, long time) {\n" +
                "        super(type, subject, time);\n" +
                "    }\n" +
                "\n";
    }

    /**
     * Returns event's java file handle.
     *
     * @return java file handle
     */
    private File getEventJavaFileHandle() {
        return eventJavaFileHandle;
    }

    /**
     * Sets event's java file handle.
     *
     * @param eventJavaFileHandle file handle for event
     */
    private void setEventJavaFileHandle(File eventJavaFileHandle) {
        this.eventJavaFileHandle = eventJavaFileHandle;
    }

    /**
     * Returns event listeners's java file handle.
     *
     * @return java file handle
     */
    private File getEventListenerJavaFileHandle() {
        return eventListenerJavaFileHandle;
    }

    /**
     * Sets event's java file handle.
     *
     * @param eventListenerJavaFileHandle file handle for event
     */
    private void setEventListenerJavaFileHandle(File eventListenerJavaFileHandle) {
        this.eventListenerJavaFileHandle = eventListenerJavaFileHandle;
    }

    /**
     * Returns event subject's java file handle.
     *
     * @return java file handle
     */
    private File getEventSubjectJavaFileHandle() {
        return eventSubjectJavaFileHandle;
    }

    /**
     * Sets event's subject java file handle.
     *
     * @param eventSubjectJavaFileHandle file handle for event's subject
     */
    private void setEventSubjectJavaFileHandle(File eventSubjectJavaFileHandle) {
        this.eventSubjectJavaFileHandle = eventSubjectJavaFileHandle;
    }

    public void generateJavaFile(int fileType, YangNode curNode) throws IOException {
        generateEventJavaFile(curNode);
        generateEventListenerJavaFile(curNode);
        generateEventSubjectJavaFile(curNode);
    }

    /**
     * Constructs java code exit.
     *
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventJavaFile(YangNode curNode)
            throws IOException {

        List<String> imports = new ArrayList<>();

        imports.add(getJavaImportData().getAbstractEventsImport());
        String curNodeInfo = getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName());
        String nodeName = curNodeInfo + EVENT_STRING;

        addEnumMethod(nodeName, curNodeInfo + EVENT_SUBJECT_NAME_SUFFIX);

        /**
         * Creates event interface file.
         */
        setEventJavaFileHandle(getJavaFileHandle(curNode, curNodeInfo + EVENT_FILE_NAME_SUFFIX));
        generateEventFile(getEventJavaFileHandle(), curNode, imports);

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Constructs java code exit.
     *
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventListenerJavaFile(YangNode curNode)
            throws IOException {

        List<String> imports = new ArrayList<>();

        imports.add(getJavaImportData().getEventListenerImport());
        String curNodeInfo = getCapitalCase(((JavaFileInfoContainer) curNode)
                .getJavaFileInfo().getJavaName());
        /**
         * Creates event listener interface file.
         */
        setEventListenerJavaFileHandle(
                getJavaFileHandle(curNode, curNodeInfo + EVENT_LISTENER_FILE_NAME_SUFFIX));
        generateEventListenerFile(getEventListenerJavaFileHandle(), curNode, imports);

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Constructs java code exit.
     *
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventSubjectJavaFile(YangNode curNode)
            throws IOException {

        String curNodeInfo = getCapitalCase(((JavaFileInfoContainer) curNode)
                .getJavaFileInfo().getJavaName());
        /**
         * Creates event interface file.
         */
        setEventSubjectJavaFileHandle(getJavaFileHandle(curNode, curNodeInfo +
                TempJavaEventFragmentFiles.EVENT_SUBJECT_NAME_SUFFIX));
        generateEventSubjectFile(getEventSubjectJavaFileHandle(), curNode);

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Returns event enum temp file.
     *
     * @return event enum temp file
     */
    public File getEventEnumTempFileHandle() {
        return eventEnumTempFileHandle;
    }

    /**
     * Sets event enum temp file.
     *
     * @param eventEnumTempFileHandle event enum temp file
     */
    public void setEventEnumTempFileHandle(File eventEnumTempFileHandle) {
        this.eventEnumTempFileHandle = eventEnumTempFileHandle;
    }

    /**
     * Returns event method temp file.
     *
     * @return event method temp file
     */
    public File getEventMethodTempFileHandle() {
        return eventMethodTempFileHandle;
    }

    /**
     * Sets event method temp file.
     *
     * @param eventMethodTempFileHandle event method temp file
     */
    public void setEventMethodTempFileHandle(File eventMethodTempFileHandle) {
        this.eventMethodTempFileHandle = eventMethodTempFileHandle;
    }

    /**
     * Returns event subject attribute temp file.
     *
     * @return event subject attribute temp file
     */
    public File getEventSubjectAttributeTempFileHandle() {
        return eventSubjectAttributeTempFileHandle;
    }

    /**
     * Sets event subject attribute temp file.
     *
     * @param eventSubjectAttributeTempFileHandle event subject attribute temp file
     */
    public void setEventSubjectAttributeTempFileHandle(File eventSubjectAttributeTempFileHandle) {
        this.eventSubjectAttributeTempFileHandle = eventSubjectAttributeTempFileHandle;
    }

    /**
     * Returns event subject getter temp file.
     *
     * @return event subject getter temp file
     */
    public File getEventSubjectGetterTempFileHandle() {
        return eventSubjectGetterTempFileHandle;
    }

    /**
     * Sets event subject getter temp file.
     *
     * @param eventSubjectGetterTempFileHandle event subject getter temp file
     */
    public void setEventSubjectGetterTempFileHandle(File eventSubjectGetterTempFileHandle) {
        this.eventSubjectGetterTempFileHandle = eventSubjectGetterTempFileHandle;
    }

    /**
     * Returns event subject setter temp file.
     *
     * @return event subject setter temp file
     */
    public File getEventSubjectSetterTempFileHandle() {
        return eventSubjectSetterTempFileHandle;
    }

    /**
     * Sets event subject setter temp file.
     *
     * @param eventSubjectSetterTempFileHandle event subject setter temp file
     */
    public void setEventSubjectSetterTempFileHandle(File eventSubjectSetterTempFileHandle) {
        this.eventSubjectSetterTempFileHandle = eventSubjectSetterTempFileHandle;
    }

    /**
     * Adds java snippet for events to event subject file.
     *
     * @param curNode      current node
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to do IO operations
     */
    public void addJavaSnippetOfEvent(YangNode curNode, YangPluginConfig pluginConfig)
            throws IOException {

        String currentInfo = getCapitalCase(getCamelCase(curNode.getName(),
                pluginConfig.getConflictResolver()));
        String notificationName = curNode.getName();

        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(curNode,
                getCapitalCase(currentInfo));

        JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(qualifiedTypeInfo, getSmallCase(currentInfo),
                null, false, false);

        /*Adds java info for event in respective temp files.*/
        addEventEnum(notificationName, pluginConfig);
        addEventSubjectAttribute(javaAttributeInfo, pluginConfig);
        addEventSubjectGetter(javaAttributeInfo, pluginConfig);
        addEventSubjectSetter(javaAttributeInfo, pluginConfig, currentInfo);
    }

    /*Adds event to enum temp file.*/
    private void addEventEnum(String notificationName, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getEventEnumTempFileHandle(),
                getJavaDoc(ENUM_ATTRIBUTE, notificationName, false, pluginConfig) + FOUR_SPACE_INDENTATION
                        + getEnumJavaAttribute(notificationName).toUpperCase() + COMMA + NEW_LINE);
    }

    /*Adds event method in event class*/
    private void addEnumMethod(String eventClassname, String className)
            throws IOException {
        appendToFile(getEventMethodTempFileHandle(), getEventFileContents(eventClassname, className));
    }

    /*Adds events to event subject file.*/
    private void addEventSubjectAttribute(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getEventSubjectAttributeTempFileHandle(),
                FOUR_SPACE_INDENTATION + parseAttribute(attr, pluginConfig));
    }

    /*Adds getter method for event in event subject class.*/
    private void addEventSubjectGetter(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getEventSubjectGetterTempFileHandle(),
                getJavaDoc(GETTER_METHOD, getCapitalCase(attr.getAttributeName()), false, pluginConfig)
                        + getGetterForClass(attr, GENERATE_EVENT_SUBJECT_CLASS) + NEW_LINE);
    }

    /*Adds setter method for event in event subject class.*/
    private void addEventSubjectSetter(JavaAttributeInfo attr, YangPluginConfig pluginConfig, String className)
            throws IOException {
        appendToFile(getEventSubjectSetterTempFileHandle(),
                getJavaDoc(MANAGER_SETTER_METHOD, getCapitalCase(attr.getAttributeName()), false, pluginConfig)
                        + getSetterForClass(attr, className, GENERATE_EVENT_SUBJECT_CLASS) + NEW_LINE);
    }

    /**
     * Returns a temporary file handle for the event's file type.
     *
     * @param name file name
     * @return temporary file handle
     * @throws IOException when fails to create new file handle
     */
    private File getJavaFileHandle(YangNode curNode, String name)
            throws IOException {

        JavaFileInfo parentInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangNode childNode = curNode.getChild();

        // Skip grouping, as it wont have the package name.
        while (childNode instanceof YangJavaGrouping) {
            childNode = childNode.getNextSibling();
        }
        JavaFileInfo childInfo = ((JavaFileInfoContainer) childNode).getJavaFileInfo();
        return getFileObject(getDirPath(parentInfo), name, JAVA_FILE_EXTENSION,
                childInfo.getBaseCodeGenPath());
    }

    /**
     * Returns the directory path.
     *
     * @return directory path
     */
    private String getDirPath(JavaFileInfo parentInfo) {
        return (parentInfo.getPackageFilePath() + SLASH + parentInfo.getJavaName()).toLowerCase();
    }
}
