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

package org.onosproject.driver.extensions;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.behaviour.ExtensionResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionInstruction;
import org.onosproject.net.flow.instructions.ExtensionType;
import org.onosproject.openflow.controller.ExtensionInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTunnelIpv4Dst;
import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * Interpreter for Nicira OpenFlow extensions.
 */
public class NiciraExtensionInterpreter extends AbstractHandlerBehaviour
        implements ExtensionInterpreter, ExtensionResolver {

    @Override
    public boolean supported(ExtensionType extensionType) {
        if (extensionType.equals(ExtensionType.ExtensionTypes.NICIRA_SET_TUNNEL_DST)) {
            return true;
        }

        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionInstruction extensionInstruction) {
        ExtensionType type = extensionInstruction.type();
        if (type.equals(ExtensionType.ExtensionTypes.NICIRA_SET_TUNNEL_DST.type())) {
            NiciraSetTunnelDst tunnelDst = (NiciraSetTunnelDst) extensionInstruction;
            return factory.actions().setField(factory.oxms().tunnelIpv4Dst(
                    IPv4Address.of(tunnelDst.tunnelDst().toInt())));
        }
        return null;
    }

    @Override
    public ExtensionInstruction mapAction(OFAction action) {
        if (action.getType().equals(OFActionType.SET_FIELD)) {
            OFActionSetField setFieldAction = (OFActionSetField) action;
            OFOxm<?> oxm = setFieldAction.getField();
            switch (oxm.getMatchField().id) {
            case TUNNEL_IPV4_DST:
                OFOxmTunnelIpv4Dst tunnelIpv4Dst = (OFOxmTunnelIpv4Dst) oxm;
                return new NiciraSetTunnelDst(Ip4Address.valueOf(tunnelIpv4Dst.getValue().getInt()));
            default:
                throw new UnsupportedOperationException(
                        "Driver does not support extension type " + oxm.getMatchField().id);
            }
        }
        return null;
    }

    @Override
    public ExtensionInstruction getExtensionInstruction(ExtensionType type) {
        if (type.equals(ExtensionType.ExtensionTypes.NICIRA_SET_TUNNEL_DST.type())) {
            return new NiciraSetTunnelDst();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
