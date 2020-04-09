/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.vpls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.StoreDelegate;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vpls.api.Vpls;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.config.VplsAppConfig;
import org.onosproject.vpls.config.VplsAppConfigTest;
import org.onosproject.vpls.config.VplsConfig;
import org.onosproject.vpls.store.VplsStoreAdapter;
import org.onosproject.vpls.store.VplsStoreEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class provides data for VPLS testing.
 */
public abstract class VplsTest {
    protected static final String APP_NAME = "org.onosproject.vpls";
    protected static final ApplicationId APPID = TestApplicationId.create(APP_NAME);
    protected static final String DASH = "-";
    protected static final int PRIORITY_OFFSET = 1000;
    protected static final String VPLS1 = "vpls1";
    protected static final String VPLS2 = "vpls2";
    protected static final String VPLS3 = "vpls3";
    protected static final String VPLS4 = "vpls4";

    protected static final PortNumber P1 = PortNumber.portNumber(1);
    protected static final PortNumber P2 = PortNumber.portNumber(2);

    protected static final DeviceId DID1 = getDeviceId(1);
    protected static final DeviceId DID2 = getDeviceId(2);
    protected static final DeviceId DID3 = getDeviceId(3);
    protected static final DeviceId DID4 = getDeviceId(4);
    protected static final DeviceId DID5 = getDeviceId(5);
    protected static final DeviceId DID6 = getDeviceId(6);

    protected static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    protected static final ConnectPoint CP2 = new ConnectPoint(DID2, P1);
    protected static final ConnectPoint CP3 = new ConnectPoint(DID3, P1);
    protected static final ConnectPoint CP4 = new ConnectPoint(DID4, P1);
    protected static final ConnectPoint CP5 = new ConnectPoint(DID5, P1);
    protected static final ConnectPoint CP6 = new ConnectPoint(DID6, P1);
    protected static final ConnectPoint CP7 = new ConnectPoint(DID4, P2);
    protected static final ConnectPoint CP8 = new ConnectPoint(DID3, P2);
    protected static final ConnectPoint CP9 = new ConnectPoint(DID5, P1);
    protected static final ConnectPoint CP10 = new ConnectPoint(DID5, P2);

    protected static final VlanId VLAN100 = VlanId.vlanId((short) 100);
    protected static final VlanId VLAN200 = VlanId.vlanId((short) 200);
    protected static final VlanId VLAN300 = VlanId.vlanId((short) 300);
    protected static final VlanId VLAN400 = VlanId.vlanId((short) 400);
    protected static final VlanId VLAN_NONE = VlanId.NONE;

    protected static final MacAddress MAC1 = getMac(1);
    protected static final MacAddress MAC2 = getMac(2);
    protected static final MacAddress MAC3 = getMac(3);
    protected static final MacAddress MAC4 = getMac(4);
    protected static final MacAddress MAC5 = getMac(5);
    protected static final MacAddress MAC6 = getMac(6);
    protected static final MacAddress MAC7 = getMac(7);
    protected static final MacAddress MAC8 = getMac(8);
    protected static final MacAddress MAC9 = getMac(9);
    protected static final MacAddress MAC10 = getMac(10);
    protected static final MacAddress MAC11 = getMac(11);

    protected static final Ip4Address IP1 = Ip4Address.valueOf("192.168.1.1");
    protected static final Ip4Address IP2 = Ip4Address.valueOf("192.168.1.2");

    protected static final HostId HID1 = HostId.hostId(MAC1, VLAN100);
    protected static final HostId HID2 = HostId.hostId(MAC2, VLAN100);
    protected static final HostId HID3 = HostId.hostId(MAC3, VLAN200);
    protected static final HostId HID4 = HostId.hostId(MAC4, VLAN200);
    protected static final HostId HID5 = HostId.hostId(MAC5, VLAN300);
    protected static final HostId HID6 = HostId.hostId(MAC6, VLAN300);
    protected static final HostId HID7 = HostId.hostId(MAC7, VLAN300);
    protected static final HostId HID8 = HostId.hostId(MAC8, VLAN400);
    protected static final HostId HID9 = HostId.hostId(MAC9);
    protected static final HostId HID10 = HostId.hostId(MAC10);
    protected static final HostId HID11 = HostId.hostId(MAC11);

    protected static final ProviderId PID = new ProviderId("of", "foo");

    protected static final NodeId NODE_ID_1 = new NodeId("Node1");
    protected static final NodeId NODE_ID_2 = new NodeId("Node2");

    protected static final Interface V100H1 =
            new Interface("v100h1", CP1, null, null, VLAN100);
    protected static final Interface V100H2 =
            new Interface("v100h2", CP2, null, null, VLAN100);
    protected static final Interface V200H1 =
            new Interface("v200h1", CP3, null, null, VLAN200);
    protected static final Interface V200H2 =
            new Interface("v200h2", CP4, null, null, VLAN200);
    protected static final Interface V300H1 =
            new Interface("v300h1", CP5, null, null, VLAN300);
    protected static final Interface V300H2 =
            new Interface("v300h2", CP6, null, null, VLAN300);
    protected static final Interface V400H1 =
            new Interface("v400h1", CP7, null, null, VLAN400);

    protected static final Interface VNONEH1 =
            new Interface("vNoneh1", CP8, null, null, VLAN_NONE);
    protected static final Interface VNONEH2 =
            new Interface("vNoneh2", CP9, null, null, VLAN_NONE);
    protected static final Interface VNONEH3 =
            new Interface("vNoneh3", CP10, null, null, VLAN_NONE);

    protected static final Host V100HOST1 =
            new DefaultHost(PID, HID1, MAC1, VLAN100,
                            getLocation(1), Collections.singleton(IP1));
    protected static final Host V100HOST2 =
            new DefaultHost(PID, HID2, MAC2, VLAN100,
                            getLocation(2), Sets.newHashSet());
    protected static final Host V200HOST1 =
            new DefaultHost(PID, HID3, MAC3, VLAN200,
                            getLocation(3), Collections.singleton(IP2));
    protected static final Host V200HOST2 =
            new DefaultHost(PID, HID4, MAC4, VLAN200,
                            getLocation(4), Sets.newHashSet());
    protected static final Host V300HOST1 =
            new DefaultHost(PID, HID5, MAC5, VLAN300,
                            getLocation(5), Sets.newHashSet());
    protected static final Host V300HOST2 =
            new DefaultHost(PID, HID6, MAC6, VLAN300,
                            getLocation(6), Sets.newHashSet());
    protected static final Host V300HOST3 =
            new DefaultHost(PID, HID7, MAC7, VLAN300,
                            getLocation(7), Sets.newHashSet());
    protected static final Host V400HOST1 =
            new DefaultHost(PID, HID8, MAC8, VLAN400,
                            getLocation(4, 2), Sets.newHashSet());

    protected static final Host VNONEHOST1 =
            new DefaultHost(PID, HID9, MAC9, VlanId.NONE,
                            getLocation(3, 2), Sets.newHashSet());
    protected static final Host VNONEHOST2 =
            new DefaultHost(PID, HID10, MAC10, VlanId.NONE,
                            getLocation(5, 1), Sets.newHashSet());
    protected static final Host VNONEHOST3 =
            new DefaultHost(PID, HID11, MAC11, VlanId.NONE,
                            getLocation(5, 2), Sets.newHashSet());

    protected static final Set<Interface> AVAILABLE_INTERFACES =
            ImmutableSet.of(V100H1, V100H2, V200H1, V200H2, V300H1, V300H2,
                            V400H1, VNONEH1, VNONEH2, VNONEH3);

    protected static final Set<Host> AVAILABLE_HOSTS =
            ImmutableSet.of(V100HOST1, V100HOST2, V200HOST1,
                            V200HOST2, V300HOST1, V300HOST2, V300HOST3,
                            VNONEHOST1, VNONEHOST2,
                            V400HOST1, VNONEHOST3);


    /**
     * Returns the device Id of the ith device.
     *
     * @param i the device to get the Id of
     * @return the device Id
     */
    protected static DeviceId getDeviceId(int i) {
        return DeviceId.deviceId("" + i);
    }

    /**
     * Generates a mac address by given number.
     *
     * @param n the number to generate mac address
     * @return the mac address
     */
    protected static MacAddress getMac(int n) {
        return MacAddress.valueOf(String.format("00:00:00:00:00:%02x", n));
    }

    /**
     * Generates a host location by given device number.
     *
     * @param i the given number
     * @return the host location
     */
    protected static HostLocation getLocation(int i) {
        return new HostLocation(new ConnectPoint(getDeviceId(i), P1), 123L);
    }

    /**
     * Generates host location by given device number and port number.
     *
     * @param d the device number
     * @param p the port number
     * @return the host location
     */
    protected static HostLocation getLocation(int d, int p) {
        return new HostLocation(new ConnectPoint(getDeviceId(d),
                                                 PortNumber.portNumber(p)), 123L);
    }

    /**
     * Test core service; For generate test application ID.
     */
    public class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return TestApplicationId.create(name);
        }
    }

    /**
     * Test intent service.
     * Always install or withdraw success for any Intents.
     */
    public class TestIntentService extends IntentServiceAdapter {
        IntentListener listener;
        List<IntentData> intents;

        public TestIntentService() {
            intents = Lists.newArrayList();
        }

        @Override
        public void submit(Intent intent) {
            intents.add(new IntentData(intent, IntentState.INSTALLED, new WallClockTimestamp()));
            if (listener != null) {
                IntentEvent.getEvent(IntentState.INSTALLED, intent).ifPresent(listener::event);

            }
        }

        @Override
        public void withdraw(Intent intent) {
            intents.forEach(intentData -> {
                if (intentData.intent().key().equals(intent.key())) {
                    intentData.setState(IntentState.WITHDRAWN);

                    if (listener != null) {
                        IntentEvent.getEvent(IntentState.WITHDRAWN, intent).ifPresent(listener::event);
                    }
                }
            });
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents.stream()
                    .map(IntentData::intent)
                    .collect(Collectors.toList());
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public void addListener(IntentListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(IntentListener listener) {
            this.listener = null;
        }
    }

    /**
     * Test leadership service.
     */
    public class TestLeadershipService extends LeadershipServiceAdapter {
        LeadershipEventListener listener;

        @Override
        public void addListener(LeadershipEventListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(LeadershipEventListener listener) {
            this.listener = null;
        }

        /**
         * Sends the leadership event to the listener.
         *
         * @param event the Intent event
         */
        public void sendEvent(LeadershipEvent event) {
            if (listener != null && listener.isRelevant(event)) {
                listener.event(event);
            }
        }

        @Override
        public NodeId getLeader(String path) {
            return NODE_ID_1;
        }
    }

    /**
     * Test interface service; contains all interfaces which already generated.
     */
    public class TestInterfaceService implements InterfaceService {

        @Override
        public void addListener(InterfaceListener listener) {
        }

        @Override
        public void removeListener(InterfaceListener listener) {
        }

        @Override
        public Set<Interface> getInterfaces() {
            return AVAILABLE_INTERFACES;
        }

        @Override
        public Interface getInterfaceByName(ConnectPoint connectPoint,
                                            String name) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.name().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint port) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.connectPoint().equals(port))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByIp(IpAddress ip) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.ipAddressesList().stream()
                                .map(InterfaceIpAddress::ipAddress)
                                .filter(ip::equals)
                                .findAny()
                                .isPresent())
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByVlan(VlanId vlan) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.vlan().equals(vlan))
                    .collect(Collectors.toSet());
        }

        @Override
        public Interface getMatchingInterface(IpAddress ip) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.ipAddressesList().stream()
                            .map(InterfaceIpAddress::ipAddress)
                            .filter(ip::equals)
                            .findAny()
                            .isPresent())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Set<Interface> getMatchingInterfaces(IpAddress ip) {
            return AVAILABLE_INTERFACES.stream()
                    .filter(intf -> intf.ipAddressesList().stream()
                            .map(InterfaceIpAddress::ipAddress)
                            .filter(ip::equals)
                            .findAny()
                            .isPresent())
                    .collect(Collectors.toSet());
        }

        @Override
        public boolean isConfigured(ConnectPoint connectPoint) {
            for (Interface intf : AVAILABLE_INTERFACES) {
                if (!intf.connectPoint().equals(connectPoint)) {
                    continue;
                }
                if (!intf.ipAddressesList().isEmpty()
                        || intf.vlan() != VlanId.NONE
                        || intf.vlanNative() != VlanId.NONE
                        || intf.vlanUntagged() != VlanId.NONE
                        || !intf.vlanTagged().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Test VPLS store.
     */
    public class TestVplsStore extends VplsStoreAdapter {
        /**
         * Clears the store.
         */
        public void clear() {
            vplsDataMap.clear();
        }

        /**
         * Gets the store delegate.
         *
         * @return the store delegate
         */
        public StoreDelegate<VplsStoreEvent> delegate() {
            return this.delegate;
        }
    }

    /**
     * Test VPLS.
     * Provides basic VPLS functionality and stores VPLS information.
     */
    public class TestVpls implements Vpls {
        public Map<String, VplsData> testData;

        public TestVpls() {
            testData = Maps.newHashMap();
        }

        public void initSampleData() {
            testData.clear();
            VplsData vplsData = VplsData.of(VPLS1);
            vplsData.addInterfaces(ImmutableSet.of(V100H1, V100H2));
            vplsData.state(VplsData.VplsState.ADDED);
            testData.put(VPLS1, vplsData);

            vplsData = VplsData.of(VPLS2);
            vplsData.addInterfaces(ImmutableSet.of(V200H1, V200H2));
            vplsData.state(VplsData.VplsState.ADDED);
            testData.put(VPLS2, vplsData);
        }

        @Override
        public VplsData createVpls(String vplsName, EncapsulationType encapsulationType) {
            VplsData vplsData = VplsData.of(vplsName, encapsulationType);
            vplsData.state(VplsData.VplsState.ADDED);
            testData.put(vplsName, vplsData);
            return vplsData;
        }

        @Override
        public VplsData removeVpls(VplsData vplsData) {
            if (!testData.containsKey(vplsData.name())) {
                return null;
            }

            testData.remove(vplsData.name());
            return vplsData;
        }

        @Override
        public void addInterfaces(VplsData vplsData, Collection<Interface> interfaces) {
            vplsData.addInterfaces(interfaces);
            testData.put(vplsData.name(), vplsData);
        }

        @Override
        public void addInterface(VplsData vplsData, Interface iface) {
            vplsData.addInterface(iface);
            testData.put(vplsData.name(), vplsData);
        }

        @Override
        public void setEncapsulationType(VplsData vplsData, EncapsulationType encapsulationType) {
            vplsData.encapsulationType(encapsulationType);
            testData.put(vplsData.name(), vplsData);
        }

        @Override
        public VplsData getVpls(String vplsName) {
            return testData.get(vplsName);
        }

        @Override
        public Collection<VplsData> getAllVpls() {
            return testData.values();
        }

        @Override
        public Collection<Interface> removeInterfaces(VplsData vplsData, Collection<Interface> interfaces) {
            vplsData.removeInterfaces(interfaces);
            testData.put(vplsData.name(), vplsData);
            return interfaces;
        }

        @Override
        public Interface removeInterface(VplsData vplsData, Interface iface) {
            vplsData.removeInterface(iface);
            testData.put(vplsData.name(), vplsData);
            return iface;
        }

        @Override
        public void removeAllVpls() {
            testData.clear();
        }
    }

    /**
     * Test host service; contains all hosts which already generated.
     *
     */
    public class TestHostService extends HostServiceAdapter {

        private HostListener listener;

        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            return AVAILABLE_HOSTS.stream()
                    .filter(host -> host.location().equals(connectPoint))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            return AVAILABLE_HOSTS.stream()
                    .filter(host -> host.mac().equals(mac))
                    .collect(Collectors.toSet());
        }

        @Override
        public Iterable<Host> getHosts() {
            return AVAILABLE_HOSTS;
        }

        @Override
        public Set<Host> getHostsByVlan(VlanId vlanId) {
            return AVAILABLE_HOSTS.stream()
                    .filter(host -> host.vlan().equals(vlanId))
                    .collect(Collectors.toSet());
        }

        @Override
        public int getHostCount() {
            return AVAILABLE_HOSTS.size();
        }

        @Override
        public Host getHost(HostId hostId) {
            return AVAILABLE_HOSTS.stream()
                    .filter(host -> host.id().equals(hostId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void addListener(HostListener listener) {
            this.listener = listener;
        }

        public void postHostEvent(HostEvent hostEvent) {
            this.listener.event(hostEvent);
        }
    }

    /**
     * Test network configuration service.
     */
    public class TestConfigService extends NetworkConfigServiceAdapter {
        public static final String EMPTY_JSON_TREE = "{}";
        NetworkConfigListener listener;
        VplsAppConfig vplsAppConfig;


        @Override
        public void addListener(NetworkConfigListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(NetworkConfigListener listener) {
            this.listener = null;
        }

        /**
         * Sends network config event to listener.
         *
         * @param event the network config event
         */
        public void sendEvent(NetworkConfigEvent event) {
            if (listener != null) {
                listener.event(event);
            }
        }

        /**
         * Constructs test config service.
         * Generates an VPLS configuration with sample VPLS configs.
         */
        public TestConfigService() {
            vplsAppConfig = new VplsAppConfig();
            final ObjectMapper mapper = new ObjectMapper();
            final ConfigApplyDelegate delegate = new VplsAppConfigTest.MockCfgDelegate();
            JsonNode tree = null;
            try {
                tree = new ObjectMapper().readTree(EMPTY_JSON_TREE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            vplsAppConfig.init(APPID, APP_NAME, tree, mapper, delegate);
            VplsConfig vplsConfig = new VplsConfig(VPLS1,
                                                   ImmutableSet.of(V100H1.name(), V100H2.name()),
                                                   EncapsulationType.NONE);
            vplsAppConfig.addVpls(vplsConfig);
            vplsConfig = new VplsConfig(VPLS2,
                                        ImmutableSet.of(V200H1.name(), V200H2.name()),
                                        EncapsulationType.VLAN);
            vplsAppConfig.addVpls(vplsConfig);

        }

        /**
         * Overrides VPLS config to the config service.
         *
         * @param vplsAppConfig the new VPLS config
         */
        public void setConfig(VplsAppConfig vplsAppConfig) {
            this.vplsAppConfig = vplsAppConfig;
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            return (C) vplsAppConfig;
        }

        @Override
        public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
            return (C) vplsAppConfig;
        }
    }

}
