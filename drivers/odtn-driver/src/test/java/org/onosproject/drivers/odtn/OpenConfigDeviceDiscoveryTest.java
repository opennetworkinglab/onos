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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_TYPE;

import java.io.IOException;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Test;
import org.onosproject.net.device.PortDescription;
import com.google.common.io.CharSource;

public class OpenConfigDeviceDiscoveryTest {

    @Test
    public void testToPortDescription() throws ConfigurationException, IOException {
        // CHECKSTYLE:OFF
        String input =
        "<data>\n" +
        "<components  xmlns=\"http://openconfig.net/yang/platform\">\n" +
        "\n" +
        "  <component>\n" +
        "    <name>TPC_1_1_4_N2_200G</name>\n" +
        "    <config>\n" +
        "      <name>TPC_1_1_4_N2_200G</name>\n" +
        "    </config>\n" +
        "    <state>\n" +
        "      <name>TPC_1_1_4_N2_200G</name>\n" +
        "      <type  xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">oc-platform-types:LINECARD</type>\n" +
        "    </state>\n" +
        "  </component>\n" +
        "\n" +
        "  <component>\n" +
        "    <name>CLIPORT_1_1_4_1</name>\n" +
        "    <config>\n" +
        "      <name>CLIPORT_1_1_4_1</name>\n" +
        "    </config>\n" +
        "    <state>\n" +
        "      <name>CLIPORT_1_1_4_1</name>\n" +
        "      <type  xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">oc-platform-types:PORT</type>\n" +
        "    </state>\n" +
        "  </component>\n" +
        "\n" +
        "  <component>\n" +
        "    <name>LINEPORT_1_1_4</name>\n" +
        "    <config>\n" +
        "      <name>LINEPORT_1_1_4</name>\n" +
        "    </config>\n" +
        "    <state>\n" +
        "      <name>LINEPORT_1_1_4</name>\n" +
        "      <type  xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">oc-platform-types:PORT</type>\n" +
        "    </state>\n" +
        "  </component>\n" +
        "\n" +
        "  <component>\n" +
        "    <name>TRANSCEIVER_1_1_4_1</name>\n" +
        "    <config>\n" +
        "      <name>TRANSCEIVER_1_1_4_1</name>    \n" +
        "    </config>\n" +
        "    <state>\n" +
        "      <name>TRANSCEIVER_1_1_4_1</name>\n" +
        "      <type  xmlns:oc-platform-types=\"http://openconfig.net/yang/platform-types\">oc-platform-types:TRANSCEIVER</type>\n" +
        "    </state>\n" +
        "        <properties>\n" +
        "          <property>\n" +
        "            <name>onos-index</name>\n" +
        "            <config>\n" +
        "              <name>onos-index</name>\n" +
        "              <value>42</value>\n" +
        "            </config>\n" +
        "          </property>\n" +
        "        </properties>\n" +
        "    <transceiver  xmlns=\"http://openconfig.net/yang/platform/transceiver\">\n" +
        "      <config>\n" +
        "        <enabled>true</enabled>\n" +
        "        <form-factor-preconf  xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
        "        <ethernet-pmd-preconf  xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">oc-opt-types:ETH_100GBASE_LR4</ethernet-pmd-preconf>\n" +
        "      </config>\n" +
        "      <state>\n" +
        "        <enabled>true</enabled>\n" +
        "        <form-factor-preconf  xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">oc-opt-types:QSFP28</form-factor-preconf>\n" +
        "        <ethernet-pmd-preconf  xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">oc-opt-types:ETH_100GBASE_LR4</ethernet-pmd-preconf>\n" +
        "      </state>\n" +
        "    </transceiver>\n" +
        "  </component>\n" +
        "\n" +
        "  <component>\n" +
        "    <name>OPTCHANNEL_1_1_4</name>\n" +
        "    <config>\n" +
        "      <name>OPTCHANNEL_1_1_4</name>\n" +
        "    </config>\n" +
        "    <state>\n" +
        "      <name>OPTCHANNEL_1_1_4</name>\n" +
        "      <type  xmlns:oc-opt-types=\"http://openconfig.net/yang/transport-types\">oc-opt-types:OPTICAL_CHANNEL</type>\n" +
        "    </state>\n" +
        "    <optical-channel  xmlns=\"http://openconfig.net/yang/terminal-device\">\n" +
        "      <config>\n" +
        "        <frequency>191500000</frequency>\n" +
        "        <target-output-power>0.0</target-output-power>\n" +
        "      </config>\n" +
        "      <state>\n" +
        "        <frequency>191500000</frequency>\n" +
        "        <target-output-power>0.0</target-output-power>\n" +
        "      </state>\n" +
        "    </optical-channel>\n" +
        "  </component>\n" +
        "\n" +
        "</components>\n" +
        "</data>";
        // CHECKSTYLE:ON

        OpenConfigDeviceDiscovery sut = new OpenConfigDeviceDiscovery();

        XMLConfiguration cfg = new XMLConfiguration();
        cfg.load(CharSource.wrap(input).openStream());

        List<PortDescription> ports = sut.discoverPorts(cfg);

        assertThat(ports, hasSize(4));
        PortDescription portDescription = ports.get(2);
        assertThat(portDescription.portNumber().toLong(), is(42L));
        assertThat(portDescription.portNumber().name(), is("TRANSCEIVER_1_1_4_1"));


        assertThat(portDescription.annotations().value(OC_NAME), is("TRANSCEIVER_1_1_4_1"));
        assertThat(portDescription.annotations().value(OC_TYPE), is("oc-platform-types:TRANSCEIVER"));

    }

}
