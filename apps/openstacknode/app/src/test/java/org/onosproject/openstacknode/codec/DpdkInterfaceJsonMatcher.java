/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.DpdkInterface.Type;
import org.slf4j.Logger;


import static org.slf4j.LoggerFactory.getLogger;

/**
 * Hamcrest matcher for dpdk interface.
 */
public final class DpdkInterfaceJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {
    private final Logger log = getLogger(getClass());

    private static final String DEVICE_NAME = "deviceName";
    private static final String INTF = "intf";
    private static final String PCI_ADDRESS = "pciAddress";
    private static final String TYPE = "type";
    private static final String MTU = "mtu";

    private final DpdkInterface dpdkIntf;

    private DpdkInterfaceJsonMatcher(DpdkInterface dpdkIntf) {
        this.dpdkIntf = dpdkIntf;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check device name
        String jsonDeviceName = jsonNode.get(DEVICE_NAME).asText();
        String deviceName = dpdkIntf.deviceName();
        if (!jsonDeviceName.equals(deviceName)) {
            description.appendText("device name was " + jsonDeviceName);
            return false;
        }

        String jsonIntf = jsonNode.get(INTF).asText();
        String intf = dpdkIntf.intf();
        if (!jsonIntf.equals(intf)) {
            description.appendText("interface name was " + jsonIntf);
            return false;
        }

        String jsonPciAddress = jsonNode.get(PCI_ADDRESS).asText();
        String pciAddress = dpdkIntf.pciAddress();
        if (!jsonPciAddress.equals(pciAddress)) {
            description.appendText("pci address was " + jsonPciAddress);
            return false;
        }

        Type jsonType = Type.valueOf(jsonNode.get(TYPE).asText());
        Type type = dpdkIntf.type();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType.name());
            return false;
        }

        JsonNode jsonMtu = jsonNode.get(MTU);
        if (jsonMtu != null) {
            Long mtu = dpdkIntf.mtu();
            if (!jsonMtu.asText().equals(mtu.toString())) {
                description.appendText("mtu was " + jsonMtu.asText());
                return false;
            }
        }

        return true;
    }


    @Override
    public void describeTo(Description description) {
        description.appendText(dpdkIntf.toString());
    }

    /**
     * Factory to allocate an dpdk interface matcher.
     *
     * @param dpdkIntf dpdk interface object we are looking for
     * @return matcher
     */
    public static DpdkInterfaceJsonMatcher matchesDpdkInterface(DpdkInterface dpdkIntf) {
        return new DpdkInterfaceJsonMatcher(dpdkIntf);
    }
}
