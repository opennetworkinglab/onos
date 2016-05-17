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

package org.onosproject.yangutils.plugin.manager;

import java.util.Objects;
import org.onosproject.yangutils.linker.impl.ResolvableStatus;
import org.onosproject.yangutils.datamodel.YangNode;

/**
 * Represents YANG file information.
 */
public class YangFileInfo {

    /**
     * YANG file name.
     */
    private String yangFileName;

    /**
     * YANG file revision.
     */
    private String revision;

    /**
     * Data model node after parsing YANG file.
     */
    private YangNode rootNode;

    /**
     * Resolution status of YANG file.
     */
    private ResolvableStatus resolvableStatus;

    /**
     * Returns data model node for YANG file.
     *
     * @return data model node for YANG file
     */
    public YangNode getRootNode() {
        return rootNode;
    }

    /**
     * Sets data model node for YANG file.
     *
     * @param rootNode of the Yang file
     */
    public void setRootNode(YangNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Returns YANG file name.
     *
     * @return yangFileName YANG file name
     */
    public String getYangFileName() {
        return yangFileName;
    }

    /**
     * Sets YANG file name.
     *
     * @param yangFileName YANG file name
     */
    public void setYangFileName(String yangFileName) {
        this.yangFileName = yangFileName;
    }

    /**
     * Returns the revision of YANG file.
     *
     * @return revision of YANG file
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Sets the revision of YANG file.
     *
     * @param revision revision of YANG file
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * Returns the resolution status of YANG file.
     *
     * @return resolution status of YANG file
     */
    public ResolvableStatus getResolvableStatus() {
        return resolvableStatus;
    }

    /**
     * Sets the resolution status of YANG file.
     *
     * @param resolvableStatus resolution status of YANG file
     */
    public void setResolvableStatus(ResolvableStatus resolvableStatus) {
        this.resolvableStatus = resolvableStatus;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof YangFileInfo) {
            final YangFileInfo other = (YangFileInfo) obj;
            return Objects.equals(this.yangFileName, other.yangFileName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.yangFileName);
    }
}
