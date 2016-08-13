/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import java.util.List;

import org.onosproject.tetopology.management.api.node.TerminationPointKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Represents the TE link underlay path data.
 */
public class UnderlayPath {
    private int protectionType;
    private UnderlayPrimaryPath primaryPath;
    private List<UnderlayBackupPath> backupPaths;
    private TerminationPointKey trailSrc;
    private TerminationPointKey trailDes;

    /**
     * Creates an instance of Underlay.
     */
    public UnderlayPath() {
    }

    /**
     * Sets the protection type.
     *
     * @param protectionType the protectionType to set
     */
    public void setProtectionType(int protectionType) {
        this.protectionType = protectionType;
    }

    /**
     * Sets the primary path.
     *
     * @param primaryPath the primaryPath to set
     */
    public void setPrimaryPath(UnderlayPrimaryPath primaryPath) {
        this.primaryPath = primaryPath;
    }

    /**
     * Sets the link of backup paths.
     *
     * @param backupPaths the backupPath to set
     */
    public void setBackupPath(List<UnderlayBackupPath> backupPaths) {
        this.backupPaths = backupPaths;
    }

    /**
     * Sets the trail source.
     *
     * @param trailSrc the trailSrc to set
     */
    public void setTrailSrc(TerminationPointKey trailSrc) {
        this.trailSrc = trailSrc;
    }

    /**
     * Sets the trail destination.
     *
     * @param trailDes the trailDes to set
     */
    public void setTrailDes(TerminationPointKey trailDes) {
        this.trailDes = trailDes;
    }

    /**
     * Returns the protection type.
     *
     * @return path protection type
     */
    public int protectionType() {
        return protectionType;
    }

    /**
     * Returns the primary path.
     *
     * @return underlay primary path
     */
    public UnderlayPrimaryPath primaryPath() {
        return primaryPath;
    }

    /**
     * Returns the backup paths.
     *
     * @return list of underlay backup paths
     */
    public List<UnderlayBackupPath> backupPaths() {
        return backupPaths;
    }

    /**
     * Returns the trail source.
     *
     * @return trail source
     */
    public TerminationPointKey trailSrc() {
        return trailSrc;
    }

    /**
     * Returns the trail destination.
     *
     * @return trail destination
     */
    public TerminationPointKey trailDes() {
        return trailDes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(protectionType, primaryPath,
                                            backupPaths, trailSrc, trailDes);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof UnderlayPath) {
            UnderlayPath that = (UnderlayPath) object;
            return Objects.equal(this.protectionType, that.protectionType) &&
                    Objects.equal(this.primaryPath, that.primaryPath) &&
                    Objects.equal(this.backupPaths, that.backupPaths) &&
                    Objects.equal(this.trailSrc, that.trailSrc) &&
                    Objects.equal(this.trailDes, that.trailDes);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("protectionType", protectionType)
                .add("primaryPath", primaryPath)
                .add("backupPaths", backupPaths)
                .add("trailSrc", trailSrc)
                .add("trailDes", trailDes)
                .toString();
    }

}
