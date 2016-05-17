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
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaNotification;

import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateManagerClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateServiceInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcManagerMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcServiceMethod;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addListnersImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_REG;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_INPUT_VAR_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.createPackage;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateJavaDocForRpc;
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

        setRpcInterfaceTempFileHandle(getTemporaryFileHandle(RPC_INTERFACE_FILE_NAME));
        setRpcImplTempFileHandle(getTemporaryFileHandle(RPC_IMPL_FILE_NAME));
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    @Override
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {
        List<String> imports = new ArrayList<>();
        imports = getJavaImportData().getImports();

        createPackage(curNode);

        boolean isNotification = false;
        YangNode tempNode = curNode.getChild();
        while (tempNode != null) {
            if (tempNode instanceof YangJavaNotification) {
                isNotification = true;
                break;
            }
            tempNode = tempNode.getNextSibling();
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
        /**
         * Create builder class file.
         */
        setManagerJavaFileHandle(getJavaFileHandle(getJavaClassName(MANAGER_FILE_NAME_SUFFIX)));
        generateManagerClassFile(getManagerJavaFileHandle(), imports, curNode, isAttributePresent());

        insertDataIntoJavaFile(getManagerJavaFileHandle(), getJavaClassDefClose());
        if (isNotification) {
            addListnersImport(curNode, imports, false, LISTENER_REG);
        }
        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Adds rpc string information to applicable temp file.
     *
     * @param javaAttributeInfoOfInput rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param rpcName name of the rpc function
     * @throws IOException IO operation fail
     */
    private void addRpcString(JavaAttributeInfo javaAttributeInfoOfInput,
            JavaAttributeInfo javaAttributeInfoOfOutput,
            String rpcName) throws IOException {
        String rpcInput = EMPTY_STRING;
        String rpcOutput = VOID;
        if (javaAttributeInfoOfInput != null) {
            rpcInput = getCapitalCase(javaAttributeInfoOfInput.getAttributeName());
        }
        if (javaAttributeInfoOfOutput != null) {
            rpcOutput = getCapitalCase(javaAttributeInfoOfOutput.getAttributeName());
        }
        appendToFile(getRpcInterfaceTempFileHandle(), generateJavaDocForRpc(rpcName, RPC_INPUT_VAR_NAME, rpcOutput)
                + getRpcServiceMethod(rpcName, rpcInput, rpcOutput) + NEW_LINE);
        appendToFile(getRpcImplTempFileHandle(), getRpcManagerMethod(rpcName, rpcInput, rpcOutput) + NEW_LINE);
    }

    /**
     * Adds the JAVA rpc snippet information.
     *
     * @param javaAttributeInfoOfInput rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param rpcName name of the rpc function
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo javaAttributeInfoOfInput,
            JavaAttributeInfo javaAttributeInfoOfOutput,
            String rpcName)
            throws IOException {
        addRpcString(javaAttributeInfoOfInput, javaAttributeInfoOfOutput, rpcName);
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
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {
        boolean isError = isErrorOccurred;

        closeFile(getServiceInterfaceJavaFileHandle(), isError);
        closeFile(getRpcInterfaceTempFileHandle(), true);
        closeFile(getRpcImplTempFileHandle(), true);
        closeFile(getGetterInterfaceTempFileHandle(), true);
        closeFile(getSetterInterfaceTempFileHandle(), true);
        closeFile(getSetterImplTempFileHandle(), true);

        super.freeTemporaryResources(isErrorOccurred);

    }
}
