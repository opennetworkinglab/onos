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
package org.onosproject.drivers.polatis.snmp;

import org.onosproject.net.driver.DriverHandler;
import org.onosproject.snmp.SnmpDevice;
import org.onosproject.snmp.SnmpController;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * SNP utility for Polatis SNMP drivers.
 */
public final class PolatisSnmpUtility {

    private static final int MAX_SIZE_RESPONSE_PDU = 65535;

    private PolatisSnmpUtility() {
    }

    private static SnmpDevice getDevice(DriverHandler handler) {
        SnmpController controller = checkNotNull(handler.get(SnmpController.class));
        SnmpDevice device = controller.getDevice(handler.data().deviceId());
        return device;
    }

    private static CommunityTarget getTarget(DriverHandler handler) {
        SnmpDevice device = getDevice(handler);
        Address targetAddress = GenericAddress.parse(device.getProtocol() +
                                                     ":" + device.getSnmpHost() +
                                                     "/" + device.getSnmpPort());
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(getDevice(handler).getCommunity()));
        target.setAddress(targetAddress);
        target.setRetries(3);
        target.setTimeout(1000L * 3L);
        target.setVersion(SnmpConstants.version2c);
        target.setMaxSizeRequestPDU(MAX_SIZE_RESPONSE_PDU);
        return target;
    }

    private static Snmp getSession(DriverHandler handler) {
        Snmp session = getDevice(handler).getSession();
        return session;
    }

    /**
     * Sends a SET request.
     *
     * @param handler parent driver handler
     * @param pdu SNMP protocol data unit
     * @return the response event
     * @throws IOException if unable to send a set request
     */
    public static ResponseEvent set(DriverHandler handler, PDU pdu) throws IOException {
        Snmp session = getSession(handler);
        CommunityTarget target = getTarget(handler);
        return session.set(pdu, target);
    }

    /**
     * Retrieves static object value.
     *
     * @param handler parent driver handler
     * @param oid object identifier
     * @return the string value
     * @throws IOException if unable to retrieve the object value
     */
    public static String getOid(DriverHandler handler, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);
        Snmp session = getSession(handler);
        CommunityTarget target = getTarget(handler);
        ResponseEvent event = session.send(pdu, target);
        return event.getResponse().get(0).getVariable().toString();
    }
}
