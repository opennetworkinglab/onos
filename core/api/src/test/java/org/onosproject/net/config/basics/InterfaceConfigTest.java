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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class InterfaceConfigTest extends AbstractConfigTest {

    private JsonNode data;

    private ConnectPoint cp1 = NetTestTools.connectPoint("device1", 1);
    private ConfigApplyDelegate delegate = configApply -> { };
    private VlanId vl1 = VlanId.vlanId((short) 1);

    private String name1 = getName(1);
    private String name2 = getName(2);
    private String name3 = getName(3);
    private String name4 = getName(4);

    @Before
    public void setUp() {
        data = getTestJson("interface-config-1.json");
    }

    private String getName(int index) {
        String nameTemplate = "interface";
        return nameTemplate + Integer.toString(index);
    }

    private MacAddress getMac(int index) {
        String macTemplate = "AB:CD:EF:00:00:0";
        return MacAddress.valueOf(macTemplate + Integer.toString(index));
    }

    private InterfaceIpAddress getIp(int index, int position) {
        IpPrefix subnet1 = IpPrefix.valueOf("1.2.0.0/16");
        IpAddress ip = IpAddress.valueOf("1.2.3." + Integer.toString(index + ((position - 1) * 10)));
        return new InterfaceIpAddress(ip, subnet1);
    }

    private Interface findInterface(Collection<Interface> ifs, String name) {
        return ifs.stream()
                .filter(iface -> iface.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void checkInterface(Interface fetchedInterface, int index) {
        String name = getName(index);
        assertThat(fetchedInterface, notNullValue());
        assertThat(fetchedInterface.name(), is(name));
        assertThat(fetchedInterface.connectPoint(), is(cp1));
        assertThat(fetchedInterface.mac(), is(getMac(index)));
        List<InterfaceIpAddress> fetchedIpAddresses = fetchedInterface.ipAddressesList();
        assertThat(fetchedIpAddresses, hasItems(getIp(index, 1), getIp(index, 2)));
    }

    /**
     * Tests construction, setters and getters of an InterfaceConfig object.
     */
    @Test
    public void testConstruction() throws Exception {
        InterfaceConfig config = new InterfaceConfig();
        config.init(cp1, "KEY", data, mapper, delegate);

        assertThat(config.isValid(), is(true));
        assertThat(config.getInterfaces(), hasSize(4));

        Interface fetchedInterface1 = findInterface(config.getInterfaces(), name1);
        checkInterface(fetchedInterface1, 1);
        assertThat(fetchedInterface1.vlan(), is(vl1));

        Interface fetchedInterface2 = findInterface(config.getInterfaces(), name2);
        checkInterface(fetchedInterface2, 2);
        assertThat(fetchedInterface2.vlanUntagged().toShort(),  is((short) 22));
        assertThat(fetchedInterface2.vlan().toShort(), is((short) 2));

        Interface fetchedInterface3 = findInterface(config.getInterfaces(), name3);
        checkInterface(fetchedInterface3, 3);
        assertThat(fetchedInterface3.vlanTagged(),  is(allOf(notNullValue(), hasSize(1))));
        assertThat(fetchedInterface3.vlan().toShort(), is((short) 3));
        assertThat(fetchedInterface3.vlanTagged(), contains(VlanId.vlanId((short) 33)));

        Interface fetchedInterface4 = findInterface(config.getInterfaces(), name4);
        checkInterface(fetchedInterface4, 4);
        assertThat(fetchedInterface4.vlanTagged(),  is(allOf(notNullValue(), hasSize(1))));
        assertThat(fetchedInterface4.vlanTagged(), contains(VlanId.vlanId((short) 44)));
        assertThat(fetchedInterface4.vlanNative().toShort(), is((short) 4));
    }

    @Test
    public void testAddRemoveInterface() throws Exception {
        InterfaceConfig config = new InterfaceConfig();
        config.init(cp1, "KEY", data, mapper, delegate);

        Set<VlanId> vlanTagged = ImmutableSet.of(VlanId.vlanId((short) 33), VlanId.vlanId((short) 44));
        String newName = "interface5";
        Interface toAdd1 = new Interface(
                newName,
                cp1,
                ImmutableList.of(getIp(5, 1), getIp(5, 2)),
                getMac(5),
                vl1,
                null,
                vlanTagged,
                vl1);
        config.addInterface(toAdd1);

        assertThat(config.isValid(), is(true));
        assertThat(config.getInterfaces(), allOf(notNullValue(), hasSize(5)));

        assertThat(findInterface(config.getInterfaces(), name1), notNullValue());
        assertThat(findInterface(config.getInterfaces(), name2), notNullValue());
        assertThat(findInterface(config.getInterfaces(), name3), notNullValue());

        Interface addedInterface1 = findInterface(config.getInterfaces(), newName);
        checkInterface(addedInterface1, 5);
        assertThat(addedInterface1.vlan(), is(vl1));
        assertThat(addedInterface1.vlanTagged(), allOf(notNullValue(), hasSize(2)));

        config.removeInterface(newName);
        assertThat(config.getInterfaces(), allOf(notNullValue(), hasSize(4)));
        assertThat(findInterface(config.getInterfaces(), newName), nullValue());

        newName = getName(6);
        Interface toAdd2 = new Interface(
                newName,
                cp1,
                ImmutableList.of(getIp(6, 1), getIp(6, 2)),
                getMac(6),
                vl1,
                VlanId.vlanId((short) 77),
                null,
                vl1);
        config.addInterface(toAdd2);

        Interface addedInterface2 = findInterface(config.getInterfaces(), newName);
        checkInterface(addedInterface2, 6);
        assertThat(addedInterface2.vlan(), is(vl1));
        assertThat(addedInterface2.vlanUntagged().toShort(), is((short) 77));
    }

}