/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.dhcprelay.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcprelay.DhcpRelayConfig;
import org.onosproject.dhcprelay.DhcpRelayManager;
import org.onosproject.dhcprelay.DhcpRelayService;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.host.HostService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

/**
 * Prints DHCP server and DHCP relay status.
 */
@Command(scope = "onos", name = "dhcp-relay", description = "DHCP relay app cli.")
public class DhcpRelayCommand extends AbstractShellCommand {
    private static final String HEADER = "DHCP relay records ([D]: Directly connected):";
    private static final String NO_RECORDS = "No DHCP relay record found";
    private static final String BASIC_HOST_D = "%s/%s: %s[D], %s";
    private static final String BASIC_HOST = "%s/%s: %s, %s";
    private static final String NO_IP4 = "\tIPv4: N/A";
    private static final String IP4_INFO_D = "\tIPv4: %s: %s";
    private static final String IP4_INFO = "\tIPv4: %s via %s: %s";
    private static final String NO_IP6 = "\tIPv6: N/A";
    private static final String IP6_INFO_D = "\tIPv6: %s: %s";
    private static final String IP6_INFO = "\tIPv6: %s via %s: %s";
    private static final String DHCP_SERVER_GW = "DHCP Server: %s, %s via %s";
    private static final String DHCP_SERVER = "DHCP Server: %s, %s";
    private static final String MISSING_SERVER_CFG = "DHCP Server info not available";

    private static final DhcpRelayService DHCP_RELAY_SERVICE = get(DhcpRelayService.class);
    private static final NetworkConfigRegistry CFG_SERVICE = get(NetworkConfigRegistry.class);
    private static final HostService HOST_SERVICE = get(HostService.class);
    private static final CoreService CORE_SERVICE = get(CoreService.class);
    private static final ApplicationId APP_ID =
            CORE_SERVICE.getAppId(DhcpRelayManager.DHCP_RELAY_APP);

    @Override
    protected void execute() {
        DhcpRelayConfig cfg = CFG_SERVICE.getConfig(APP_ID, DhcpRelayConfig.class);
        if (cfg == null) {
            print(MISSING_SERVER_CFG);
            return;
        }

        // DHCP server information
        ConnectPoint connectPoint = cfg.getDhcpServerConnectPoint();
        Ip4Address gatewayAddress = cfg.getDhcpGatewayIp();
        Ip4Address serverIp = cfg.getDhcpServerIp();
        if (gatewayAddress != null) {
            print(DHCP_SERVER_GW, connectPoint, serverIp, gatewayAddress);
        } else {
            print(DHCP_SERVER, connectPoint, serverIp);
        }

        // DHCP records
        Collection<DhcpRecord> records = DHCP_RELAY_SERVICE.getDhcpRecords();
        if (records.isEmpty()) {
            print(NO_RECORDS);
            return;
        }
        print(HEADER);
        records.forEach(record -> {
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
            String lastSeen = df.format(new Date(record.lastSeen()));

            if (record.directlyConnected()) {
                print(BASIC_HOST_D, record.macAddress(), record.vlanId(),
                      record.locations(), lastSeen);
            } else {
                print(BASIC_HOST, record.macAddress(), record.vlanId(),
                      record.locations(), lastSeen);
            }

            if (record.ip4Status().isPresent()) {
                if (record.directlyConnected()) {
                    print(IP4_INFO_D,
                          record.ip4Address().orElse(null),
                          record.ip4Status().orElse(null));
                } else {
                    IpAddress gatewayIp = record.nextHop()
                            .flatMap(mac -> findGatewayIp(mac, record.vlanId()))
                            .orElse(null);
                    print(IP4_INFO,
                          record.ip4Address().orElse(null),
                          gatewayIp,
                          record.ip4Status().orElse(null));
                }
            } else {
                print(NO_IP4);
            }

            if (record.ip6Status().isPresent()) {
                if (record.directlyConnected()) {
                    print(IP6_INFO_D,
                          record.ip6Address().orElse(null),
                          record.ip6Status().orElse(null));
                } else {
                    IpAddress gatewayIp = record.nextHop()
                            .flatMap(mac -> findGatewayIp(mac, record.vlanId()))
                            .orElse(null);
                    print(IP6_INFO,
                          record.ip6Address().orElse(null),
                          gatewayIp,
                          record.ip6Status().orElse(null));
                }
            } else {
                print(NO_IP6);
            }
        });
    }

    private Optional<IpAddress> findGatewayIp(MacAddress macAddress, VlanId vlanId) {
        Host host = HOST_SERVICE.getHost(HostId.hostId(macAddress, vlanId));
        return host.ipAddresses().stream().findFirst();
    }
}
