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
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.DefaultResourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Beta
@Component(immediate = true)
public class NetconfYangListener implements DynamicConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NetconfYangListener.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService cfgServcie;
    public static final String DEVNMSPACE = "namespace1";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfTranslator netconfTranslator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private ResourceId resId = new ResourceId.Builder()
            .addBranchPointSchema("device", DEVNMSPACE )
            .build();
    @Activate
    protected void activate() {
        cfgServcie.addListener(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgServcie.removeListener(this);
        log.info("Stopped");
    }

    @Override
    public boolean isRelevant(DynamicConfigEvent event) {
        if (event.subject().equals(resId) &&
                mastershipService.isLocalMaster(retrieveDeviceId(event.subject()))) {
                return true;
            } else {
            return false;
        }
    }
    @Override
    public void event(DynamicConfigEvent event) {
        Filter filt = new Filter();
        DataNode node = cfgServcie.readNode(event.subject(), filt);
        DeviceId deviceId = retrieveDeviceId(event.subject());
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
     * Takes a resourceId corresponding to the provided event and uses it to
     * retrieve/generate a deviceId corresponding to the effected device.
     * @param resourceId the resourceId associated with the event
     * @return the deviceId of the effected device
     */
    private DeviceId retrieveDeviceId(ResourceId resourceId) {
        /*TODO this requires a real implementation instead of a placeholder */
        return DeviceId.NONE;
    }
}