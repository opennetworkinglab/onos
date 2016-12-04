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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents the TE link underlay path and tunnel data.
 */
public class UnderlayPath {
    private final UnderlayPrimaryPath primaryPath;
    private final List<UnderlayBackupPath> backupPaths;
    private final TunnelProtectionType tunnelProtectionType;
    private final long srcTtpId;
    private final long dstTtpId;
    private final TeTunnelId teTunnelId;

    /**
     * Creates a underlay path.
     *
     * @param primaryPath          the underlay primary path
     * @param backupPaths          the underlay backup paths
     * @param tunnelProtectionType the supporting tunnel protection type to set
     * @param srcTtpId             the source tunnel termination point id
     * @param dstTtpId             the destination tunnel termination point id
     * @param teTunnelId           the supporting TE tunnel id
     */
    public UnderlayPath(UnderlayPrimaryPath primaryPath,
                        List<UnderlayBackupPath> backupPaths,
                        TunnelProtectionType tunnelProtectionType,
                        long srcTtpId,
                        long dstTtpId,
                        TeTunnelId teTunnelId) {
        this.primaryPath = primaryPath;
        this.backupPaths = backupPaths != null ?
                Lists.newArrayList(backupPaths) : null;
        this.tunnelProtectionType = tunnelProtectionType;
        this.srcTtpId = srcTtpId;
        this.dstTtpId = dstTtpId;
        this.teTunnelId = teTunnelId;
    }

    /**
     * Creates a underlay path based on a TE link.
     *
     * @param link the TE link
     */
    public UnderlayPath(TeLink link) {
        this.primaryPath = link.primaryPath();
        this.backupPaths = link.backupPaths() != null ?
                Lists.newArrayList(link.backupPaths()) : null;
        this.tunnelProtectionType = link.tunnelProtectionType();
        this.srcTtpId = link.sourceTtpId();
        this.dstTtpId = link.destinationTtpId();
        this.teTunnelId = link.teTunnelId();
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
        if (backupPaths == null) {
            return null;
        }
        return ImmutableList.copyOf(backupPaths);
    }

    /**
     * Returns the supporting tunnel protection type.
     *
     * @return the supporting tunnel protection type
     */
    public TunnelProtectionType tunnelProtectionType() {
        return tunnelProtectionType;
    }

    /**
     * Returns the supporting TE tunnel's source tunnel termination point
     * identifier.
     *
     * @return the supporting source TTP id
     */
    public long srcTtpId() {
        return srcTtpId;
    }

    /**
     * Returns the supporting TE tunnel's destination tunnel termination
     * point identifier.
     *
     * @return the destination TTP id
     */
    public long dstTtpId() {
        return dstTtpId;
    }

    /**
     * Returns the supporting TE tunnel identifier.
     *
     * @return the supporting tunnel id
     */
    public TeTunnelId teTunnelId() {
        return teTunnelId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(primaryPath, backupPaths, tunnelProtectionType,
                                srcTtpId, dstTtpId, teTunnelId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof UnderlayPath) {
            UnderlayPath that = (UnderlayPath) object;
            return Objects.equal(primaryPath, that.primaryPath) &&
                    Objects.equal(backupPaths, that.backupPaths) &&
                    Objects.equal(tunnelProtectionType, that.tunnelProtectionType) &&
                    Objects.equal(srcTtpId, that.srcTtpId) &&
                    Objects.equal(dstTtpId, that.dstTtpId) &&
                    Objects.equal(teTunnelId, that.teTunnelId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("primaryPath", primaryPath)
                .add("backupPaths", backupPaths)
                .add("tunnelProtectionType", tunnelProtectionType)
                .add("srcTtpId", srcTtpId)
                .add("dstTtpId", dstTtpId)
                .add("teTunnelId", teTunnelId)
                .toString();
    }

}
