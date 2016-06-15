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

import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.addCurNodeAsAttributeInTargetTempFile;
import static org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModelUtils.updatePackageInfo;

/**
 * Represents uses information extended to support java code generation.
 */
public class YangJavaUses
        extends YangUses
        implements JavaCodeGeneratorInfo, JavaCodeGenerator {

    private static final long serialVersionUID = 806201618L;

    /**
     * Contains the information of the java file being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * File handle to maintain temporary java code fragments as per the code
     * snippet types.
     */
    private transient TempJavaCodeFragmentFiles tempFileHandle;

    /**
     * Creates YANG java uses object.
     */
    public YangJavaUses() {
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
            throw new TranslatorException("Missing java info in java datamodel node");
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

    /**
     * Returns the temporary file handle.
     *
     * @return temporary file handle
     */
    @Override
    public TempJavaCodeFragmentFiles getTempJavaCodeFragmentFiles() {
        return tempFileHandle;
    }

    /**
     * Sets temporary file handle.
     *
     * @param fileHandle temporary file handle
     */
    @Override
    public void setTempJavaCodeFragmentFiles(TempJavaCodeFragmentFiles fileHandle) {
        tempFileHandle = fileHandle;
    }

    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin)
            throws TranslatorException {
        try {
            updatePackageInfo(this, yangPlugin);

            if (!(getParentNodeInGenCode(this) instanceof JavaCodeGeneratorInfo)) {
                throw new TranslatorException("invalid container of uses");
            }
            JavaCodeGeneratorInfo javaCodeGeneratorInfo = (JavaCodeGeneratorInfo) getParentNodeInGenCode(this);

            if (javaCodeGeneratorInfo instanceof YangGrouping) {
                /*
                 * Do nothing, since it will taken care in the groupings uses.
                 */
                return;
            }

            for (List<YangLeaf> leavesList : getUsesResolvedLeavesList()) {
                // add the resolved leaves to the parent as an attribute
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getBeanTempFiles().addLeavesInfoToTempFiles(leavesList, yangPlugin);
            }

            for (List<YangLeafList> listOfLeafLists : getUsesResolvedListOfLeafList()) {
                // add the resolved leaf-list to the parent as an attribute
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getBeanTempFiles().addLeafListInfoToTempFiles(listOfLeafLists, yangPlugin);
            }

            for (YangNode usesResolvedNode : getUsesResolvedNodeList()) {
                // add the resolved nodes to the parent as an attribute
                addCurNodeAsAttributeInTargetTempFile(usesResolvedNode, yangPlugin,
                        getParentNodeInGenCode(this));
            }

        } catch (IOException e) {
            throw new TranslatorException(e.getCause());
        }
    }

    @Override
    public void generateCodeExit()
            throws TranslatorException {
        /*
         * Do nothing.
         */
    }
}
