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

import java.util.HashSet;
import java.util.Set;
import org.onosproject.yangutils.datamodel.ResolvableType;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.linker.YangLinker;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;

/**
 * Representation of entity which provides linking service of YANG files.
 */
public class YangLinkerManager
        implements YangLinker {

    /*
     * Set of all the YANG nodes, corresponding to the YANG files parsed by
     * parser.
     */
    Set<YangNode> yangNodeSet = new HashSet<>();

    /**
     * Returns set of YANG node.
     *
     * @return set of YANG node
     */
    public Set<YangNode> getYangNodeSet() {
        return yangNodeSet;
    }

    /**
     * Creates YANG nodes set.
     *
     * @param yangNodeSet YANG node information set
     */
    public void createYangNodeSet(Set<YangNode> yangNodeSet) {
        getYangNodeSet().addAll(yangNodeSet);
    }

    @Override
    public void resolveDependencies(Set<YangNode> yangNodeSet) {

        // Create YANG node set.
        createYangNodeSet(yangNodeSet);

        // Carry out linking of sub module with module.
        linkSubModulesToParentModule(yangNodeSet);

        // Add references to import list.
        addRefToYangFilesImportList(yangNodeSet);

        // Add reference to include list.
        addRefToYangFilesIncludeList(yangNodeSet);

        // TODO check for circular import/include.

        // Carry out inter-file linking.
        processInterFileLinking(yangNodeSet);
    }

    /**
     * Resolves sub-module linking by linking sub module with parent module.
     *
     * @param yangNodeSet set of YANG files info
     * @throws LinkerException fails to link sub-module to parent module
     */
    public void linkSubModulesToParentModule(Set<YangNode> yangNodeSet)
            throws LinkerException {
        for (YangNode yangNode : yangNodeSet) {
            if (yangNode instanceof YangSubModule) {
                try {
                    ((YangSubModule) yangNode).linkWithModule(getYangNodeSet());
                } catch (DataModelException e) {
                    String errorInfo = "YANG file error: " + yangNode.getName() + " at line: "
                            + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE
                            + e.getMessage();
                    throw new LinkerException(errorInfo);
                    // TODO add file path in exception message in util manager.
                }
            }
        }
    }

    /**
     * Adds imported node information to the import list.
     *
     * @param yangNodeSet set of YANG files info
     * @throws LinkerException fails to find imported module
     */
    public void addRefToYangFilesImportList(Set<YangNode> yangNodeSet) throws LinkerException {
        for (YangNode yangNode : yangNodeSet) {
            if (yangNode instanceof YangReferenceResolver) {
                try {
                    ((YangReferenceResolver) yangNode).addReferencesToImportList(getYangNodeSet());
                } catch (DataModelException e) {
                    String errorInfo = "Error in file: " + yangNode.getName() + " at line: "
                            + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE
                            + e.getMessage();
                    throw new LinkerException(errorInfo);
                    // TODO add file path in exception message in util manager.
                }
            }
        }
    }

    /**
     * Adds included node information to the include list.
     *
     * @param yangNodeSet set of YANG files info
     * @throws LinkerException fails to find included sub-module
     */
    public void addRefToYangFilesIncludeList(Set<YangNode> yangNodeSet) throws LinkerException {
        for (YangNode yangNode : yangNodeSet) {
            if (yangNode instanceof YangReferenceResolver) {
                try {
                    ((YangReferenceResolver) yangNode).addReferencesToIncludeList(getYangNodeSet());
                } catch (DataModelException e) {
                    String errorInfo = "Error in file: " + yangNode.getName() + " at line: "
                            + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE
                            + e.getMessage();
                    throw new LinkerException(errorInfo);
                    // TODO add file path in exception message in util manager.
                }
            }
        }
    }

    /**
     * Processes inter file linking for type and uses.
     *
     * @param yangNodeSet set of YANG files info
     * @throws LinkerException a violation in linker execution
     */
    public void processInterFileLinking(Set<YangNode> yangNodeSet)
            throws LinkerException {
        for (YangNode yangNode : yangNodeSet) {
            try {
                ((YangReferenceResolver) yangNode).resolveInterFileLinking(ResolvableType.YANG_USES);
                ((YangReferenceResolver) yangNode)
                        .resolveInterFileLinking(ResolvableType.YANG_DERIVED_DATA_TYPE);
            } catch (DataModelException e) {
                String errorInfo = "Error in file: " + yangNode.getName() + " at line: "
                        + e.getLineNumber() + " at position: " + e.getCharPositionInLine() + NEW_LINE + e.getMessage();
                throw new LinkerException(errorInfo);
                // TODO add file path in exception message in util manager.
            }
        }
    }
}
