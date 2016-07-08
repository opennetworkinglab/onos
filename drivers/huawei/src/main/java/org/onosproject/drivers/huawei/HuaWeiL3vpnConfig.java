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
package org.onosproject.drivers.huawei;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import org.dom4j.Document;
import org.onosproject.drivers.huawei.util.DocumentConvertUtil;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.L3vpnConfig;
import org.onosproject.net.behaviour.NetconfBgp;
import org.onosproject.net.behaviour.NetconfL3vpn;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

/**
 * Configures l3vpn on HuaWei devices.
 */
public class HuaWeiL3vpnConfig extends AbstractHandlerBehaviour
        implements L3vpnConfig {

    private final String RPC_XMLNS = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private final String CONFIG_XMLNS = "http://www.huawei.com/netconf/vrp";
    private final String ERROR_OPERATION = "rollback-on-error";

    @Override
    public boolean createVrf(DeviceId deviceId, NetconfL3vpn netconfL3vpn) {
        NetconfController controller = checkNotNull(handler()
                .get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(deviceId)
                .getSession();
        Document l3vpnDocument = DocumentConvertUtil
                .convertEditL3vpnDocument(RPC_XMLNS,
                                          UUID.randomUUID().toString(),
                                          NetconfConfigDatastoreType.RUNNING,
                                          ERROR_OPERATION, CONFIG_XMLNS,
                                          netconfL3vpn);
        boolean reply;
        try {
            reply = session.editConfig(l3vpnDocument.asXML());
        } catch (NetconfException e) {
            throw new RuntimeException(new NetconfException("Failed to create virtual routing forwarding.",
                                                            e));
        }
        return reply;
    }

    @Override
    public boolean createBgpImportProtocol(DeviceId deviceId,
                                           NetconfBgp netconfBgp) {
        NetconfController controller = checkNotNull(handler()
                .get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(deviceId)
                .getSession();
        Document bgpDocument = DocumentConvertUtil
                .convertEditBgpDocument(RPC_XMLNS, UUID.randomUUID().toString(),
                                        NetconfConfigDatastoreType.RUNNING,
                                        ERROR_OPERATION, CONFIG_XMLNS,
                                        netconfBgp);
        boolean reply;
        try {
            reply = session.editConfig(bgpDocument.asXML());
        } catch (NetconfException e) {
            throw new RuntimeException(new NetconfException("Failed to create bgp import protocol.",
                                                            e));
        }
        return reply;
    }


    /**
     * The enumeration of Netconf Config Datastore type.
     */
    public enum NetconfConfigDatastoreType {
        STARTUP(1), RUNNING(2), CANDIDATE(3), UNKNOWN(-1);

        int value;

        private NetconfConfigDatastoreType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * The enumeration of Filter type.
     */
    public enum FilterType {
        SUBTREE, XPATH, OTHERS;
    }

}
