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

package org.onosproject.netconf.storeadapter;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;

import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.netconf.client.NetconfTranslator;
import org.onosproject.netconf.client.NetconfTranslator.OperationType;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfController;
import java.net.URI;
import java.net.URISyntaxException;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.DefaultResourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


@Beta
@Component(immediate = true)
public class NetconfActiveComponent implements DynamicConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NetconfActiveComponent.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService cfgService;
    public static final String DEVNMSPACE = "namespace1";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfTranslator netconfTranslator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController controller;

    private ResourceId resId = new ResourceId.Builder()
            .addBranchPointSchema("device", DEVNMSPACE )
            .build();
    @Activate
    protected void activate() {
        cfgService.addListener(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(this);
        log.info("Stopped");
    }

    @Override
    public boolean isRelevant(DynamicConfigEvent event) {
        if (event.subject().equals(resId)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isMaster(DeviceId deviceId) {
        if (mastershipService.isLocalMaster(deviceId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void event(DynamicConfigEvent event) {
        Filter filt = new Filter();
        DataNode node = cfgService.readNode(event.subject(), filt);
        DeviceId deviceId = getDeviceId(node);
        if (!isMaster(deviceId)) {
            log.info("NetConfListener: not master, ignoring config for {}", event.type());
            return;
        }
        initiateConnection(deviceId);
        switch (event.type()) {
            case NODE_ADDED:
            case NODE_UPDATED:
            case NODE_REPLACED:
                configUpdate(node, deviceId, event.subject());
                break;
            case NODE_DELETED:
                configDelete(node, deviceId, event.subject());
                break;
            case UNKNOWN_OPRN:
            default:
                log.warn("NetConfListener: unknown event: {}", event.type());
                break;
        }
    }

    /**
     * Performs the delete operation corresponding to the passed event.
     * @param node a relevant dataNode
     * @param deviceId the deviceId of the device to be updated
     * @param resourceId the resourceId of the root of the subtree to be edited
     * @return true if the update succeeds false otherwise
     */
    private boolean configDelete(DataNode node, DeviceId deviceId, ResourceId resourceId) {
        return parseAndEdit(node, deviceId, resourceId, OperationType.DELETE);
    }

    /**
     * Performs the update operation corresponding to the passed event.
     * @param node a relevant dataNode
     * @param deviceId the deviceId of the device to be updated
     * @param resourceId the resourceId of the root of the subtree to be edited
     * @return true if the update succeeds false otherwise
     */
    private boolean configUpdate(DataNode node, DeviceId deviceId, ResourceId resourceId) {
        return parseAndEdit(node, deviceId, resourceId, OperationType.REPLACE);
    }

    /**
     * Parses the incoming event and pushes configuration to the effected
     * device.
     * @param node the dataNode effecting a particular device of which this node
     *              is master
     * @param deviceId the deviceId of the device to be modified
     * @param resourceId the resourceId of the root of the subtree to be edited
     * @param operationType the type of editing to be performed
     * @return true if the operation succeeds, false otherwise
     */
    private boolean parseAndEdit(DataNode node, DeviceId deviceId,
                                 ResourceId resourceId,
                                 NetconfTranslator.OperationType operationType) {
        try {
            return netconfTranslator.editDeviceConfig(
                    deviceId,
                    DefaultResourceData.builder()
                            .addDataNode(node)
                            .resourceId(resourceId)
                            .build(),
                    operationType);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves device id from Data node.
     *
     * @param node the node associated with the event
     * @return the deviceId of the effected device
     */
    public DeviceId getDeviceId(DataNode node) {
        String[] temp;
        String ip, port;
        if (node.type() == DataNode.Type.SINGLE_INSTANCE_LEAF_VALUE_NODE) {
            temp = ((LeafNode) node).asString().split("\\:");
            if (temp.length != 3) {
                throw new RuntimeException(new NetconfException("Invalid device id form, cannot apply"));
            }
            ip = temp[1];
            port = temp[2];
        } else {
            throw new RuntimeException(new NetconfException("Invalid device id type, cannot apply"));
        }
        try {
            return DeviceId.deviceId(new URI("netconf", ip + ":" + port, (String) null));
        } catch (URISyntaxException var4) {
            throw new IllegalArgumentException("Unable to build deviceID for device " + ip + ":" + port, var4);
        }
    }

    /**
     * Inititates a Netconf connection to the device.
     *
     * @param deviceId of the added device
     */
    private void initiateConnection(DeviceId deviceId) {
        if (controller.getNetconfDevice(deviceId) == null) {
            try {
                //if (this.isReachable(deviceId)) {
                    this.controller.connectDevice(deviceId);
                //}
            } catch (Exception ex) {
                throw new RuntimeException(new NetconfException("Can\'t " +
                        "connect to NETCONF device on " + deviceId + ":" + deviceId, ex));
            }
        }
    }
}