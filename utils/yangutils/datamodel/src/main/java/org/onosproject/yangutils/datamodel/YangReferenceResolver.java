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

package org.onosproject.yangutils.datamodel;

import java.util.List;
import java.util.Set;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

/**
 * Abstraction of YANG dependency resolution information. Abstracted to obtain the
 * resolution information.
 */
public interface YangReferenceResolver {

    /**
     * Returns unresolved resolution list.
     *
     * @param type resolvable type
     * @return list of resolution information objects
     */
    List<YangResolutionInfo> getUnresolvedResolutionList(ResolvableType type);

    /**
     * Adds to the resolution list.
     *
     * @param resolutionInfo resolution information
     * @param type           resolvable type
     */
    void addToResolutionList(YangResolutionInfo resolutionInfo, ResolvableType type);

    /**
     * Creates resolution list.
     *
     * @param resolutionList resolution list
     * @param type           resolvable type
     */
    void setResolutionList(List<YangResolutionInfo> resolutionList, ResolvableType type);

    /**
     * Returns unresolved imported list.
     *
     * @return unresolved imported list
     */
    List<YangImport> getImportList();

    /**
     * Adds to the import list.
     *
     * @param yangImport import to be added
     */
    void addToImportList(YangImport yangImport);

    /**
     * Create import list.
     *
     * @param importList import list
     */
    void setImportList(List<YangImport> importList);

    /**
     * Returns unresolved include list.
     *
     * @return unresolved include list
     */
    List<YangInclude> getIncludeList();

    /**
     * Adds to the include list.
     *
     * @param yangInclude include to be added
     */
    void addToIncludeList(YangInclude yangInclude);

    /**
     * Creates include list.
     *
     * @param includeList include list
     */
    void setIncludeList(List<YangInclude> includeList);

    /**
     * Returns prefix of resolution root node.
     *
     * @return prefix resolution root node prefix
     */
    String getPrefix();

    /**
     * Sets prefix of resolution list root node.
     *
     * @param prefix resolution root node prefix
     */
    void setPrefix(String prefix);

    /**
     * Resolves self file linking.
     *
     * @param type resolvable type
     * @throws DataModelException a violation in data model rule
     */
    void resolveSelfFileLinking(ResolvableType type)
            throws DataModelException;

    /**
     * Resolves inter file linking.
     *
     * @param type resolvable type
     * @throws DataModelException a violation in data model rule
     */
    void resolveInterFileLinking(ResolvableType type)
            throws DataModelException;

    /**
     * Adds references to include.
     *
     * @param yangNodeSet YANG node info set
     * @throws DataModelException a violation of data model rules
     */
    void addReferencesToIncludeList(Set<YangNode> yangNodeSet)
            throws DataModelException;

    /**
     * Adds references to import.
     *
     * @param yangNodeSet YANG node info set
     * @throws DataModelException a violation of data model rules
     */
    void addReferencesToImportList(Set<YangNode> yangNodeSet)
            throws DataModelException;
}
