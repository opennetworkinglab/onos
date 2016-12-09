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
import org.onosproject.tetopology.management.api.EncodingType;
import org.onosproject.tetopology.management.api.SwitchingType;
import org.onosproject.tetopology.management.api.TeConstants;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;

import java.util.BitSet;
import java.util.List;

/**
 * The default implementation of TE link.
 */
public class DefaultTeLink implements TeLink {
    private final TeLinkTpKey teLinkKey;
    private final TeLinkTpKey peerTeLinkKey;
    private final TeTopologyKey underlayTopologyId;
    private final TeLinkTpGlobalKey supportTeLinkId;
    private final TeLinkTpGlobalKey sourceTeLinkId;
    private final CommonLinkData teData;

    /**
     * Creates an instance of a TE link.
     *
     * @param teLinkKey          the TE link key
     * @param peerTeLinkKey      the bi-directional peer link key
     * @param underlayTopologyId the link underlay TE topology id
     * @param supportTeLinkId    the supporting TE link id
     * @param sourceTeLinkId     the source TE link id
     * @param teData             the link common te data
     */
    public DefaultTeLink(TeLinkTpKey teLinkKey,
                         TeLinkTpKey peerTeLinkKey,
                         TeTopologyKey underlayTopologyId,
                         TeLinkTpGlobalKey supportTeLinkId,
                         TeLinkTpGlobalKey sourceTeLinkId,
                         CommonLinkData teData) {
        this.teLinkKey = teLinkKey;
        this.peerTeLinkKey = peerTeLinkKey;
        this.underlayTopologyId = underlayTopologyId;
        this.supportTeLinkId = supportTeLinkId;
        this.sourceTeLinkId = sourceTeLinkId;
        this.teData = teData;
    }

    @Override
    public TeLinkTpKey teLinkKey() {
        return teLinkKey;
    }

    @Override
    public TeLinkTpKey peerTeLinkKey() {
        return peerTeLinkKey;
    }

    @Override
    public BitSet flags() {
        if (teData == null) {
            return null;
        }
        return teData.flags();
    }

    @Override
    public SwitchingType switchingLayer() {
        if (teData == null) {
            return null;
        }
        return teData.switchingLayer();
    }

    @Override
    public EncodingType encodingLayer() {
        if (teData == null) {
            return null;
        }
        return teData.encodingLayer();
    }

    @Override
    public ExternalLink externalLink() {
        if (teData == null) {
            return null;
        }
        return teData.externalLink();
    }

    @Override
    public TeTopologyKey underlayTeTopologyId() {
        return underlayTopologyId;
    }

    @Override
    public UnderlayPrimaryPath primaryPath() {
        if (teData == null || teData.underlayPath() == null) {
            return null;
        }
        return teData.underlayPath().primaryPath();
    }

    @Override
    public List<UnderlayBackupPath> backupPaths() {
        if (teData == null || teData.underlayPath() == null) {
            return null;
        }
        return teData.underlayPath().backupPaths();
    }

    @Override
    public TunnelProtectionType tunnelProtectionType() {
        if (teData == null || teData.underlayPath() == null) {
            return null;
        }
        return teData.underlayPath().tunnelProtectionType();
    }

    @Override
    public long sourceTtpId() {
        if (teData == null || teData.underlayPath() == null) {
            return TeConstants.NIL_LONG_VALUE;
        }
        return teData.underlayPath().srcTtpId();
    }

    @Override
    public long destinationTtpId() {
        if (teData == null || teData.underlayPath() == null) {
            return TeConstants.NIL_LONG_VALUE;
        }
        return teData.underlayPath().dstTtpId();
    }

    @Override
    public TeTunnelId teTunnelId() {
        if (teData == null || teData.underlayPath() == null) {
            return null;
        }
        return teData.underlayPath().teTunnelId();
    }

    @Override
    public TeLinkTpGlobalKey supportingTeLinkId() {
        return supportTeLinkId;
    }

    @Override
    public TeLinkTpGlobalKey sourceTeLinkId() {
        return sourceTeLinkId;
    }

    @Override
    public Long cost() {
        if (teData == null || teData.teAttributes() == null) {
            return TeConstants.NIL_LONG_VALUE;
        }
        return teData.teAttributes().cost();
    }

    @Override
    public Long delay() {
        if (teData == null || teData.teAttributes() == null) {
            return TeConstants.NIL_LONG_VALUE;
        }
        return teData.teAttributes().delay();
    }

    @Override
    public List<Long> srlgs() {
        if (teData == null || teData.teAttributes() == null) {
            return null;
        }
        return teData.teAttributes().srlgs();
    }

    @Override
    public Long administrativeGroup() {
        if (teData == null) {
            return null;
        }
        return teData.adminGroup();
    }

    @Override
    public List<Long> interLayerLocks() {
        if (teData == null) {
            return null;
        }
        return teData.interLayerLocks();
    }

    @Override
    public float[] maxBandwidth() {
        if (teData == null || teData.bandwidth() == null) {
            return null;
        }
        return teData.bandwidth().maxBandwidth();
    }

    @Override
    public float[] availBandwidth() {
        if (teData == null || teData.bandwidth() == null) {
            return null;
        }
        return teData.bandwidth().availBandwidth();
    }

    @Override
    public float[] maxAvailLspBandwidth() {
        if (teData == null || teData.bandwidth() == null) {
            return null;
        }
        return teData.bandwidth().maxAvailLspBandwidth();
    }

    @Override
    public float[] minAvailLspBandwidth() {
        if (teData == null || teData.bandwidth() == null) {
            return null;
        }
        return teData.bandwidth().minAvailLspBandwidth();
    }

    @Override
    public OduResource oduResource() {
        if (teData == null || teData.bandwidth() == null) {
            return null;
        }
        return teData.bandwidth().oduResource();
    }

    @Override
    public TeStatus adminStatus() {
        if (teData == null) {
            return null;
        }
        return teData.adminStatus();
    }

    @Override
    public TeStatus opStatus() {
        if (teData == null) {
            return null;
        }
        return teData.opStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teLinkKey, peerTeLinkKey, underlayTopologyId,
                                supportTeLinkId, sourceTeLinkId, teData);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTeLink) {
            DefaultTeLink that = (DefaultTeLink) object;
            return Objects.equal(teLinkKey, that.teLinkKey) &&
                    Objects.equal(peerTeLinkKey, that.peerTeLinkKey) &&
                    Objects.equal(underlayTopologyId, that.underlayTopologyId) &&
                    Objects.equal(supportTeLinkId, that.supportTeLinkId) &&
                    Objects.equal(sourceTeLinkId, that.sourceTeLinkId) &&
                    Objects.equal(teData, that.teData);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teLinkKey", teLinkKey)
                .add("peerTeLinkKey", peerTeLinkKey)
                .add("underlayTopologyId", underlayTopologyId)
                .add("supportTeLinkId", supportTeLinkId)
                .add("sourceTeLinkId", sourceTeLinkId)
                .add("teData", teData)
                .toString();
    }


}
