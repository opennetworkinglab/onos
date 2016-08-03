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
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.javadatamodel.JavaFileInfo;
import org.onosproject.yangutils.datamodel.javadatamodel.YangPluginConfig;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModuleTranslator;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModuleTranslator;
import org.onosproject.yangutils.translator.tojava.utils.JavaExtendsListHolder;

import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.addListenersImport;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateServiceInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.addResolvedAugmentedDataNodeImports;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcManagerMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcServiceMethod;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_INPUT_VAR_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.closeFile;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateJavaDocForRpc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;

/**
 * Represents implementation of java service code fragments temporary implementations. Maintains the temp files required
 * specific for service and manager java snippet generation.
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
     * File name for generated class file for service suffix.
     */
    private static final String SERVICE_FILE_NAME_SUFFIX = "Service";

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
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated file information
     * @throws IOException when fails to create new file handle
     */
    TempJavaServiceFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {
        setJavaExtendsListHolder(new JavaExtendsListHolder());
        setJavaImportData(new JavaImportData());
        setJavaFileInfo(javaFileInfo);
        setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath()));
        addGeneratedTempFile(RPC_INTERFACE_MASK);
        addGeneratedTempFile(RPC_IMPL_MASK);

        setRpcInterfaceTempFileHandle(getTemporaryFileHandle(RPC_INTERFACE_FILE_NAME));
        setRpcImplTempFileHandle(getTemporaryFileHandle(RPC_IMPL_FILE_NAME));
    }

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
    private void setRpcImplTempFileHandle(File rpcImplTempFileHandle) {
        this.rpcImplTempFileHandle = rpcImplTempFileHandle;
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

        addResolvedAugmentedDataNodeImports(curNode);
        List<String> imports = ((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getServiceTempFiles()
                .getJavaImportData().getImports();
        createPackage(curNode);
        boolean isNotification = false;
        if (curNode instanceof YangJavaModuleTranslator) {
            if (!((YangJavaModuleTranslator) curNode).getNotificationNodes().isEmpty()) {
                isNotification = true;
            }
        } else if (curNode instanceof YangJavaSubModuleTranslator) {
            if (!((YangJavaSubModuleTranslator) curNode).getNotificationNodes().isEmpty()) {
                isNotification = true;
            }
        }

        if (isNotification) {
            addListenersImport(curNode, imports, true, LISTENER_SERVICE);
        }

        setServiceInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(SERVICE_FILE_NAME_SUFFIX)));
        generateServiceInterfaceFile(getServiceInterfaceJavaFileHandle(), curNode, imports);

        // Close all the file handles.
        freeTemporaryResources(false);
    }

    /**
     * Adds rpc string information to applicable temp file.
     *
     * @param javaAttributeInfoOfInput  RPCs input node attribute info
     * @param javaAttributeInfoOfOutput RPCs output node attribute info
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
     * @param javaAttributeInfoOfInput  RPCs input node attribute info
     * @param javaAttributeInfoOfOutput RPCs output node attribute info
     * @param pluginConfig              plugin configurations
     * @param rpcName                   name of the rpc function
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo javaAttributeInfoOfInput,
                                                        JavaAttributeInfo javaAttributeInfoOfOutput,
                                                        YangPluginConfig pluginConfig, String rpcName)
            throws IOException {
        addRpcString(javaAttributeInfoOfInput, javaAttributeInfoOfOutput, pluginConfig, rpcName);
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred flag to tell translator that error has occurred while file generation
     * @throws IOException when failed to delete the temporary files
     */
    @Override
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {

        closeFile(getServiceInterfaceJavaFileHandle(), isErrorOccurred);

        closeFile(getRpcInterfaceTempFileHandle(), true);
        closeFile(getRpcImplTempFileHandle(), true);
        closeFile(getGetterInterfaceTempFileHandle(), true);
        closeFile(getSetterInterfaceTempFileHandle(), true);
        closeFile(getSetterImplTempFileHandle(), true);

        super.freeTemporaryResources(isErrorOccurred);

    }
}
