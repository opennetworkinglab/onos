/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.ovsdb;

import org.onlab.packet.IpAddress;
import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DefaultQosDescription;
import org.onosproject.net.behaviour.QosConfigBehaviour;
import org.onosproject.net.behaviour.QosDescription;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbQos;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.ovsdb.controller.OvsdbConstant.CBS;
import static org.onosproject.ovsdb.controller.OvsdbConstant.CIR;
import static org.onosproject.ovsdb.controller.OvsdbConstant.MAX_RATE;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_EGRESS_POLICER;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_EXTERNAL_ID_KEY;
import static org.onosproject.ovsdb.controller.OvsdbConstant.QOS_TYPE_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OVSDB-based implementation of qos config behaviour.
 */
public class OvsdbQosConfig extends AbstractHandlerBehaviour implements QosConfigBehaviour {

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<QosDescription> getQoses() {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return null;
        }
        Set<OvsdbQos> qoses = ovsdbClient.getQoses();

        return qoses.stream()
                .map(qos -> DefaultQosDescription.builder()
                        .qosId(QosId.qosId(qos.externalIds().get(QOS_EXTERNAL_ID_KEY)))
                        .type(QOS_EGRESS_POLICER.equals(qos.qosType()) ?
                                QosDescription.Type.EGRESS_POLICER :
                                QosDescription.Type.valueOf(qos.qosType()
                                        .replace(QOS_TYPE_PREFIX, "")
                                        .toUpperCase()))
                        .maxRate(qos.otherConfigs().get(MAX_RATE) != null ?
                                Bandwidth.bps(Long.parseLong(qos.otherConfigs().get(MAX_RATE))) :
                                Bandwidth.bps(0L))
                        .cbs(qos.otherConfigs().get(CBS) != null ?
                                Long.valueOf(qos.otherConfigs().get(CBS)) : null)
                        .cir(qos.otherConfigs().get(CIR) != null ?
                                Long.valueOf(qos.otherConfigs().get(CIR)) : null)
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public QosDescription getQos(QosDescription qosDesc) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return null;
        }
        OvsdbQos qos = ovsdbClient.getQos(qosDesc.qosId());
        if (qos == null) {
            return null;
        }
        return DefaultQosDescription.builder()
                        .qosId(QosId.qosId(qos.externalIds().get(QOS_EXTERNAL_ID_KEY)))
                        .type(QOS_EGRESS_POLICER.equals(qos.qosType()) ?
                                QosDescription.Type.EGRESS_POLICER :
                                QosDescription.Type.valueOf(qos.qosType()
                                        .replace(QOS_TYPE_PREFIX, "")
                                        .toUpperCase()))
                        .maxRate(qos.otherConfigs().get(MAX_RATE) != null ?
                                Bandwidth.bps(Long.parseLong(qos.otherConfigs().get(MAX_RATE))) :
                                Bandwidth.bps(0L))
                        .cbs(qos.otherConfigs().get(CBS) != null ?
                                Long.valueOf(qos.otherConfigs().get(CBS)) : null)
                        .cir(qos.otherConfigs().get(CIR) != null ?
                                Long.valueOf(qos.otherConfigs().get(CIR)) : null)
                        .build();
    }

    @Override
    public boolean addQoS(QosDescription qos) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        OvsdbQos ovsdbQos = OvsdbQos.builder(qos).build();
        return ovsdbClient.createQos(ovsdbQos);
    }

    @Override
    public void deleteQoS(QosId qosId) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        ovsdbClient.dropQos(qosId);
    }

    // OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

    private OvsdbClientService getOvsdbClient(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(handler.data().deviceId());

        return ovsController.getOvsdbClient(nodeId);
    }
}

