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

package org.onosproject.netconf.client.impl;

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.ResourceIdParser;
import org.onosproject.config.Filter;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.client.NetconfTranslator;
import org.onosproject.netconf.client.NetconfTranslator.OperationType;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.LeafNode;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.DefaultResourceData;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static com.google.common.base.Preconditions.checkNotNull;


@Beta
@Component(immediate = true)
public class NetconfActiveComponent implements DynamicConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NetconfActiveComponent.class);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfTranslator netconfTranslator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController controller;

    private final Accumulator<DynamicConfigEvent> accumulator = new InternalEventAccummulator();
    private static final String DEVNMSPACE = "ne-l3vpn-api";
    private static final String DEVICES = "devices";
    private static final String DEVICE = "device";
    private static final String DEVICE_ID = "deviceid";
    private static final String DEF_IP = "0:0:0"; //hack, remove later

    //Symbolic constants for use with the accumulator
    private static final int MAX_EVENTS = 1000;
    private static final int MAX_BATCH_MS = 5000;
    private static final int MAX_IDLE_MS = 1000;


    private ResourceId defParent = new ResourceId.Builder()
            .addBranchPointSchema(DEVICES, DEVNMSPACE)
            .addBranchPointSchema(DEVICE, DEVNMSPACE)
            //.addBranchPointSchema(DEVICE_ID, DEVNMSPACE)
            .addKeyLeaf(DEVICE_ID, DEVNMSPACE, DEF_IP)
            .build();

    //TODO remove this hack after store ordering is fixed
    private static final String EXCPATH = "root|devices#ne-l3vpn-api|" +
            "device#ne-l3vpn-api$deviceid#ne-l3vpn-api#netconf:172.16.5.11:22|" +
            "l3vpn#ne-l3vpn-api|l3vpncomm#ne-l3vpn-api|l3vpnInstances#ne-l3vpn-api|" +
            "l3vpnInstance#ne-l3vpn-api$vrfName#ne-l3vpn-api#vrf2|l3vpnIfs#ne-l3vpn-api";
    //end of hack

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
        String resId = ResourceIdParser.parseResId(event.subject());
        String refId = ResourceIdParser.parseResId(defParent);
        refId = refId.substring(0, refId.length() - (DEF_IP.length() + 1));
        if (!resId.contains(refId)) {
            return false;
        }
        if (resId.length() < refId.length()) {
            return false;
        }
        return (resId.substring(0, (refId.length())).compareTo(refId) == 0);
    }

    public boolean isMaster(DeviceId deviceId) {
        return mastershipService.isLocalMaster(deviceId);
    }


    @Override
    public void event(DynamicConfigEvent event) {
        accumulator.add(event);
    }

    /**
     * Performs the delete operation corresponding to the passed event.
     *
     * @param node       a relevant dataNode
     * @param deviceId   the deviceId of the device to be updated
     * @param resourceId the resourceId of the root of the subtree to be edited
     * @return true if the update succeeds false otherwise
     */
    private boolean    configDelete(DataNode node, DeviceId deviceId, ResourceId resourceId) {
        return parseAndEdit(node, deviceId, resourceId, OperationType.DELETE);
    }

    /**
     * Performs the update operation corresponding to the passed event.
     *
     * @param node       a relevant dataNode
     * @param deviceId   the deviceId of the device to be updated
     * @param resourceId the resourceId of the root of the subtree to be edited
     * @return true if the update succeeds false otherwise
     */
    private boolean configUpdate(DataNode node, DeviceId deviceId, ResourceId resourceId) {
        return parseAndEdit(node, deviceId, resourceId, OperationType.REPLACE);
    }

    /**
     * Parses the incoming event and pushes configuration to the effected
     * device.
     *
     * @param node          the dataNode effecting a particular device of which this node
     *                      is master
     * @param deviceId      the deviceId of the device to be modified
     * @param resourceId    the resourceId of the root of the subtree to be edited
     * @param operationType the type of editing to be performed
     * @return true if the operation succeeds, false otherwise
     */
    private boolean parseAndEdit(DataNode node, DeviceId deviceId,
                                 ResourceId resourceId,
                                 NetconfTranslator.OperationType operationType) {
        //FIXME Separate edit and delete, delete can proceed with a null node
        DefaultResourceData.Builder builder = DefaultResourceData.builder();
        if (node != null) {
            //add all low level nodes of devices
            Iterator<Map.Entry<NodeKey, DataNode>> it = ((InnerNode) node)
                    .childNodes().entrySet().iterator();
            while (it.hasNext()) {
                DataNode n = it.next().getValue();
                if (!n.key().schemaId().name().equals("deviceid")) {
                    builder.addDataNode(n);
                }
            }
        }
        //add resouce id //TODO: check if it is correct
        try {
            return netconfTranslator.editDeviceConfig(
                    deviceId, builder.build(), operationType);
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
    @Beta
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
        } else if (node.type() == DataNode.Type.MULTI_INSTANCE_NODE) {
            ListKey key = (ListKey) node.key();
            temp = key.keyLeafs().get(0).leafValAsString().split("\\:");
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
                throw new RuntimeException(new NetconfException("Unable to connect to NETCONF device on " +
                        deviceId, ex));
            }
        }
    }

    /**
     * Retrieves device key from Resource id.
     *
     * @param path associated with the event
     * @return the deviceId of the effected device
     */
    @Beta
    public ResourceId getDeviceKey(ResourceId path) {
        String resId = ResourceIdParser.parseResId(path);
        String[] el = resId.split(ResourceIdParser.EL_CHK);
        if (el.length < 3) {
            throw new RuntimeException(new NetconfException("Invalid resource id, cannot apply"));
        }
        if (!el[2].contains((ResourceIdParser.KEY_SEP))) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        String[] keys = el[2].split(ResourceIdParser.KEY_CHK);
        if (keys.length < 2) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        String[] parts = keys[1].split(ResourceIdParser.NM_CHK);
        if (parts.length < 3) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        if (parts[2].split("\\:").length != 3) {
            throw new RuntimeException(new NetconfException("Invalid device id form, cannot apply"));
        }
        return (new ResourceId.Builder()
                .addBranchPointSchema(el[1].split(ResourceIdParser.NM_CHK)[0],
                        el[1].split(ResourceIdParser.NM_CHK)[1])
                .addBranchPointSchema(keys[0].split(ResourceIdParser.NM_CHK)[0],
                        keys[0].split(ResourceIdParser.NM_CHK)[1])
                .addKeyLeaf(parts[0], parts[1], parts[2])
                .build());
    }

    /**
     * Retrieves device id from Resource id.
     *
     * @param path associated with the event
     * @return the deviceId of the effected device
     */
    @Beta
    public DeviceId getDeviceId(ResourceId path) {
        String resId = ResourceIdParser.parseResId(path);
        String[] el = resId.split(ResourceIdParser.EL_CHK);
        if (el.length < 3) {
            throw new RuntimeException(new NetconfException("Invalid resource id, cannot apply"));
        }
        if (!el[2].contains((ResourceIdParser.KEY_SEP))) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        String[] keys = el[2].split(ResourceIdParser.KEY_CHK);
        if (keys.length < 2) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        String[] parts = keys[1].split(ResourceIdParser.NM_CHK);
        if (parts.length < 3) {
            throw new RuntimeException(new NetconfException("Invalid device id key, cannot apply"));
        }
        String[] temp = parts[2].split("\\:");
        String ip, port;
        if (temp.length != 3) {
            throw new RuntimeException(new NetconfException("Invalid device id form, cannot apply"));
        }
        ip = temp[1];
        port = temp[2];
        try {
            return DeviceId.deviceId(new URI("netconf", ip + ":" + port, (String) null));
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Unable to build deviceID for device " + ip + ":" + port, ex);
        }
    }

    /* Accumulates events to allow processing after a desired number of events were accumulated.
    */
    private class InternalEventAccummulator extends AbstractAccumulator<DynamicConfigEvent> {
        protected InternalEventAccummulator() {
            super(new Timer("dyncfgevt-timer"), MAX_EVENTS, MAX_BATCH_MS, MAX_IDLE_MS);
        }
        @Override
        public void processItems(List<DynamicConfigEvent> events) {
            Map<ResourceId, List<DynamicConfigEvent>> evtmap = new LinkedHashMap<>();
            Boolean handleEx = false; //hack
            ResourceId exId = null; //hack
            for (DynamicConfigEvent e : events) {
                checkNotNull(e, "process:Event cannot be null");
                ResourceId cur = e.subject();
                //TODO remove this hack after store ordering is fixed
                String expat = ResourceIdParser.parseResId(cur);
                if ((expat.compareTo(EXCPATH) == 0) && (e.type() == DynamicConfigEvent.Type.NODE_ADDED)) {
                        handleEx = true;
                        exId = cur;
                } else { //actual code
                    ResourceId key = getDeviceKey(e.subject());
                    List<DynamicConfigEvent> el = evtmap.get(key);
                    if (el == null) {
                        el = new ArrayList<>();
                    }
                    el.add(e);
                    evtmap.put(key, el);
                }
            }
            evtmap.forEach((k, v) -> {
                DeviceId curDevice = getDeviceId(k);
                if (!isMaster(curDevice)) {
                    log.info("NetConfListener: not master, ignoring config for device {}", k);
                    return;
                }
                initiateConnection(curDevice);
               for (DynamicConfigEvent curEvt : v) {
                    switch (curEvt.type()) {
                        case NODE_ADDED:
                        case NODE_UPDATED:
                        case NODE_REPLACED:
                            Filter filt = new Filter();
                            DataNode node = cfgService.readNode(k, filt);
                            configUpdate(node, curDevice, k);
                            break;
                        case NODE_DELETED:
                            configDelete(null, curDevice, k); //TODO curEvt.subject())??
                            break;
                        case UNKNOWN_OPRN:
                        default:
                            log.warn("NetConfListener: unknown event: {}", curEvt.type());
                            break;
                    }
                }
            });
            //TODO remove this hack after store ordering is fixed
            if (handleEx) {
                DeviceId exDevice = getDeviceId(exId);
                if (!isMaster(exDevice)) {
                    log.info("NetConfListener: not master, ignoring config for expath {}", exId);
                    return;
                }
                initiateConnection(exDevice);
                Filter filt = new Filter();
                DataNode exnode = cfgService.readNode(exId, filt);
                configUpdate(exnode, exDevice, exId);
            } //end of hack
        }
    }
}