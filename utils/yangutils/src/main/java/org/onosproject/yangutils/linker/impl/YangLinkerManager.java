/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onosproject.yangutils.linker.impl;

import java.util.Set;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.linker.ResolvableType;
import org.onosproject.yangutils.linker.YangLinker;
import org.onosproject.yangutils.linker.YangReferenceResolver;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.plugin.manager.YangFileInfo;

import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;

/**
 * Representation of entity which provides linking service of YANG files.
 */
public class YangLinkerManager
        implements YangLinker {
    @Override
    public void resolveDependencies(Set<YangFileInfo> yangFileInfoSet) {

        // Carry out linking of sub module with module.
        linkSubModulesToParentModule(yangFileInfoSet);

        // Add references to import list.
        addRefToYangFilesImportList(yangFileInfoSet);

        // Add reference to include list.
        addRefToYangFilesIncludeList(yangFileInfoSet);

        // TODO check for circular import/include.

        // Carry out inter-file linking.
        processInterFileLinking(yangFileInfoSet);
    }

    /**
     * Resolves sub-module linking by linking sub module with parent module.
     *
     * @param yangFileInfoSet set of YANG files info
     * @throws LinkerException fails to link sub-module to parent module
     */
    public void linkSubModulesToParentModule(Set<YangFileInfo> yangFileInfoSet)
            throws LinkerException {
        for (YangFileInfo yangFileInfo : yangFileInfoSet) {
            YangNode yangNode = yangFileInfo.getRootNode();
            if (yangNode instanceof YangSubModule) {
                try {
                    ((YangSubModule) yangNode).linkWithModule(yangFileInfoSet);
                } catch (DataModelException e) {
                    String errorInfo = "YANG file error: " + yangFileInfo.getYangFileName() + " at line: "
                            + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE
                            + e.getMessage();
                    throw new LinkerException(errorInfo);
                }
            }
        }
    }

    /**
     * Adds imported node information to the import list.
     *
     * @param yangFileInfoSet set of YANG files info
     * @throws LinkerException fails to find imported module
     */
    public void addRefToYangFilesImportList(Set<YangFileInfo> yangFileInfoSet) throws LinkerException {
        for (YangFileInfo yangFileInfo : yangFileInfoSet) {
            YangNode yangNode = yangFileInfo.getRootNode();
            if (yangNode instanceof YangReferenceResolver) {
                ((YangReferenceResolver) yangNode).addReferencesToImportList(yangFileInfoSet);
            }
        }
    }

    /**
     * Adds included node information to the include list.
     *
     * @param yangFileInfoSet set of YANG files info
     * @throws LinkerException fails to find included sub-module
     */
    public void addRefToYangFilesIncludeList(Set<YangFileInfo> yangFileInfoSet) throws LinkerException {
        for (YangFileInfo yangFileInfo : yangFileInfoSet) {
            YangNode yangNode = yangFileInfo.getRootNode();
            if (yangNode instanceof YangReferenceResolver) {
                ((YangReferenceResolver) yangNode).addReferencesToIncludeList(yangFileInfoSet);
            }
        }
    }

    /**
     * Processes inter file linking for type and uses.
     *
     * @param yangFileInfoSet set of YANG files info
     * @throws LinkerException a violation in linker execution
     */
    public void processInterFileLinking(Set<YangFileInfo> yangFileInfoSet)
            throws LinkerException {
        for (YangFileInfo yangFileInfo : yangFileInfoSet) {
            try {
                ((YangReferenceResolver) yangFileInfo.getRootNode()).resolveInterFileLinking(ResolvableType.YANG_USES);
                ((YangReferenceResolver) yangFileInfo.getRootNode())
                        .resolveInterFileLinking(ResolvableType.YANG_DERIVED_DATA_TYPE);
            } catch (DataModelException e) {
                String errorInfo = "Error in file: " + yangFileInfo.getYangFileName() + " at line: "
                        + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE + e.getMessage();
                throw new LinkerException(errorInfo);
            }
        }
    }
}
