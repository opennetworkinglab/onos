/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnweb.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.VirtualPort;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * VirtualPort JSON codec.
 */
public final class VirtualPortCodec extends JsonCodec<VirtualPort> {
    @Override
    public ObjectNode encode(VirtualPort vPort, CodecContext context) {
        checkNotNull(vPort, "VPort cannot be null");
        ObjectNode result = context
                .mapper()
                .createObjectNode()
                .put("id", vPort.portId().toString())
                .put("network_id", vPort.networkId().toString())
                .put("admin_state_up", vPort.adminStateUp())
                .put("name", vPort.name())
                .put("status", vPort.state().toString())
                .put("mac_address", vPort.macAddress().toString())
                .put("tenant_id", vPort.tenantId().toString())
                .put("device_id", vPort.deviceId().toString())
                .put("device_owner", vPort.deviceOwner())
                .put("binding:vnic_type", vPort.bindingVnicType())
                .put("binding:Vif_type", vPort.bindingVifType())
                .put("binding:host_id", vPort.bindingHostId().toString())
                .put("binding:vif_details", vPort.bindingVifDetails());
        result.set("allowed_address_pairs", new AllowedAddressPairCodec().encode(
                                                                               vPort.allowedAddressPairs(), context));
        result.set("fixed_ips", new FixedIpCodec().encode(
                                                        vPort.fixedIps(), context));
        result.set("security_groups", new SecurityGroupCodec().encode(
                                                        vPort.securityGroups(), context));
        return result;
    }
}
