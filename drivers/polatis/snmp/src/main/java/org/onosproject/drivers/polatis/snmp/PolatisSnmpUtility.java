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
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * SNP utility for Polatis SNMP drivers.
 */
public final class PolatisSnmpUtility {

    private static final int MAX_SIZE_RESPONSE_PDU = 65535;

    private static final Logger log = getLogger(PolatisSnmpUtility.class);

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
     * @return the variable
     * @throws IOException if unable to retrieve the object value
     */
    public static Variable get(DriverHandler handler, String oid) throws IOException {
        List<VariableBinding> vbs = new ArrayList<>();
        vbs.add(new VariableBinding(new OID(oid)));
        PDU pdu = new PDU(PDU.GET, vbs);
        Snmp session = getSession(handler);
        CommunityTarget target = getTarget(handler);
        ResponseEvent event = session.send(pdu, target);
        return event.getResponse().get(0).getVariable();
    }

    /**
     * Retrieves a table.
     *
     * @param handler parent driver handler
     * @param columnOIDs column oid object identifiers
     * @return the table
     * @throws IOException if unable to retrieve the object value
     */
    public static List<TableEvent> getTable(DriverHandler handler, OID[] columnOIDs) throws IOException {
        Snmp session = getSession(handler);
        CommunityTarget target = getTarget(handler);
        TableUtils tableUtils = new TableUtils(session, new DefaultPDUFactory());
        return tableUtils.getTable(target, columnOIDs, null, null);
    }

    /**
     * Sends a synchronous SET request to the supplied target.
     *
     * @param handler parent driver handler
     * @param vbs a list of variable bindings
     * @return the response event
     * @throws IOException if unable to set the target
     */
    public static ResponseEvent set(DriverHandler handler, List<? extends VariableBinding> vbs) throws IOException {
        Snmp session = getSession(handler);
        CommunityTarget target = getTarget(handler);
        PDU pdu = new PDU(PDU.SET, vbs);
        return session.set(pdu, target);
    }
}
