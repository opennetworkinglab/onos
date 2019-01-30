#!/usr/bin/python

import os
import sys
import json
import argparse
from collections import OrderedDict

TEMP_NETCFG_FILE = '/tmp/bmv2-demo-cfg.json'
BASE_LONGITUDE = -115
SWITCH_BASE_LATITUDE = 25
HOST_BASE_LATITUDE = 28
BASE_SHIFT = 8
VLAN_NONE = -1
DEFAULT_SW_BW = 50
DEFAULT_HOST_BW = 25
# Jumbo frame
JUMBO_MTU=9000

if 'ONOS_ROOT' not in os.environ:
    print "Environment var $ONOS_ROOT not set"
    exit()
else:
    ONOS_ROOT = os.environ["ONOS_ROOT"]
    sys.path.append(ONOS_ROOT + "/tools/dev/mininet")
if 'RUN_PACK_PATH' not in os.environ:
    print "Environment var $RUN_PACK_PATH not set"
    exit()
else:
    RUN_PACK_PATH = os.environ["RUN_PACK_PATH"]

from onos import ONOSCluster, ONOSCLI
from bmv2 import ONOSBmv2Switch, ONOSHost

from itertools import combinations
from time import sleep
from subprocess import call

from mininet.cli import CLI
from mininet.link import TCLink
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController, Host
from mininet.topo import Topo


def getCmdBg(cmd, logfile="/dev/null"):
    return "{} > {} 2>&1 &".format(cmd, logfile)


class ClosTopo(Topo):
    """2 stage Clos topology"""

    def __init__(self, args, **opts):
        # Initialize topology and default options
        Topo.__init__(self, **opts)

        bmv2SwitchIds = []
        for row in (1, 2):
            for col in range(1, args.size + 1):
                bmv2SwitchIds.append("s%d%d" % (row, col))

        bmv2Switches = {}

        for switchId in bmv2SwitchIds:
            deviceId = int(switchId[1:])
            # Use first number in device id to calculate latitude (row number),
            # use second to calculate longitude (column number)
            latitude = SWITCH_BASE_LATITUDE + (deviceId // 10) * BASE_SHIFT
            longitude = BASE_LONGITUDE + (deviceId % 10) * BASE_SHIFT

            bmv2Switches[switchId] = self.addSwitch(switchId,
                                                    cls=ONOSBmv2Switch,
                                                    loglevel=args.log_level,
                                                    deviceId=deviceId,
                                                    netcfg=True,
                                                    netcfgDelay=0.5,
                                                    longitude=longitude,
                                                    latitude=latitude,
                                                    pipeconf=args.pipeconf_id)

        for i in range(1, args.size + 1):
            for j in range(1, args.size + 1):
                if i == j:
                    self.addLink(bmv2Switches["s1%d" % i],
                                 bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=DEFAULT_SW_BW)
                    if args.with_imbalanced_striping:
                        # 2 links
                        self.addLink(bmv2Switches["s1%d" % i],
                                     bmv2Switches["s2%d" % j],
                                     cls=TCLink, bw=DEFAULT_SW_BW)
                else:
                    self.addLink(bmv2Switches["s1%d" % i],
                                 bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=DEFAULT_SW_BW)

        for hostId in range(1, args.size + 1):
            host = self.addHost("h%d" % hostId,
                                cls=DemoHost,
                                ip="10.0.0.%d/24" % hostId,
                                mac='00:00:00:00:00:%02x' % hostId)
            self.addLink(host, bmv2Switches["s1%d" % hostId],
                         cls=TCLink, bw=DEFAULT_HOST_BW)


class DemoHost(ONOSHost):
    """Demo host"""

    def __init__(self, name, **params):
        ONOSHost.__init__(self, name, **params)
        self.exectoken = "/tmp/mn-exec-token-host-%s" % name
        self.cmd("touch %s" % self.exectoken)

    def startPingBg(self, h):
        self.cmd(self.getInfiniteCmdBg("ping -i0.5 %s" % h.IP()))
        self.cmd(self.getInfiniteCmdBg("arping -w5000000 %s" % h.IP()))

    def startIperfServer(self):
        self.cmd(self.getInfiniteCmdBg("iperf -s -u"))

    def startIperfClient(self, h, flowBw="512k", numFlows=5, duration=5):
        iperfCmd = "iperf -c{} -u -b{} -P{} -t{}".format(
            h.IP(), flowBw, numFlows, duration)
        self.cmd(self.getInfiniteCmdBg(iperfCmd, delay=0))

    def stop(self, **kwargs):
        self.cmd("killall iperf")
        self.cmd("killall ping")
        self.cmd("killall arping")

    def describe(self):
        print "**********"
        print self.name
        print "default interface: %s\t%s\t%s" % (
            self.defaultIntf().name,
            self.defaultIntf().IP(),
            self.defaultIntf().MAC()
        )
        print "**********"

    def getInfiniteCmdBg(self, cmd, logfile="/dev/null", delay=1):
        return "(while [ -e {} ]; " \
               "do {}; " \
               "sleep {}; " \
               "done;) > {} 2>&1 &".format(self.exectoken, cmd, delay, logfile)


def generateNetcfg(onosIp, net, args):
    netcfg = OrderedDict()

    netcfg['hosts'] = {}
    netcfg['devices'] = {}
    netcfg['links'] = {}

    if args.full_netcfg:
        # Device configs
        for sw in net.switches:
            srcIp = sw.getSourceIp(onosIp)
            netcfg['devices'][sw.onosDeviceId] = sw.getDeviceConfig(srcIp)

    hostLocations = {}
    for link in net.links:
        switchPort = link.intf1.name.split('-')
        sw1Name = switchPort[0]  # s11
        port1Name = switchPort[1]  # eth0
        port1 = port1Name[3:]
        switchPort = link.intf2.name.split('-')
        sw2Name = switchPort[0]
        port2Name = switchPort[1]
        port2 = port2Name[3:]
        sw1 = net[sw1Name]
        sw2 = net[sw2Name]
        if isinstance(sw1, Host):
            # record host location and ignore it
            # e.g. {'h1': 'device:bmv2:11'}
            hostLocations[sw1.name] = '%s/%s' % (sw2.onosDeviceId, port2)
            continue

        if isinstance(sw2, Host):
            # record host location and ignore it
            # e.g. {'h1': 'device:bmv2:11'}
            hostLocations[sw2.name] = '%s/%s' % (sw1.onosDeviceId, port1)
            continue

        if args.full_netcfg:
            # Link configs
            for linkId in ('%s/%s-%s/%s' % (sw1.onosDeviceId, port1,
                                            sw2.onosDeviceId, port2),
                           '%s/%s-%s/%s' % (sw2.onosDeviceId, port2,
                                            sw1.onosDeviceId, port1)):
                netcfg['links'][linkId] = {
                    'basic': {
                        'type': 'DIRECT',
                        'bandwidth': DEFAULT_SW_BW
                    }
                }

    # Host configs
    longitude = BASE_LONGITUDE
    for host in net.hosts:
        longitude = longitude + BASE_SHIFT
        hostDefaultIntf = host.defaultIntf()
        hostMac = host.MAC(hostDefaultIntf)
        hostIp = host.IP(hostDefaultIntf)
        hostId = '%s/%d' % (hostMac, VLAN_NONE)
        location = hostLocations[host.name]

        # use host Id to generate host location
        hostConfig = {
            'basic': {
                'locations': [location],
                'ips': [hostIp],
                'name': host.name,
                'latitude': HOST_BASE_LATITUDE,
                'longitude': longitude
            }
        }
        netcfg['hosts'][hostId] = hostConfig

    if args.full_netcfg:
        netcfg["apps"] = {
            "org.onosproject.core": {
                "core": {
                    "linkDiscoveryMode": "STRICT"
                }
            }
        }

    print "Writing network config to %s" % TEMP_NETCFG_FILE
    with open(TEMP_NETCFG_FILE, 'w') as tempFile:
        json.dump(netcfg, tempFile, indent=4)

def setMTU(net, mtu):
    for link in net.links:
        intf1 = link.intf1.name
        switchPort = intf1.split('-')
        sw1Name = switchPort[0]  # s11
        sw1 = net[sw1Name]

        intf2 = link.intf2.name
        switchPort = intf2.split('-')
        sw2Name = switchPort[0]
        sw2 = net[sw2Name]

        if isinstance(sw1, Host):
            continue

        if isinstance(sw2, Host):
            continue

        call(('ifconfig', intf1, 'mtu', str(mtu)))
        call(('ifconfig', intf2, 'mtu', str(mtu)))

def main(args):
    if not args.onos_ip:
        controller = ONOSCluster('c0', 3)
        onosIp = controller.nodes()[0].IP()
    else:
        controller = RemoteController('c0', ip=args.onos_ip)
        onosIp = args.onos_ip

    topo = ClosTopo(args)

    net = Mininet(topo=topo, build=False, controller=[controller])

    net.build()
    net.start()

    print "Network started"

    # Always generate background pings.
    sleep(3)
    for (h1, h2) in combinations(net.hosts, 2):
        h1.startPingBg(h2)
        h2.startPingBg(h1)

    print "Background ping started"

    # Increase the MTU size for INT operation
    if args.pipeconf_id.endswith("int") or args.pipeconf_id.endswith("full"):
        setMTU(net, JUMBO_MTU)

    for h in net.hosts:
        h.startIperfServer()

    print "Iperf servers started"

    if args.bg_traffic:
        sleep(4)
        print "Starting iperf clients..."
        net.hosts[0].startIperfClient(net.hosts[-1], flowBw="400k",
                                      numFlows=50, duration=10)

    generateNetcfg(onosIp, net, args)

    if args.netcfg_sleep > 0:
        print "Waiting %d seconds before pushing config to ONOS..." \
              % args.netcfg_sleep
        sleep(args.netcfg_sleep)

    print "Pushing config to ONOS..."
    call(("%s/onos-netcfg" % RUN_PACK_PATH, onosIp, TEMP_NETCFG_FILE))

    if not args.onos_ip:
        ONOSCLI(net)
    else:
        CLI(net)

    net.stop()
    call(("rm", "-f", TEMP_NETCFG_FILE))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='BMv2 mininet demo script (2-stage Clos topology)')
    parser.add_argument('--onos-ip', help='ONOS-BMv2 controller IP address',
                        type=str, action="store", required=False)
    parser.add_argument('--size', help='Number of leaf/spine switches',
                        type=int, action="store", required=False, default=2)
    parser.add_argument('--with-imbalanced-striping',
                        help='Topology with imbalanced striping',
                        type=bool, action="store", required=False,
                        default=False)
    parser.add_argument('--pipeconf-id', help='Pipeconf ID for switches',
                        type=str, action="store", required=False, default='')
    parser.add_argument('--netcfg-sleep',
                        help='Seconds to wait before pushing config to ONOS',
                        type=int, action="store", required=False, default=5)
    parser.add_argument('--log-level', help='BMv2 log level',
                        type=str, action="store", required=False,
                        default='warn')
    parser.add_argument('--full-netcfg',
                        help='Generate full netcfg JSON with links and devices',
                        type=bool, action="store", required=False,
                        default=False)
    parser.add_argument('--bg-traffic',
                        help='Starts background traffic',
                        type=bool, action="store", required=False,
                        default=False)
    setLogLevel('info')
    main(parser.parse_args())
