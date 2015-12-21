#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel, info, debug
from mininet.node import Host, RemoteController, OVSSwitch

QUAGGA_DIR = '/usr/lib/quagga'
# Must exist and be owned by quagga user (quagga:quagga by default on Ubuntu)
QUAGGA_RUN_DIR = '/var/run/quagga'
CONFIG_DIR = 'configs-ipv6'

class SdnIpHost(Host):
    def __init__(self, name, ip, route, *args, **kwargs):
        Host.__init__(self, name, ip=ip, *args, **kwargs)

        self.name = name
        self.ip = ip
        self.route = route

    def config(self, **kwargs):
        Host.config(self, **kwargs)

        debug("configuring route %s" % self.route)

        self.cmd('ip addr add %s dev %s-eth0' % (self.ip, self.name))
        self.cmd('ip route add default via %s' % self.route)

class Router(Host):
    def __init__(self, name, quaggaConfFile, zebraConfFile, intfDict, *args, **kwargs):
        Host.__init__(self, name, *args, **kwargs)

        self.quaggaConfFile = quaggaConfFile
        self.zebraConfFile = zebraConfFile
        self.intfDict = intfDict

    def config(self, **kwargs):
        Host.config(self, **kwargs)
        self.cmd('sysctl net.ipv4.ip_forward=1')
        self.cmd('sysctl net.ipv6.conf.all.forwarding=1')

        for intf, attrs in self.intfDict.items():
            self.cmd('ip addr flush dev %s' % intf)
            if 'mac' in attrs:
                self.cmd('ip link set %s down' % intf)
                self.cmd('ip link set %s address %s' % (intf, attrs['mac']))
                self.cmd('ip link set %s up ' % intf)
            for addr in attrs['ipAddrs']:
                self.cmd('ip addr add %s dev %s' % (addr, intf))

        self.cmd('/usr/lib/quagga/zebra -d -f %s -z %s/zebra%s.api -i %s/zebra%s.pid' % (self.zebraConfFile, QUAGGA_RUN_DIR, self.name, QUAGGA_RUN_DIR, self.name))
        self.cmd('/usr/lib/quagga/bgpd -d -f %s -z %s/zebra%s.api -i %s/bgpd%s.pid' % (self.quaggaConfFile, QUAGGA_RUN_DIR, self.name, QUAGGA_RUN_DIR, self.name))


    def terminate(self):
        self.cmd("ps ax | egrep 'bgpd%s.pid|zebra%s.pid' | awk '{print $1}' | xargs kill" % (self.name, self.name))

        Host.terminate(self)

class SdnSwitch(OVSSwitch):
    def __init__(self, name, dpid, *args, **kwargs):
        OVSSwitch.__init__(self, name, dpid=dpid, *args, **kwargs)

    def start(self, controllers):
        OVSSwitch.start(self, controllers)
        self.cmd("ovs-vsctl set Bridge %s protocols=OpenFlow13" % self.name)


class SdnIpTopo( Topo ):
    "SDN-IP tutorial topology"

    def build( self ):
        s1 = self.addSwitch('s1', cls=SdnSwitch, dpid='00000000000000a1')
        s2 = self.addSwitch('s2', cls=SdnSwitch, dpid='00000000000000a2')
        s3 = self.addSwitch('s3', cls=SdnSwitch, dpid='00000000000000a3')
        s4 = self.addSwitch('s4', cls=SdnSwitch, dpid='00000000000000a4')
        s5 = self.addSwitch('s5', cls=SdnSwitch, dpid='00000000000000a5')
        s6 = self.addSwitch('s6', cls=SdnSwitch, dpid='00000000000000a6')

        zebraConf = '%s/zebra.conf' % CONFIG_DIR

        # Switches we want to attach our routers to, in the correct order
        attachmentSwitches = [s1, s2, s5, s6]

        for i in range(1, 4+1):
            name = 'r%s' % i

            eth0 = { 'mac' : '00:00:00:00:0%s:01' % i,
                     'ipAddrs' : ['2001:%s::1/48' % i] }
            eth1 = { 'ipAddrs' : ['2001:10%s::101/48' % i] }
            intfs = { '%s-eth0' % name : eth0,
                      '%s-eth1' % name : eth1 }

            quaggaConf = '%s/quagga%s.conf' % (CONFIG_DIR, i)

            router = self.addHost(name, cls=Router, quaggaConfFile=quaggaConf,
                                  zebraConfFile=zebraConf, intfDict=intfs)

            host = self.addHost('h%s' % i, cls=SdnIpHost,
                                ip='2001:10%s::1/48' % i,
                                route='2001:10%s::101' % i)

            self.addLink(router, attachmentSwitches[i-1])
            self.addLink(router, host)

        # Set up the internal BGP speaker
        bgpEth0 = { 'mac':'00:00:00:00:00:01',
                    'ipAddrs' : ['2001:1::101/48',
                                 '2001:2::101/48',
                                 '2001:3::101/48',
                                 '2001:4::101/48',] }
        bgpEth1 = { 'ipAddrs' : ['10.10.10.1/24'] }
        bgpIntfs = { 'bgp-eth0' : bgpEth0,
                     'bgp-eth1' : bgpEth1 }

        bgp = self.addHost( "bgp", cls=Router,
                             quaggaConfFile = '%s/quagga-sdn.conf' % CONFIG_DIR,
                             zebraConfFile = zebraConf,
                             intfDict=bgpIntfs )

        self.addLink( bgp, s3 )

        # Connect BGP speaker to the root namespace so it can peer with ONOS
        root = self.addHost( 'root', inNamespace=False, ip='10.10.10.2/24' )
        self.addLink( root, bgp )


        # Wire up the switches in the topology
        self.addLink( s1, s2 )
        self.addLink( s1, s3 )
        self.addLink( s2, s4 )
        self.addLink( s3, s4 )
        self.addLink( s3, s5 )
        self.addLink( s4, s6 )
        self.addLink( s5, s6 )

topos = { 'sdnip' : SdnIpTopo }

if __name__ == '__main__':
    setLogLevel('debug')
    topo = SdnIpTopo()

    net = Mininet(topo=topo, controller=RemoteController)

    net.start()

    CLI(net)

    net.stop()

    info("done\n")
