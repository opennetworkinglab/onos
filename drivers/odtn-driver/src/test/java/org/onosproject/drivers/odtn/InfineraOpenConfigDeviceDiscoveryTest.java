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
import java.io.IOException;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import org.onosproject.net.device.PortDescription;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ODTN_PORT_TYPE;

public class InfineraOpenConfigDeviceDiscoveryTest {

    @Test
    public void testToPortDescription() throws ConfigurationException, IOException {
        // CHECKSTYLE:OFF
        String input =
          "<data>\n" +
          "  <interfaces xmlns=\"http://openconfig.net/yang/interfaces\">\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-2</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-2</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-3</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-3</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-4</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-4</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-5</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-5</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>CARRIERCTP.1-L1-6</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>CARRIERCTP.1-L1-6</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>GIGECLIENTCTP.1-A-2-T1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>GIGECLIENTCTP.1-A-2-T1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>GIGECLIENTCTP.1-A-2-T2</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>GIGECLIENTCTP.1-A-2-T2</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>GIGECLIENTCTP.1-L1-1-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>GIGECLIENTCTP.1-L1-1-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>GIGECLIENTCTP.1-L2-1-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>GIGECLIENTCTP.1-L2-1-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>NCTGIGE.1-NCT-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:ethernetCsmacd</type>\n" +
          "        <name>NCTGIGE.1-NCT-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>NCTGIGE.1-NCT-2</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:ethernetCsmacd</type>\n" +
          "        <name>NCTGIGE.1-NCT-2</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>OCHCTP.1-L1-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>OCHCTP.1-L1-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>SCHCTP.1-L1-1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>SCHCTP.1-L1-1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>TRIBPTP.1-A-2-T1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>TRIBPTP.1-A-2-T1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>TRIBPTP.1-A-2-T2</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>TRIBPTP.1-A-2-T2</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "    <interface>\n" +
          "      <name>XTSCGPTP.1-L1</name>\n" +
          "      <config>\n" +
          "        <type xmlns:ianaift=\"urn:ietf:params:xml:ns:yang:iana-if-type\">ianaift:opticalTransport</type>\n" +
          "        <name>XTSCGPTP.1-L1</name>\n" +
          "        <description/>\n" +
          "        <enabled>true</enabled>\n" +
          "      </config>\n" +
          "    </interface>\n" +
          "  </interfaces>\n" +
          "</data>\n";
        // CHECKSTYLE:ON

        InfineraOpenConfigDeviceDiscovery sut = new InfineraOpenConfigDeviceDiscovery();

        XMLConfiguration cfg = new XMLConfiguration();
        cfg.load(CharSource.wrap(input).openStream());

        List<PortDescription> ports = sut.discoverPorts(cfg);

        assertThat(ports, hasSize(4));

        PortDescription portDescription;
        portDescription = ports.get(0);
        assertThat(portDescription.portNumber().toLong(), is(1L));
        assertThat(portDescription.portNumber().name(), is("GIGECLIENTCTP.1-A-2-T1"));
        assertThat(portDescription.annotations().value(OC_NAME), is("GIGECLIENTCTP.1-A-2-T1"));
        assertThat(portDescription.annotations().value(OC_TYPE), is("GIGECLIENTCTP.1-A-2-T1"));
        assertThat(portDescription.annotations().value(ODTN_PORT_TYPE),
                is(OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value()));

        portDescription = ports.get(3);
        assertThat(portDescription.portNumber().toLong(), is(102L));
        assertThat(portDescription.portNumber().name(), is("GIGECLIENTCTP.1-L2-1-1"));
        assertThat(portDescription.annotations().value(OC_NAME), is("GIGECLIENTCTP.1-L2-1-1"));
        assertThat(portDescription.annotations().value(OC_TYPE), is("GIGECLIENTCTP.1-L2-1-1"));
        assertThat(portDescription.annotations().value(ODTN_PORT_TYPE),
                is(OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value()));
    }
}
