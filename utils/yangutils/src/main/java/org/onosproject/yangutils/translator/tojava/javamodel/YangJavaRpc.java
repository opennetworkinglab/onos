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
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_RPC_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.getCurNodeAsAttributeInParent;
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

    /**
     * Creates an instance of YANG java rpc.
     */
    public YangJavaRpc() {
        super();
        setJavaFileInfo(new JavaFileInfo());
        getJavaFileInfo().setGeneratedFileTypes(GENERATE_RPC_INTERFACE);
        try {
            setTempJavaCodeFragmentFiles(new TempJavaCodeFragmentFiles(getJavaFileInfo()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary RPC file handle");
        }
    }

    /**
     * Prepares the information for java code generation corresponding to YANG
     * RPC info.
     *
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operations fails
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin)
            throws IOException {

        if (!(this instanceof JavaCodeGeneratorInfo)) {
            // TODO:throw exception
        }

        // Add package information for rpc and create corresponding folder.
        updatePackageInfo((JavaCodeGeneratorInfo) this, yangPlugin);

    }

    /**
     * Creates a java file using the YANG RPC info.
     *
     * @throws IOException IO operations fails
     */
    @Override
    public void generateCodeExit()
            throws IOException {
        // Get the parent module/sub-module.
        YangNode parent = getParentNodeInGenCode((YangNode) this);

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

        // Get the child input and output node and obtain create java attribute info.
        YangNode yangNode = this.getChild();
        while (yangNode != null) {
            if (yangNode instanceof YangInput) {
                javaAttributeInfoOfInput = getCurNodeAsAttributeInParent(yangNode, this, false);
            } else if (yangNode instanceof YangOutput) {
                javaAttributeInfoOfOutput = getCurNodeAsAttributeInParent(yangNode, this, false);
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
        ((TempJavaCodeFragmentFilesContainer) parent)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles()
                .addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfoOfInput, javaAttributeInfoOfOutput,
                        ((YangNode) this).getName());
        // No file will be generated during RPC exit.
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
}

