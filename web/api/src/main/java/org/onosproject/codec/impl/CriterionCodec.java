/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Criterion codec.
 */
public class CriterionCodec extends JsonCodec<Criterion> {

    protected static final Logger log = LoggerFactory.getLogger(CriterionCodec.class);

    @Override
    public ObjectNode encode(Criterion criterion, CodecContext context) {
        checkNotNull(criterion, "Criterion cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("type", criterion.type().toString());

        switch (criterion.type()) {

            case IN_PORT:
                final Criteria.PortCriterion portCriterion = (Criteria.PortCriterion) criterion;
                result.put("tcpPort", portCriterion.port().toLong());
                break;

            case ETH_SRC:
            case ETH_DST:
                final Criteria.EthCriterion ethCriterion = (Criteria.EthCriterion) criterion;
                result.put("mac", ethCriterion.mac().toString());
                break;

            case ETH_TYPE:
                final Criteria.EthTypeCriterion ethTypeCriterion =
                        (Criteria.EthTypeCriterion) criterion;
                result.put("ethType", ethTypeCriterion.ethType());
                break;

            case IPV4_SRC:
            case IPV6_SRC:
            case IPV4_DST:
            case IPV6_DST:
                final Criteria.IPCriterion iPCriterion = (Criteria.IPCriterion) criterion;
                result.put("ip", iPCriterion.ip().toString());
                break;

            case IP_PROTO:
                final Criteria.IPProtocolCriterion iPProtocolCriterion =
                        (Criteria.IPProtocolCriterion) criterion;
                result.put("protocol", iPProtocolCriterion.protocol());
                break;

            case VLAN_PCP:
                final Criteria.VlanPcpCriterion vlanPcpCriterion =
                        (Criteria.VlanPcpCriterion) criterion;
                result.put("priority", vlanPcpCriterion.priority());
                break;

            case VLAN_VID:
                final Criteria.VlanIdCriterion vlanIdCriterion =
                        (Criteria.VlanIdCriterion) criterion;
                result.put("vlanId", vlanIdCriterion.vlanId().toShort());
                break;

            case TCP_SRC:
            case TCP_DST:
                final Criteria.TcpPortCriterion tcpPortCriterion =
                        (Criteria.TcpPortCriterion) criterion;
                result.put("tcpPort", tcpPortCriterion.tcpPort().byteValue());
                break;

            case MPLS_LABEL:
                final Criteria.MplsCriterion mplsCriterion =
                        (Criteria.MplsCriterion) criterion;
                result.put("label", mplsCriterion.label());
                break;

            case OCH_SIGID:
                final Criteria.LambdaCriterion lambdaCriterion =
                        (Criteria.LambdaCriterion) criterion;
                result.put("lambda", lambdaCriterion.lambda());
                break;

            case OCH_SIGTYPE:
                final Criteria.OpticalSignalTypeCriterion opticalSignalTypeCriterion =
                        (Criteria.OpticalSignalTypeCriterion) criterion;
                result.put("signalType", opticalSignalTypeCriterion.signalType());
                break;

            default:
                // Don't know how to format this type
                log.info("Cannot convert criterion of type {} to JSON",
                        criterion.type());
                break;
        }

        return result;
    }
}
