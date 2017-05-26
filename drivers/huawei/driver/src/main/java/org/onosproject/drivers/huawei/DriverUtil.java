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

package org.onosproject.drivers.huawei;

import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.DefaultDevices;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.DeviceKeys;
import org.onosproject.yang.model.AtomicPath;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.KeyInfo;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.MultiInstanceNode;

import java.util.Iterator;
import java.util.List;

import static org.onosproject.drivers.huawei.L3VpnUtil.getDevIdFromIns;

/**
 * Representation of utility for huawei driver.
 */
public final class DriverUtil {

    /**
     * Static constant value for devices.
     */
    static final String CONS_DEVICES = "Devices";

    /**
     * Error message for YANG model registry service not found.
     */
    static final String SERVICE_NOT_FOUND = "Service required for huawei " +
            "driver is not found.";

    /**
     * Error message for object from the standard device model being null.
     */
    static final String OBJECT_NULL = "Object from device model cannot be null";

    /**
     * Error message for device object under devices from the standard model
     * being null.
     */
    static final String DEVICE_NULL = "Device object from the devices of " +
            "standard device model cannot be null";

    /**
     * Error message for device object under devices from the standard model
     * being null.
     */
    static final String INS_NULL = "Instance object from the instances of " +
            "standard device model cannot be null";

    /**
     * Error message for unsupported model id level.
     */
    static final String UNSUPPORTED_MODEL_LVL = "The model id level is not " +
            "supported";

    /**
     * Error message for failure of device info retrieval.
     */
    static final String DEV_INFO_FAILURE = "Failed to retrieve device info.";

    /**
     * Error message for failure of interface info retrieval.
     */
    static final String INT_INFO_FAILURE = "Failed to retrieve Interfaces";

    /**
     * RPC message header.
     */
    static final String RPC_MSG = "<rpc message-id=\"101\" xmlns=\"urn:ietf:" +
            "params:xml:ns:netconf:base:1.0\">";

    /**
     * RPC get message.
     */
    static final String RPC_GET = "<get>";

    /**
     * RPC subtree filter message.
     */
    static final String RPC_FILTER = "<filter type=\"subtree\">";

    /**
     * RPC system message with namespace.
     */
    static final String RPC_SYS = "<system xmlns=\"http://www.huawei.com/" +
            "netconf/vrp\" format-version=\"1.0\" content-version=\"1.0\"/>";

    /**
     * RPC ifm message with namespace.
     */
    static final String RPC_IFM = "<ifm xmlns=\"http://www.huawei.com" +
            "/netconf/vrp\" format-version=\"1.0\" content-version=\"1.0\">";

    /**
     * RPC interfaces message.
     */
    static final String RPC_IFS = "<interfaces><interface><ifPhyType>Ethernet" +
            "</ifPhyType><ifName></ifName><ifNumber></ifNumber>" +
            "<ifDynamicInfo></ifDynamicInfo><ifStatistics></ifStatistics>" +
            "</interface></interfaces>";

    /**
     * RPC ifm message.
     */
    static final String RPC_CLOSE_IFM = "</ifm>";

    /**
     * RPC filter close message.
     */
    static final String RPC_CLOSE_FILTER = "</filter>";

    /**
     * RPC close message.
     */
    static final String RPC_CLOSE = "</rpc>";

    /**
     * RPC get close message.
     */
    static final String RPC_CLOSE_GET = "</get>";

    /**
     * Static constant for slash.
     */
    static final String SLASH = "/";

    /**
     * Static constant for devices name.
     */
    static final String DEVICES = "devices";

    /**
     * Static constant for devices namespace.
     */
    static final String NAMESPACE = "ne-l3vpn-api";

    /**
     * Error message for model object id having more than two objects.
     */
    private static final String MODEL_OBJ_ID_LIMIT = "The model object id " +
            "must not have more than two objects.";

    // No instantiation.
    private DriverUtil() {
    }

    /**
     * Returns the device id from the model object id. If model object id is
     * not present then null is returned. If only one path is available in
     * the list then devices value is returned.
     *
     * @param id    model object id
     * @param isIns if for VPN instance
     * @return device id
     */
    static String getIdFromModId(ModelObjectId id, boolean isIns) {
        if (id == null) {
            return null;
        }
        List<AtomicPath> paths = id.atomicPaths();
        int size = paths.size();

        switch (size) {
            case 1:
                return CONS_DEVICES;

            case 2:
                return getDevId(paths, isIns);

            default:
                throw new IllegalArgumentException(MODEL_OBJ_ID_LIMIT);
        }
    }

    /**
     * Returns the device id from the model object id's key info.
     *
     * @param paths atomic path list
     * @param isIns if VPN instance
     * @return device id
     */
    private static String getDevId(List<AtomicPath> paths, boolean isIns) {
        MultiInstanceNode info = (MultiInstanceNode) paths.get(1);
        KeyInfo key = info.key();
        if (!isIns) {
            return ((DeviceKeys) key).deviceid();
        }
        return getDevIdFromIns(key);
    }

    /**
     * Returns the first object from the model object data. If no objects are
     * present then it returns null.
     *
     * @param modData model object data
     * @return object
     */
    static Object getObjFromModData(ModelObjectData modData) {
        List<ModelObject> obj = modData.modelObjects();
        Iterator<ModelObject> it = obj.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    /**
     * Returns the model object id for with the devices object added to it.
     *
     * @return model object id
     */
    static ModelObjectId getModObjIdDriDevices() {
        return ModelObjectId.builder().addChild(DefaultDevices.class).build();
    }

    /**
     * Returns model object data built from the object that has to be added
     * and the model object id.
     *
     * @param id  model object id
     * @param obj object
     * @return model object data
     */
    static ModelObjectData getData(ModelObjectId id, InnerModelObject obj) {
        return DefaultModelObjectData.builder().addModelObject(obj)
                .identifier(id).build();
    }
}
