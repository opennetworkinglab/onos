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

package org.onosproject.yangutils.translator.tojava.javamodel;

import java.io.IOException;

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModelUtils.updatePackageInfo;
import static org.onosproject.yangutils.utils.UtilConstants.ACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.COMPONENT;
import static org.onosproject.yangutils.utils.UtilConstants.DEACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.REFERENCE;
import static org.onosproject.yangutils.utils.UtilConstants.REFERENCE_CARDINALITY;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE;

/**
 * Represents rpc information extended to support java code generation.
 */
public class YangJavaRpc
        extends YangRpc
        implements JavaCodeGenerator, JavaCodeGeneratorInfo {

    private static final long serialVersionUID = 806201622L;

    /**
     * Contains the information of the java file being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * Temproary file for code generation.
     */
    private transient TempJavaCodeFragmentFiles tempJavaCodeFragmentFiles;

    /**
     * Creates an instance of YANG java rpc.
     */
    public YangJavaRpc() {
        super();
        setJavaFileInfo(new JavaFileInfo());
    }

    /**
     * Returns the generated java file information.
     *
     * @return generated java file information
     */
    @Override
    public JavaFileInfo getJavaFileInfo() {

        if (javaFileInfo == null) {
            throw new TranslatorException("missing java info in java datamodel node");
        }
        return javaFileInfo;
    }

    /**
     * Sets the java file info object.
     *
     * @param javaInfo java file info object
     */
    @Override
    public void setJavaFileInfo(JavaFileInfo javaInfo) {
        javaFileInfo = javaInfo;
    }

    @Override
    public TempJavaCodeFragmentFiles getTempJavaCodeFragmentFiles() {
        return tempJavaCodeFragmentFiles;
    }

    @Override
    public void setTempJavaCodeFragmentFiles(TempJavaCodeFragmentFiles fileHandle) {
        tempJavaCodeFragmentFiles = fileHandle;
    }

    /**
     * Prepares the information for java code generation corresponding to YANG
     * RPC info.
     *
     * @param yangPlugin YANG plugin config
     * @throws TranslatorException translator operations fails
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin)
            throws TranslatorException {

        // Add package information for rpc and create corresponding folder.
        try {
            updatePackageInfo(this, yangPlugin);
        } catch (IOException e) {
            throw new TranslatorException("Failed to prepare generate code entry for RPC node " + getName());
        }
    }

    /**
     * Creates a java file using the YANG RPC info.
     *
     * @throws TranslatorException translator operations fails
     */
    @Override
    public void generateCodeExit()
            throws TranslatorException {
        // Get the parent module/sub-module.
        YangNode parent = getParentNodeInGenCode(this);

        // Parent should be holder of rpc or notification.
        if (!(parent instanceof RpcNotificationContainer)) {
            throw new TranslatorException("parent node of rpc can only be module or sub-module");
        }

        /*
         * Create attribute info for input and output of rpc and add it to the
         * parent import list.
         */

        JavaAttributeInfo javaAttributeInfoOfInput = null;
        JavaAttributeInfo javaAttributeInfoOfOutput = null;

        // Get the child input and output node and obtain create java attribute
        // info.
        YangNode yangNode = getChild();
        while (yangNode != null) {
            if (yangNode instanceof YangInput) {
                javaAttributeInfoOfInput = getChildNodeAsAttributeInParentService(yangNode, this);

            } else if (yangNode instanceof YangOutput) {
                javaAttributeInfoOfOutput = getChildNodeAsAttributeInParentService(yangNode, this);
            } else {
                throw new TranslatorException("RPC should contain only input/output child nodes.");
            }
            yangNode = yangNode.getNextSibling();
        }

        if (!(parent instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("missing parent temp file handle");
        }

        /*
         * Add the rpc information to the parent's service temp file.
         */
        try {

            ((TempJavaCodeFragmentFilesContainer) parent).getTempJavaCodeFragmentFiles().getServiceTempFiles()
                    .addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfoOfInput, javaAttributeInfoOfOutput,
                            ((JavaFileInfoContainer) parent).getJavaFileInfo().getPluginConfig(), getName());

        } catch (IOException e) {
            throw new TranslatorException("Failed to generate code for RPC node " + getName());
        }
        // No file will be generated during RPC exit.
    }

    /**
     * Creates an attribute info object corresponding to a data model node and
     * return it.
     *
     * @param childNode   child data model node(input / output) for which the java code generation
     *                    is being handled
     * @param currentNode parent node (module / sub-module) in which the child node is an attribute
     * @return AttributeInfo attribute details required to add in temporary
     * files
     */
    public JavaAttributeInfo getChildNodeAsAttributeInParentService(
            YangNode childNode, YangNode currentNode) {

        YangNode parentNode = getParentNodeInGenCode(currentNode);

        String childNodeName = ((JavaFileInfoContainer) childNode).getJavaFileInfo().getJavaName();
        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(childNode,
                getCapitalCase(childNodeName));
        if (!(parentNode instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("Parent node does not have file info");
        }

        TempJavaFragmentFiles tempJavaFragmentFiles;
        tempJavaFragmentFiles = ((TempJavaCodeFragmentFilesContainer) parentNode)
                .getTempJavaCodeFragmentFiles()
                .getServiceTempFiles();

        if (tempJavaFragmentFiles == null) {
            throw new TranslatorException("Parent node does not have service file info");
        }
        boolean isQualified = addImportToService(qualifiedTypeInfo);
        return getAttributeInfoForTheData(qualifiedTypeInfo, childNodeName, null, isQualified, false);
    }

    /**
     * Adds to service class import list.
     *
     * @param importInfo import info
     * @return true or false
     */
    private boolean addImportToService(JavaQualifiedTypeInfo importInfo) {
        JavaFileInfo fileInfo = ((JavaFileInfoContainer) getParent()).getJavaFileInfo();

        if (importInfo.getClassInfo().contentEquals(SERVICE)
                || importInfo.getClassInfo().contentEquals(COMPONENT)
                || importInfo.getClassInfo().contentEquals(getCapitalCase(ACTIVATE))
                || importInfo.getClassInfo().contentEquals(getCapitalCase(DEACTIVATE))
                || importInfo.getClassInfo().contentEquals(REFERENCE_CARDINALITY)
                || importInfo.getClassInfo().contentEquals(REFERENCE)
                || importInfo.getClassInfo().contentEquals(getCapitalCase(fileInfo.getJavaName() + SERVICE))
                || importInfo.getClassInfo().contentEquals(getCapitalCase(fileInfo.getJavaName() + MANAGER))) {
            return true;
        }

        String className;
        className = getCapitalCase(fileInfo.getJavaName()) + "Service";

        return ((TempJavaCodeFragmentFilesContainer) getParent()).getTempJavaCodeFragmentFiles()
                .getServiceTempFiles().getJavaImportData().addImportInfo(importInfo,
                        className, fileInfo.getPackage());
    }

}
