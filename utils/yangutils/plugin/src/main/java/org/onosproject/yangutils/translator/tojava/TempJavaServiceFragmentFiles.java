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
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModule;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModule;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_LISTENER_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_ENUM_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_METHOD_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_ATTRIBUTE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_GETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_SETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventListenerFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventSubjectFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateManagerClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateServiceInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getEnumJavaAttribute;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcManagerMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcServiceMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addAnnotationsImports;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addListnersImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_REG;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_INPUT_VAR_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateJavaDocForRpc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_ATTRIBUTE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.MANAGER_SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;

/**
 * Represents implementation of java service code fragments temporary implementations.
 * Maintains the temp files required specific for service and manager java snippet generation.
 */
public class TempJavaServiceFragmentFiles
        extends TempJavaFragmentFiles {

    /**
     * File name for rpc method.
     */
    private static final String RPC_INTERFACE_FILE_NAME = "Rpc";

    /**
     * File name for rpc implementation method.
     */
    private static final String RPC_IMPL_FILE_NAME = "RpcImpl";

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
     * File name for generated class file for service
     * suffix.
     */
    private static final String SERVICE_FILE_NAME_SUFFIX = "Service";

    /**
     * File name for generated class file for manager
     * suffix.
     */
    private static final String MANAGER_FILE_NAME_SUFFIX = "Manager";

    /**
     * File name for generated class file for special type like union, typedef
     * suffix.
     */
    private static final String EVENT_FILE_NAME_SUFFIX = "Event";

    /**
     * File name for generated class file for special type like union, typedef
     * suffix.
     */
    private static final String EVENT_LISTENER_FILE_NAME_SUFFIX = "Listener";

    /**
     * File name for generated class file for special type like union, typedef
     * suffix.
     */
    public static final String EVENT_SUBJECT_NAME_SUFFIX = "EventSubject";

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
     * Temporary file handle for rpc interface.
     */
    private File rpcInterfaceTempFileHandle;

    /**
     * Temporary file handle for rpc manager impl.
     */
    private File rpcImplTempFileHandle;

    /**
     * Java file handle for rpc interface file.
     */
    private File serviceInterfaceJavaFileHandle;

    /**
     * Java file handle for manager impl file.
     */
    private File managerJavaFileHandle;

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
     * Returns rpc method's java file handle.
     *
     * @return java file handle
     */
    private File getServiceInterfaceJavaFileHandle() {
        return serviceInterfaceJavaFileHandle;
    }

    /**
     * Sets rpc method's java file handle.
     *
     * @param serviceInterfaceJavaFileHandle file handle for to rpc method
     */
    private void setServiceInterfaceJavaFileHandle(File serviceInterfaceJavaFileHandle) {
        this.serviceInterfaceJavaFileHandle = serviceInterfaceJavaFileHandle;
    }

    /**
     * Returns managers java file handle.
     *
     * @return java file handle
     */
    public File getManagerJavaFileHandle() {
        return managerJavaFileHandle;
    }

    /**
     * Sets manager java file handle.
     *
     * @param managerJavaFileHandle file handle for to manager
     */
    public void setManagerJavaFileHandle(File managerJavaFileHandle) {
        this.managerJavaFileHandle = managerJavaFileHandle;
    }

    /**
     * Returns rpc method's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getRpcInterfaceTempFileHandle() {
        return rpcInterfaceTempFileHandle;
    }

    /**
     * Sets rpc method's temporary file handle.
     *
     * @param rpcInterfaceTempFileHandle file handle for to rpc method
     */
    private void setRpcInterfaceTempFileHandle(File rpcInterfaceTempFileHandle) {
        this.rpcInterfaceTempFileHandle = rpcInterfaceTempFileHandle;
    }

    /**
     * Retrieves the manager impl temp file.
     *
     * @return the manager impl temp file
     */
    public File getRpcImplTempFileHandle() {
        return rpcImplTempFileHandle;
    }

    /**
     * Sets the manager impl temp file.
     *
     * @param rpcImplTempFileHandle the manager impl temp file
     */
    public void setRpcImplTempFileHandle(File rpcImplTempFileHandle) {
        this.rpcImplTempFileHandle = rpcImplTempFileHandle;
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

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated file information
     * @throws IOException when fails to create new file handle
     */
    public TempJavaServiceFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {
        super(javaFileInfo);

        addGeneratedTempFile(RPC_INTERFACE_MASK);
        addGeneratedTempFile(RPC_IMPL_MASK);

        addGeneratedTempFile(EVENT_ENUM_MASK);
        addGeneratedTempFile(EVENT_METHOD_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_ATTRIBUTE_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_GETTER_MASK);
        addGeneratedTempFile(EVENT_SUBJECT_SETTER_MASK);

        setRpcInterfaceTempFileHandle(getTemporaryFileHandle(RPC_INTERFACE_FILE_NAME));
        setRpcImplTempFileHandle(getTemporaryFileHandle(RPC_IMPL_FILE_NAME));

        setEventEnumTempFileHandle(getTemporaryFileHandle(EVENT_ENUM_FILE_NAME));
        setEventMethodTempFileHandle(getTemporaryFileHandle(EVENT_METHOD_FILE_NAME));
        setEventSubjectAttributeTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_ATTRIBUTE_FILE_NAME));
        setEventSubjectGetterTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_GETTER_FILE_NAME));
        setEventSubjectSetterTempFileHandle(getTemporaryFileHandle(EVENT_SUBJECT_SETTER_FILE_NAME));
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    @Override
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {
        List<String> imports = getJavaImportData().getImports();

        createPackage(curNode);

        boolean isNotification = false;
        if (curNode instanceof YangJavaModule) {
            if (!((YangJavaModule) curNode).getNotificationNodes().isEmpty()) {
                isNotification = true;
            }
        } else if (curNode instanceof YangJavaSubModule) {
            if (!((YangJavaSubModule) curNode).getNotificationNodes().isEmpty()) {
                isNotification = true;
            }
        }

        if (isNotification) {
            addListnersImport(curNode, imports, true, LISTENER_SERVICE);
        }
        /**
         * Creates rpc interface file.
         */
        setServiceInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(SERVICE_FILE_NAME_SUFFIX)));
        generateServiceInterfaceFile(getServiceInterfaceJavaFileHandle(), curNode, imports, isAttributePresent());

        if (isNotification) {
            addListnersImport(curNode, imports, false, LISTENER_SERVICE);
            addListnersImport(curNode, imports, true, LISTENER_REG);
        }
        addAnnotationsImports(imports, true);
        /**
         * Create builder class file.
         */
        setManagerJavaFileHandle(getJavaFileHandle(getJavaClassName(MANAGER_FILE_NAME_SUFFIX)));
        generateManagerClassFile(getManagerJavaFileHandle(), imports, curNode, isAttributePresent());

        insertDataIntoJavaFile(getManagerJavaFileHandle(), getJavaClassDefClose());
        if (isNotification) {
            addListnersImport(curNode, imports, false, LISTENER_REG);
        }
        addAnnotationsImports(imports, false);

        if (isNotification) {
            generateEventJavaFile(GENERATE_EVENT_CLASS, curNode);
            generateEventListenerJavaFile(GENERATE_EVENT_LISTENER_INTERFACE, curNode);
            generateEventSubjectJavaFile(GENERATE_EVENT_SUBJECT_CLASS, curNode);
        }

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Adds rpc string information to applicable temp file.
     *
     * @param javaAttributeInfoOfInput  rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param rpcName                   name of the rpc function
     * @param pluginConfig              plugin configurations
     * @throws IOException IO operation fail
     */
    private void addRpcString(JavaAttributeInfo javaAttributeInfoOfInput,
            JavaAttributeInfo javaAttributeInfoOfOutput, YangPluginConfig pluginConfig,
            String rpcName)
            throws IOException {
        String rpcInput = EMPTY_STRING;
        String rpcOutput = VOID;
        String rpcInputJavaDoc = EMPTY_STRING;
        if (javaAttributeInfoOfInput != null) {
            rpcInput = getCapitalCase(javaAttributeInfoOfInput.getAttributeName());
        }
        if (javaAttributeInfoOfOutput != null) {
            rpcOutput = getCapitalCase(javaAttributeInfoOfOutput.getAttributeName());
        }
        if (!rpcInput.equals(EMPTY_STRING)) {
            rpcInputJavaDoc = RPC_INPUT_VAR_NAME;
        }
        appendToFile(getRpcInterfaceTempFileHandle(),
                generateJavaDocForRpc(rpcName, rpcInputJavaDoc, rpcOutput, pluginConfig)
                        + getRpcServiceMethod(rpcName, rpcInput, rpcOutput, pluginConfig) + NEW_LINE);
        appendToFile(getRpcImplTempFileHandle(),
                getRpcManagerMethod(rpcName, rpcInput, rpcOutput, pluginConfig) + NEW_LINE);
    }

    /**
     * Adds the JAVA rpc snippet information.
     *
     * @param javaAttributeInfoOfInput  rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param pluginConfig              plugin configurations
     * @param rpcName                   name of the rpc function
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo javaAttributeInfoOfInput,
            JavaAttributeInfo javaAttributeInfoOfOutput, YangPluginConfig pluginConfig,
            String rpcName)
            throws IOException {
        addRpcString(javaAttributeInfoOfInput, javaAttributeInfoOfOutput, pluginConfig, rpcName);
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventJavaFile(int fileType, YangNode curNode)
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
     * @param fileType generated file type
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventListenerJavaFile(int fileType, YangNode curNode)
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
     * @param fileType generated file type
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateEventSubjectJavaFile(int fileType, YangNode curNode)
            throws IOException {

        String curNodeInfo = getCapitalCase(((JavaFileInfoContainer) curNode)
                .getJavaFileInfo().getJavaName());
        /**
         * Creates event interface file.
         */
        setEventSubjectJavaFileHandle(getJavaFileHandle(curNode, curNodeInfo +
                EVENT_SUBJECT_NAME_SUFFIX));
        generateEventSubjectFile(getEventSubjectJavaFileHandle(), curNode);

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we
     *                        need to close all open file handles include temporary files
     *                        and java files.
     * @throws IOException when failed to delete the temporary files
     */
    @Override
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {
        boolean isError = isErrorOccurred;

        closeFile(getServiceInterfaceJavaFileHandle(), isError);
        closeFile(getManagerJavaFileHandle(), isError);

        if (getEventJavaFileHandle() != null) {
            closeFile(getEventJavaFileHandle(), isError);
        }
        if (getEventListenerJavaFileHandle() != null) {
            closeFile(getEventListenerJavaFileHandle(), isError);
        }
        if (getEventSubjectJavaFileHandle() != null) {
            closeFile(getEventSubjectJavaFileHandle(), isError);
        }

        closeFile(getRpcInterfaceTempFileHandle(), true);
        closeFile(getRpcImplTempFileHandle(), true);
        closeFile(getGetterInterfaceTempFileHandle(), true);
        closeFile(getSetterInterfaceTempFileHandle(), true);
        closeFile(getSetterImplTempFileHandle(), true);

        super.freeTemporaryResources(isErrorOccurred);

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

        String currentInfo = getCapitalCase(getCamelCase(((YangNotification) curNode).getName(),
                pluginConfig.getConflictResolver()));
        String notificationName = ((YangNotification) curNode).getName();

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
     * @param fileName file name
     * @return temporary file handle
     * @throws IOException when fails to create new file handle
     */
    private File getJavaFileHandle(YangNode curNode, String name)
            throws IOException {

        JavaFileInfo parentInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        return getFileObject(getDirPath(parentInfo), name, JAVA_FILE_EXTENSION,
                parentInfo);
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
