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
import java.util.List;

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.YangNodeType.LIST_NODE;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.resolveGroupingsQuailifiedInfo;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaDataType;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportClass;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportPackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.utils.YangJavaModelUtils.updatePackageInfo;

/**
 * Represents rpc information extended to support java code generation.
 */
public class YangJavaRpc
        extends YangRpc
        implements JavaCodeGenerator, JavaCodeGeneratorInfo {

    /**
     * Contains the information of the java file being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * Temproary file for code generation.
     */
    private TempJavaCodeFragmentFiles tempJavaCodeFragmentFiles;

    private boolean isInputLeafHolder;
    private boolean isOutputLeafHolder;
    private boolean isInputSingleChildHolder;
    private boolean isOutputSingleChildHolder;

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
    public void generateCodeEntry(YangPluginConfig yangPlugin) throws TranslatorException {

        if (!(this instanceof JavaCodeGeneratorInfo)) {
            // TODO:throw exception
        }

        // Add package information for rpc and create corresponding folder.
        try {
            updatePackageInfo(this, yangPlugin);
            if (this.getChild() != null) {
                processNodeEntry(this.getChild(), yangPlugin);
                if (this.getChild().getNextSibling() != null) {
                    processNodeEntry(this.getChild().getNextSibling(), yangPlugin);
                }
            }
        } catch (IOException e) {
            throw new TranslatorException("Failed to prepare generate code entry for RPC node " + this.getName());
        }
    }

    /**
     * Creates a java file using the YANG RPC info.
     *
     * @throws TranslatorException translator operations fails
     */
    @Override
    public void generateCodeExit() throws TranslatorException {
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
        YangNode yangNode = this.getChild();
        while (yangNode != null) {
            if (yangNode instanceof YangInput) {
                javaAttributeInfoOfInput = processNodeExit(yangNode, getJavaFileInfo().getPluginConfig());

            } else if (yangNode instanceof YangOutput) {
                javaAttributeInfoOfOutput = processNodeExit(yangNode, getJavaFileInfo().getPluginConfig());
            } else {
                // TODO throw exception
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
                            ((JavaFileInfoContainer) parent).getJavaFileInfo().getPluginConfig(),
                            ((YangNode) this).getName(), isInputLeafHolder(), isOutputLeafHolder(),
                            isInputSingleChildHolder(), isOutputSingleChildHolder());
        } catch (IOException e) {
            throw new TranslatorException("Failed to generate code for RPC node " + this.getName());
        }
        // No file will be generated during RPC exit.
    }

    /**
     * Creates an attribute info object corresponding to a data model node and
     * return it.
     *
     * @param childNode child data model node(input / output) for which the java code generation
     * is being handled
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
        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(currentNode,
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
     * Process input/output nodes.
     *
     * @param node YANG node
     * @param yangPluginConfig plugin configurations
     */
    private void processNodeEntry(YangNode node, YangPluginConfig yangPluginConfig) {
        YangLeavesHolder holder = (YangLeavesHolder) node;
        if (node.getChild() == null) {
            if (holder.getListOfLeaf() != null && holder.getListOfLeafList().isEmpty()
                    && holder.getListOfLeaf().size() == 1) {
                setCodeGenFlagForNode(node, false);
            } else if (holder.getListOfLeaf().isEmpty() && holder.getListOfLeafList() != null
                    && holder.getListOfLeafList().size() == 1) {
                setCodeGenFlagForNode(node, false);
            } else {
                setCodeGenFlagForNode(node, true);
            }
        } else if (node.getChild() != null && holder.getListOfLeaf().isEmpty()
                && holder.getListOfLeafList().isEmpty()) {
            if (getNumberOfChildNodes(node) == 1) {
                setCodeGenFlagForNode(node, false);
            } else {
                setCodeGenFlagForNode(node, true);
            }
        } else {
            setCodeGenFlagForNode(node, true);
        }
    }

    /**
     * Process input/output nodes.
     *
     * @param node YANG node
     * @param yangPluginConfig plugin configurations
     * @return java attribute info
     */
    private JavaAttributeInfo processNodeExit(YangNode node, YangPluginConfig yangPluginConfig) {
        YangLeavesHolder holder = (YangLeavesHolder) node;
        if (node.getChild() == null) {
            if (holder.getListOfLeaf() != null && holder.getListOfLeafList().isEmpty()
                    && holder.getListOfLeaf().size() == 1) {
                return processNodeWhenOnlyOneLeafIsPresent(node, yangPluginConfig);

            } else if (holder.getListOfLeaf().isEmpty() && holder.getListOfLeafList() != null
                    && holder.getListOfLeafList().size() == 1) {
                return processNodeWhenOnlyOneLeafListIsPresent(node, yangPluginConfig);
            } else {
                return processNodeWhenMultipleContaintsArePresent(node);
            }
        } else if (node.getChild() != null && holder.getListOfLeaf().isEmpty()
                && holder.getListOfLeafList().isEmpty()) {
            if (getNumberOfChildNodes(node) == 1) {
                return processNodeWhenOnlyOneChildNodeIsPresent(node, yangPluginConfig);
            } else {
                return processNodeWhenMultipleContaintsArePresent(node);
            }
        } else {
            return processNodeWhenMultipleContaintsArePresent(node);
        }
    }

    /**
     * Process input/output node when one leaf is present.
     *
     * @param node input/output node
     * @param yangPluginConfig plugin configurations
     * @return java attribute for node
     */
    private JavaAttributeInfo processNodeWhenOnlyOneLeafIsPresent(YangNode node,
            YangPluginConfig yangPluginConfig) {

        YangLeavesHolder holder = (YangLeavesHolder) node;
        List<YangLeaf> listOfLeaves = holder.getListOfLeaf();

        for (YangLeaf leaf : listOfLeaves) {
            if (!(leaf instanceof JavaLeafInfoContainer)) {
                throw new TranslatorException("Leaf does not have java information");
            }
            JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leaf;
            javaLeaf.setConflictResolveConfig(yangPluginConfig.getConflictResolver());
            javaLeaf.updateJavaQualifiedInfo();
            JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(
                    javaLeaf.getJavaQualifiedInfo(),
                    javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                    javaLeaf.getDataType(),
                    addTypeImport(javaLeaf.getDataType(), false, yangPluginConfig), false);
            setLeafHolderFlag(node, true);
            return javaAttributeInfo;
        }
        return null;
    }

    /**
     * Process input/output node when one leaf list is present.
     *
     * @param node input/output node
     * @param yangPluginConfig plugin configurations
     * @return java attribute for node
     */
    private JavaAttributeInfo processNodeWhenOnlyOneLeafListIsPresent(YangNode node,
            YangPluginConfig yangPluginConfig) {

        YangLeavesHolder holder = (YangLeavesHolder) node;
        List<YangLeafList> listOfLeafList = holder.getListOfLeafList();

        for (YangLeafList leafList : listOfLeafList) {
            if (!(leafList instanceof JavaLeafInfoContainer)) {
                throw new TranslatorException("Leaf-list does not have java information");
            }
            JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leafList;
            javaLeaf.setConflictResolveConfig(yangPluginConfig.getConflictResolver());
            javaLeaf.updateJavaQualifiedInfo();
            ((TempJavaCodeFragmentFilesContainer) this.getParent()).getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles().getJavaImportData().setIfListImported(true);
            JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(
                    javaLeaf.getJavaQualifiedInfo(),
                    javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                    javaLeaf.getDataType(),
                    addTypeImport(javaLeaf.getDataType(), true, yangPluginConfig),
                    true);
            setLeafHolderFlag(node, true);
            return javaAttributeInfo;
        }
        return null;
    }

    /**
     * Process input/output node when one child node is present.
     *
     * @param node input/output node
     * @param yangPluginConfig plugin configurations
     * @return java attribute for node
     */
    private JavaAttributeInfo processNodeWhenOnlyOneChildNodeIsPresent(YangNode node,
            YangPluginConfig yangPluginConfig) {
        JavaFileInfo rpcInfo = getJavaFileInfo();
        String clsInfo = "";
        JavaQualifiedTypeInfo childInfo = new JavaQualifiedTypeInfo();
        if (node.getChild() instanceof YangJavaUses) {
            childInfo = resolveGroupingsQuailifiedInfo(((YangJavaUses) node.getChild()).getRefGroup(),
                    yangPluginConfig);
            clsInfo = getCapitalCase(getCamelCase(((YangJavaUses) node.getChild()).getRefGroup().getName(),
                    yangPluginConfig.getConflictResolver()));
        } else {
            String pkg = (rpcInfo.getPackage() + "." + rpcInfo.getJavaName() + "."
                    + getCamelCase(node.getName(), yangPluginConfig.getConflictResolver())).toLowerCase();
            clsInfo = getCapitalCase(
                    getCamelCase(node.getChild().getName(), yangPluginConfig.getConflictResolver()));
            childInfo.setPkgInfo(pkg);
            childInfo.setClassInfo(clsInfo);
        }
        boolean isList = false;
        if (node.getChild().getNodeType().equals(LIST_NODE)) {
            isList = true;
        }
        boolean isQualified = addImportToService(childInfo);

        JavaAttributeInfo javaAttributeInfo =
                getAttributeInfoForTheData(childInfo, clsInfo, null, isQualified, isList);

        setLeafHolderFlag(node, false);
        setSingleChildHolderFlag(node, true);
        return javaAttributeInfo;
    }

    /**
     * Process input/output node when multiple leaf and child nodes are present.
     *
     * @param node input/output node
     * @return java attribute for node
     */
    private JavaAttributeInfo processNodeWhenMultipleContaintsArePresent(YangNode node) {

        setLeafHolderFlag(node, false);
        setSingleChildHolderFlag(node, false);
        return getChildNodeAsAttributeInParentService(node, this);
    }

    /**
     * Adds type import to the RPC import list.
     *
     * @param type YANG type
     * @param isList is list attribute
     * @param pluginConfig plugin configurations
     * @return type import to the RPC import list
     */
    private boolean addTypeImport(YangType<?> type, boolean isList, YangPluginConfig pluginConfig) {

        String classInfo = getJavaImportClass(type, isList, pluginConfig.getConflictResolver());
        if (classInfo == null) {
            classInfo = getJavaDataType(type);
            return false;
        } else {
            classInfo = getJavaImportClass(type, isList, pluginConfig.getConflictResolver());
            String pkgInfo = getJavaImportPackage(type, isList, pluginConfig.getConflictResolver());
            JavaQualifiedTypeInfo importInfo = new JavaQualifiedTypeInfo();
            importInfo.setPkgInfo(pkgInfo);
            importInfo.setClassInfo(classInfo);
            if (!((JavaFileInfoContainer) this.getParent()).getJavaFileInfo().getJavaName().equals(classInfo)) {
                return addImportToService(importInfo);
            } else {
                return true;
            }
        }
    }

    /**
     * Adds to service class import list.
     *
     * @param importInfo import info
     * @return true or false
     */
    private boolean addImportToService(JavaQualifiedTypeInfo importInfo) {
        if (((TempJavaCodeFragmentFilesContainer) this.getParent()).getTempJavaCodeFragmentFiles()
                .getServiceTempFiles().getJavaImportData().addImportInfo(importInfo)) {
            return !((TempJavaCodeFragmentFilesContainer) this.getParent()).getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles().getJavaImportData().getImportSet().contains(importInfo);
        } else {
            return true;
        }
    }

    /**
     * Sets leaf holder flag for input/output.
     *
     * @param node input/output node
     * @param flag true or false
     */
    private void setLeafHolderFlag(YangNode node, boolean flag) {
        if (node instanceof YangJavaInput) {
            setInputLeafHolder(flag);
        } else {
            setOutputLeafHolder(flag);
        }
    }

    /**
     * Sets sing child holder flag for input/output.
     *
     * @param node input/output node
     * @param flag true or false
     */
    private void setSingleChildHolderFlag(YangNode node, boolean flag) {
        if (node instanceof YangJavaInput) {
            setInputSingleChildHolder(flag);
        } else {
            setOutputSingleChildHolder(flag);
        }
    }

    /**
     * Sets code generator flag for input and output.
     *
     * @param node YANG node
     * @param flag cod generator flag
     */
    private void setCodeGenFlagForNode(YangNode node, boolean flag) {
        if (node instanceof YangJavaInput) {
            ((YangJavaInput) node).setCodeGenFlag(flag);
        } else {
            ((YangJavaOutput) node).setCodeGenFlag(flag);
        }

    }

    /**
     * Counts the number of child nodes of a YANG node.
     *
     * @param node YANG node
     * @return count of children
     */
    private int getNumberOfChildNodes(YangNode node) {
        YangNode tempNode = node.getChild();
        int count = 0;
        if (tempNode != null) {
            count = 1;
        }
        while (tempNode != null) {

            tempNode = tempNode.getNextSibling();
            if (tempNode != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if input is a leaf holder.
     *
     * @return true if input is a leaf holder
     */
    public boolean isInputLeafHolder() {
        return isInputLeafHolder;
    }

    /**
     * Sets true if input is a leaf holder.
     *
     * @param isInputLeafHolder true if input is a leaf holder
     */
    public void setInputLeafHolder(boolean isInputLeafHolder) {
        this.isInputLeafHolder = isInputLeafHolder;
    }

    /**
     * Returns true if output is a leaf holder.
     *
     * @return true if output is a leaf holder
     */
    public boolean isOutputLeafHolder() {
        return isOutputLeafHolder;
    }

    /**
     * Sets true if output is a leaf holder.
     *
     * @param isOutputLeafHolder true if output is a leaf holder
     */
    public void setOutputLeafHolder(boolean isOutputLeafHolder) {
        this.isOutputLeafHolder = isOutputLeafHolder;
    }

    /**
     * Returns true if input is single child holder.
     *
     * @return true if input is single child holder
     */
    public boolean isInputSingleChildHolder() {
        return isInputSingleChildHolder;
    }

    /**
     * Sets true if input is single child holder.
     *
     * @param isInputSingleChildHolder true if input is single child holder
     */
    public void setInputSingleChildHolder(boolean isInputSingleChildHolder) {
        this.isInputSingleChildHolder = isInputSingleChildHolder;
    }

    /**
     * Returns true if output is single child holder.
     *
     * @return true if output is single child holder
     */
    public boolean isOutputSingleChildHolder() {
        return isOutputSingleChildHolder;
    }

    /**
     * Sets true if output is single child holder.
     *
     * @param isOutputSingleChildHolder true if output is single child holder
     */
    public void setOutputSingleChildHolder(boolean isOutputSingleChildHolder) {
        this.isOutputSingleChildHolder = isOutputSingleChildHolder;
    }

}
