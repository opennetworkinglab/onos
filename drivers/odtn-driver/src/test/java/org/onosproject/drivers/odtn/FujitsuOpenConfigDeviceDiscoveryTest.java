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
package org.onosproject.drivers.odtn;

import com.google.common.io.CharSource;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ODTN_PORT_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class FujitsuOpenConfigDeviceDiscoveryTest {
    private static final Logger log = getLogger(FujitsuOpenConfigDeviceDiscoveryTest.class);
    private static final String PORT_INPUT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"2\">\n" +
            "   <data>\n" +
            "      <components xmlns=\"http://openconfig.net/yang/platform\">\n" +
            "         <component>\n" +
            "            <name>transceiver-1/1/0/C1</name>\n" +
            "            <config>\n" +
            "               <name>transceiver-1/1/0/C1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>transceiver-1/1/0/C1</name>\n" +
            "               <type>TRANSCEIVER</type>\n" +
            "               <id>C1</id>\n" +
            "               <description />\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">" +
            "oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <transceiver xmlns=\"http://openconfig.net/yang/platform/transceiver\">\n" +
            "               <config>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "               </config>\n" +
            "               <state>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "                  <form-factor xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor>\n" +
            "                  <output-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </output-power>\n" +
            "                  <input-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </input-power>\n" +
            "                  <laser-bias-current>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </laser-bias-current>\n" +
            "               </state>\n" +
            "            </transceiver>\n" +
            "         </component>\n" +
            "         <component>\n" +
            "            <name>transceiver-1/1/0/C2</name>\n" +
            "            <config>\n" +
            "               <name>transceiver-1/1/0/C2</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>transceiver-1/1/0/C2</name>\n" +
            "               <type>TRANSCEIVER</type>\n" +
            "               <id>C2</id>\n" +
            "               <description />\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">" +
            "oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <transceiver xmlns=\"http://openconfig.net/yang/platform/transceiver\">\n" +
            "               <config>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "               </config>\n" +
            "               <state>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "                  <form-factor xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor>\n" +
            "                  <output-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </output-power>\n" +
            "                  <input-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </input-power>\n" +
            "                  <laser-bias-current>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </laser-bias-current>\n" +
            "               </state>\n" +
            "            </transceiver>\n" +
            "         </component>\n" +
            "         <component>\n" +
            "            <name>transceiver-1/1/0/C3</name>\n" +
            "            <config>\n" +
            "               <name>transceiver-1/1/0/C3</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>transceiver-1/1/0/C3</name>\n" +
            "               <type>TRANSCEIVER</type>\n" +
            "               <id>C3</id>\n" +
            "               <description />\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=" +
            "\"http://openconfig.net/yang/platform-types\">oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <transceiver xmlns=\"http://openconfig.net/yang/platform/transceiver\">\n" +
            "               <config>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "               </config>\n" +
            "               <state>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "                  <form-factor xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">" +
            "oc-opt-types:QSFP28</form-factor>\n" +
            "                  <output-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </output-power>\n" +
            "                  <input-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </input-power>\n" +
            "                  <laser-bias-current>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </laser-bias-current>\n" +
            "               </state>\n" +
            "            </transceiver>\n" +
            "         </component>\n" +
            "         <component>\n" +
            "            <name>transceiver-1/1/0/C4</name>\n" +
            "            <config>\n" +
            "               <name>transceiver-1/1/0/C4</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>transceiver-1/1/0/C4</name>\n" +
            "               <type>TRANSCEIVER</type>\n" +
            "               <id>C4</id>\n" +
            "               <description />\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=" +
            "\"http://openconfig.net/yang/platform-types\">oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <transceiver xmlns=\"http://openconfig.net/yang/platform/transceiver\">\n" +
            "               <config>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "               </config>\n" +
            "               <state>\n" +
            "                  <enabled>true</enabled>\n" +
            "                  <form-factor-preconf xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
            "                  <form-factor xmlns:oc-opt-types=" +
            "\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor>\n" +
            "                  <output-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </output-power>\n" +
            "                  <input-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </input-power>\n" +
            "                  <laser-bias-current>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </laser-bias-current>\n" +
            "               </state>\n" +
            "            </transceiver>\n" +
            "         </component>\n" +
            "         <component>\n" +
            "            <name>port-1/1/0/E1</name>\n" +
            "            <config>\n" +
            "               <name>port-1/1/0/E1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>port-1/1/0/E1</name>\n" +
            "               <type xmlns:oc-platform-types=" +
            "\"http://openconfig.net/yang/platform-types\">oc-platform-types:PORT</type>\n" +
            "               <id>E1</id>\n" +
            "               <description />\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=" +
            "\"http://openconfig.net/yang/platform-types\">oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <subcomponents>\n" +
            "               <subcomponent>\n" +
            "                  <name>otsi-1/1/0/E1</name>\n" +
            "                  <config>\n" +
            "                     <name>otsi-1/1/0/E1</name>\n" +
            "                  </config>\n" +
            "                  <state>\n" +
            "                     <name>otsi-1/1/0/E1</name>\n" +
            "                  </state>\n" +
            "               </subcomponent>\n" +
            "            </subcomponents>\n" +
            "         </component>\n" +
            "         <component>\n" +
            "            <name>otsi-1/1/0/E1</name>\n" +
            "            <config>\n" +
            "               <name>otsi-1/1/0/E1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "               <name>otsi-1/1/0/E1</name>\n" +
            "               <type>OPTICAL_CHANNEL</type>\n" +
            "               <id>E1</id>\n" +
            "               <mfg-name />\n" +
            "               <hardware-version />\n" +
            "               <serial-no />\n" +
            "               <part-no />\n" +
            "               <oper-status xmlns:oc-platform-types=" +
            "\"http://openconfig.net/yang/platform-types\">oc-platform-types:INACTIVE</oper-status>\n" +
            "            </state>\n" +
            "            <optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">\n" +
            "               <config>\n" +
            "                  <frequency>0</frequency>\n" +
            "                  <target-output-power>1.0</target-output-power>\n" +
            "                  <operational-mode>5</operational-mode>\n" +
            "               </config>\n" +
            "               <state>\n" +
            "                  <frequency>0</frequency>\n" +
            "                  <target-output-power>1.0</target-output-power>\n" +
            "                  <operational-mode>5</operational-mode>\n" +
            "                  <output-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </output-power>\n" +
            "                  <input-power>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </input-power>\n" +
            "                  <laser-bias-current>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                     <interval>900000000000</interval>\n" +
            "                  </laser-bias-current>\n" +
            "                  <chromatic-dispersion>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                  </chromatic-dispersion>\n" +
            "                  <polarization-mode-dispersion>\n" +
            "                     <instant>-99.9</instant>\n" +
            "                  </polarization-mode-dispersion>\n" +
            "               </state>\n" +
            "            </optical-channel>\n" +
            "         </component>\n" +
            "      </components>\n" +
            "   </data>\n" +
            "</rpc-reply>";

    private static final String DEVICE_DISCOVERY_INPUT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"2\">\n" +
            "  <data>\n" +
            "    <components xmlns=\"http://openconfig.net/yang/platform\">\n" +
            "      <component>\n" +
            "        <name>shelf-1</name>\n" +
            "        <config>\n" +
            "          <name>shelf-1</name>\n" +
            "        </config>\n" +
            "        <state>\n" +
            "          <name>shelf-1</name>\n" +
            "          <type xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">" +
            "oc-platform-types:CHASSIS</type>\n" +
            "          <id>1</id>\n" +
            "          <description>BDT6-T600</description>\n" +
            "          <mfg-name>FUJITSU</mfg-name>\n" +
            "          <hardware-version>01</hardware-version>\n" +
            "          <software-version>1.1.1</software-version>\n" +
            "          <serial-no>00026</serial-no>\n" +
            "          <part-no>FC9569T600</part-no>\n" +
            "          <oper-status xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">" +
            "oc-platform-types:ACTIVE</oper-status>\n" +
            "          <temperature>\n" +
            "            <instant>0.0</instant>\n" +
            "            <interval>900000000000</interval>\n" +
            "          </temperature>\n" +
            "          <memory>\n" +
            "            <available>1997</available>\n" +
            "            <utilized>632</utilized>\n" +
            "          </memory>\n" +
            "        </state>\n" +
            "        <subcomponents>\n" +
            "          <subcomponent>\n" +
            "            <name>slot-1/0</name>\n" +
            "            <config>\n" +
            "              <name>slot-1/0</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>slot-1/0</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>slot-1/1</name>\n" +
            "            <config>\n" +
            "              <name>slot-1/1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>slot-1/1</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>fan-1/FAN1</name>\n" +
            "            <config>\n" +
            "              <name>fan-1/FAN1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>fan-1/FAN1</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>fan-1/FAN2</name>\n" +
            "            <config>\n" +
            "              <name>fan-1/FAN2</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>fan-1/FAN2</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>fan-1/FAN3</name>\n" +
            "            <config>\n" +
            "              <name>fan-1/FAN3</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>fan-1/FAN3</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>fan-1/FAN4</name>\n" +
            "            <config>\n" +
            "              <name>fan-1/FAN4</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>fan-1/FAN4</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>fan-1/FAN5</name>\n" +
            "            <config>\n" +
            "              <name>fan-1/FAN5</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>fan-1/FAN5</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>psu-1/PWR1</name>\n" +
            "            <config>\n" +
            "              <name>psu-1/PWR1</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>psu-1/PWR1</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "          <subcomponent>\n" +
            "            <name>psu-1/PWR2</name>\n" +
            "            <config>\n" +
            "              <name>psu-1/PWR2</name>\n" +
            "            </config>\n" +
            "            <state>\n" +
            "              <name>psu-1/PWR2</name>\n" +
            "            </state>\n" +
            "          </subcomponent>\n" +
            "        </subcomponents>\n" +
            "        <cpu>\n" +
            "          <utilization xmlns=\"http://openconfig.net/yang/platform/cpu\">\n" +
            "            <state>\n" +
            "              <instant>8</instant>\n" +
            "              <avg>9</avg>\n" +
            "              <min>8</min>\n" +
            "              <max>11</max>\n" +
            "              <interval>600000000000</interval>\n" +
            "            </state>\n" +
            "          </utilization>\n" +
            "        </cpu>\n" +
            "      </component>\n" +
            "    </components>\n" +
            "  </data>\n" +
            "</rpc-reply>\n";
    private static final String DEVICE_ID = "netconf:167.253.10.10:830";
    private static final String DEVICE_MANUFACTURER = "FUJITSU";
    private static final String SW_VERSION = "1.1.1";
    private static final String TRANSCEIVER_COMPONENT_1 = "transceiver-1/1/0/C1";
    private static final String TRANSCEIVER = "TRANSCEIVER";
    private static final String OPTICAL_CHANNEL_COMPONENT_1 = "otsi-1/1/0/E1";
    private static final String OPTICAL_CHANNEL = "OPTICAL_CHANNEL";

    @Test
    public void testDeviceDescription() throws IOException, ConfigurationException, URISyntaxException {
        FujitsuOpenConfigDeviceDiscovery fujitsuOpenConfigDeviceDiscovery = new FujitsuOpenConfigDeviceDiscovery();
        URI uri = new URI(DEVICE_ID);
        XMLConfiguration xmlCfg = (XMLConfiguration) XmlConfigParser.loadXmlString(DEVICE_DISCOVERY_INPUT);
        xmlCfg.load(CharSource.wrap(DEVICE_DISCOVERY_INPUT).openStream());
        DeviceDescription deviceDescription = fujitsuOpenConfigDeviceDiscovery.parseDeviceInformation(uri, xmlCfg);
        assertEquals(deviceDescription.manufacturer(), DEVICE_MANUFACTURER);
        assertEquals(deviceDescription.swVersion(), SW_VERSION);
    }

    @Test
    public void testPortDescription() throws IOException, ConfigurationException {
        XMLConfiguration portCfg = new XMLConfiguration();
        portCfg.load(CharSource.wrap(PORT_INPUT).openStream());
        FujitsuOpenConfigDeviceDiscovery fujitsuOpenConfigDeviceDiscovery = new FujitsuOpenConfigDeviceDiscovery();
        List<PortDescription> t600Ports = fujitsuOpenConfigDeviceDiscovery.parse1FinityPorts(portCfg);
        assertThat(t600Ports.size(), is(5));
        PortDescription portDescription;
        portDescription = t600Ports.get(0);
        assertThat(portDescription.portNumber().name(), is(TRANSCEIVER_COMPONENT_1));
        assertThat(portDescription.annotations().value(OC_NAME), is(TRANSCEIVER_COMPONENT_1));
        assertThat(portDescription.annotations().value(OC_TYPE), is(TRANSCEIVER));
        assertThat(portDescription.annotations().value(ODTN_PORT_TYPE),
                is(FujitsuOpenConfigDeviceDiscovery.OdtnPortType.CLIENT.value()));
        assertThat(portDescription.type(), is(Port.Type.PACKET));
        portDescription = t600Ports.get(4);
        assertThat(portDescription.portNumber().name(), is(OPTICAL_CHANNEL_COMPONENT_1));
        assertThat(portDescription.annotations().value(OC_NAME), is(OPTICAL_CHANNEL_COMPONENT_1));
        assertThat(portDescription.annotations().value(OC_TYPE), is(OPTICAL_CHANNEL));
        assertThat(portDescription.annotations().value(ODTN_PORT_TYPE),
                is(FujitsuOpenConfigDeviceDiscovery.OdtnPortType.LINE.value()));
        assertThat(portDescription.type(), is(Port.Type.OCH));
    }
}
