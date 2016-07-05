#!/usr/bin/python

import os
import sys
import argparse

if 'ONOS_ROOT' not in os.environ:
    print "Environment var $ONOS_ROOT not set"
    exit()
else:
    ONOS_ROOT = os.environ["ONOS_ROOT"]
    sys.path.append(ONOS_ROOT + "/tools/dev/mininet")

from onos import ONOSCluster, ONOSCLI
from bmv2 import ONOSBmv2Switch

from itertools import combinations
from time import sleep
from subprocess import call

from mininet.cli import CLI
from mininet.link import TCLink
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController, Host
from mininet.topo import Topo, SingleSwitchTopo


class ClosTopo(Topo):
    "2 stage Clos topology"

    def __init__(self, **opts):
        # Initialize topology and default options
        Topo.__init__(self, **opts)

        bmv2SwitchIds = ["s11", "s12", "s13", "s21", "s22", "s23"]

        bmv2Switches = {}

        tport = 9090
        for switchId in bmv2SwitchIds:
            bmv2Switches[switchId] = self.addSwitch(switchId,
                                                    cls=ONOSBmv2Switch,
                                                    loglevel="warn",
                                                    deviceId=int(switchId[1:]),
                                                    thriftPort=tport)
            tport += 1

        for i in (1, 2, 3):
            for j in (1, 2, 3):
                if i == j:
                    # 2 links
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=50)
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=50)
                else:
                    self.addLink(bmv2Switches["s1%d" % i], bmv2Switches["s2%d" % j],
                                 cls=TCLink, bw=50)

        for hostId in (1, 2, 3):
            host = self.addHost("h%d" % hostId,
                                cls=DemoHost,
                                ip="10.0.0.%d/24" % hostId,
                                mac='00:00:00:00:00:%02x' % hostId)
            self.addLink(host, bmv2Switches["s1%d" % hostId], cls=TCLink, bw=22)


class DemoHost(Host):
    "Demo host"

    def __init__(self, name, inNamespace=True, **params):
        Host.__init__(self, name, inNamespace=inNamespace, **params)
        self.exectoken = "/tmp/mn-exec-token-host-%s" % name
        self.cmd("touch %s" % self.exectoken)

    def config(self, **params):
        r = super(Host, self).config(**params)

        self.defaultIntf().rename("eth0")

        for off in ["rx", "tx", "sg"]:
            cmd = "/sbin/ethtool --offload eth0 %s off" % off
            self.cmd(cmd)

        # disable IPv6
        self.cmd("sysctl -w net.ipv6.conf.all.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.default.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.lo.disable_ipv6=1")

        return r

    def startPingBg(self, h):
        self.cmd(self.getInfiniteCmdBg("ping -i0.5 %s" % h.IP()))
        self.cmd(self.getInfiniteCmdBg("arping -w5000000 %s" % h.IP()))

    def startIperfServer(self):
        self.cmd(self.getInfiniteCmdBg("iperf3 -s"))

    def startIperfClient(self, h, flowBw="512k", numFlows=5, duration=5):
        iperfCmd = "iperf3 -c{} -b{} -P{} -t{}".format(h.IP(), flowBw, numFlows, duration)
        self.cmd(self.getInfiniteCmdBg(iperfCmd, sleep=0))

    def stop(self):
        self.cmd("killall iperf3")
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

    def getInfiniteCmdBg(self, cmd, logfile="/dev/null", sleep=1):
        return "(while [ -e {} ]; " \
               "do {}; " \
               "sleep {}; " \
               "done;) > {} 2>&1 &".format(self.exectoken, cmd, sleep, logfile)

    def getCmdBg(self, cmd, logfile="/dev/null"):
        return "{} > {} 2>&1 &".format(cmd, logfile)


def main(args):
    topo = ClosTopo()

    if not args.onos_ip:
        controller = ONOSCluster('c0', 3)
        onosIp = controller.nodes()[0].IP()
    else:
        controller = RemoteController('c0', ip=args.onos_ip, port=args.onos_port)
        onosIp = args.onos_ip

    net = Mininet(topo=topo, build=False, controller=[controller])

    net.build()
    net.start()

    print "Network started"

    # Generate background traffic.
    sleep(3)
    for (h1, h2) in combinations(net.hosts, 2):
        h1.startPingBg(h2)
        h2.startPingBg(h1)

    print "Background ping started"

    for h in net.hosts:
        h.startIperfServer()

    print "Iperf servers started"

    # sleep(4)
    # print "Starting traffic from h1 to h3..."
    # net.hosts[0].startIperfClient(net.hosts[-1], flowBw="200k", numFlows=100, duration=10)

    print "Setting netcfg..."
    call(("onos-netcfg", onosIp,
          "%s/tools/test/topos/bmv2-demo-cfg.json" % ONOS_ROOT))

    if not args.onos_ip:
        ONOSCLI(net)
    else:
        CLI(net)

    net.stop()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='BMv2 mininet demo script (2-stage Clos topology)')
    parser.add_argument('--onos-ip', help='ONOS-BMv2 controller IP address',
                        type=str, action="store", required=False)
    parser.add_argument('--onos-port', help='ONOS-BMv2 controller port',
                        type=int, action="store", default=40123)
    args = parser.parse_args()
    setLogLevel('info')
    main(args)
