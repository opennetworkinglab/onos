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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * App configuration object for Pwaas.
 */
public class PwaasConfig extends Config<ApplicationId> {

    private static Logger log = LoggerFactory
            .getLogger(PwaasConfig.class);

    private static final String SRC_CP = "cP1";
    private static final String DST_CP = "cP2";
    private static final String SRC_OUTER_TAG = "cP1OuterTag";
    private static final String DST_OUTER_TAG = "cP2OuterTag";
    private static final String SRC_INNER_TAG = "cP1InnerTag";
    private static final String DST_INNER_TAG = "cP2InnerTag";
    private static final String MODE = "mode";
    private static final String ALL_VLAN = "allVlan";
    private static final String SD_TAG = "sdTag";
    private static final String PW_LABEL = "pwLabel";

    /**
     * Error message for missing parameters.
     */
    private static final String MISSING_PARAMS = "Missing parameters in pseudo wire description";

    /**
     * Error message for invalid l2 mode.
     */
    private static final String INVALID_L2_MODE = "Invalid pseudo wire mode";

    /**
     * Verify if the pwaas configuration block is valid.
     *
     * @return true, if the configuration block is valid.
     *         False otherwise.
     */
    @Override
    public boolean isValid() {
        try {
            getPwIds().forEach(this::getPwDescription);
        } catch (IllegalArgumentException e) {
            log.warn("{}", e.getMessage());
            return false;
        }
        return true;

    }

    /**
     * Returns all pseudo wire keys.
     *
     * @return all keys (tunnels id)
     * @throws IllegalArgumentException if wrong format
     */
    public Set<Long> getPwIds() {
        ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
        object.fields().forEachRemaining(entry -> {
            Long tunnelId = Long.parseLong(entry.getKey());
            builder.add(tunnelId);
        });
        return builder.build();
    }

    /**
     * Returns pw description of given pseudo wire id.
     *
     * @param tunnelId pseudo wire key
     * @return set of l2 tunnel descriptions
     * @throws IllegalArgumentException if wrong format
     */
    public DefaultL2TunnelDescription getPwDescription(Long tunnelId) {
        JsonNode pwDescription = object.get(tunnelId.toString());
        if (!hasFields((ObjectNode) pwDescription,
                      SRC_CP, SRC_INNER_TAG, SRC_OUTER_TAG,
                      DST_CP, DST_INNER_TAG, DST_OUTER_TAG,
                      MODE, ALL_VLAN, SD_TAG, PW_LABEL)) {
            throw new IllegalArgumentException(MISSING_PARAMS);
        }
        String tempString;

        tempString = pwDescription.get(SRC_CP).asText();
        ConnectPoint srcCp = ConnectPoint.deviceConnectPoint(tempString);

        tempString = pwDescription.get(DST_CP).asText();
        ConnectPoint dstCp = ConnectPoint.deviceConnectPoint(tempString);

        tempString = pwDescription.get(SRC_INNER_TAG).asText();
        VlanId srcInnerTag = VlanId.vlanId(tempString);

        tempString = pwDescription.get(SRC_OUTER_TAG).asText();
        VlanId srcOuterTag = VlanId.vlanId(tempString);

        tempString = pwDescription.get(DST_INNER_TAG).asText();
        VlanId dstInnerTag = VlanId.vlanId(tempString);

        tempString = pwDescription.get(DST_OUTER_TAG).asText();
        VlanId dstOuterTag = VlanId.vlanId(tempString);

        tempString = pwDescription.get(MODE).asText();

        L2Mode l2Mode = L2Mode.valueOf(tempString);

        boolean allVlan = pwDescription.get(ALL_VLAN).asBoolean();

        tempString = pwDescription.get(SD_TAG).asText();
        VlanId sdTag = VlanId.vlanId(tempString);

        tempString = pwDescription.get(PW_LABEL).asText();
        MplsLabel pwLabel = MplsLabel.mplsLabel(tempString);

        DefaultL2Tunnel l2Tunnel = new DefaultL2Tunnel(
                l2Mode,
                sdTag,
                tunnelId,
                pwLabel
        );

        DefaultL2TunnelPolicy l2TunnelPolicy = new DefaultL2TunnelPolicy(
                tunnelId,
                srcCp,
                srcInnerTag,
                srcOuterTag,
                dstCp,
                dstInnerTag,
                dstOuterTag,
                allVlan
        );

        return new DefaultL2TunnelDescription(l2Tunnel, l2TunnelPolicy);
    }

}
