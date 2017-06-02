#!/usr/bin/env python

import json

from mininet.net import Mininet
from mininet.node import UserSwitch, DefaultController, RemoteController, Host
from mininet.topo import Topo
from mininet.log import  setLogLevel, info, error, warn
from mininet.cli import CLI
from mininet.link import OVSIntf
from mininet.util import quietRun

from opticalUtils import LINCSwitch, LINCLink

"""XXX: separate out into domainlib"""
class Domain(object):
    """
    A container for switch, host, link, and controller information to be dumped
    into the Mininet mid-level API.
    """

    def __init__ (self, did=0):
        # each Domain has a numeric ID for sanity/convenience
        self.__dId = did

        # information about network elements - for calling the "mid-level" APIs
        self.__ctrls = {}
        self.__switches = {}
        self.__hosts = {}
        self.__links = {}
        # maps of devices, hosts, and controller names to actual objects
        self.__smap = {}
        self.__hmap = {}
        self.__cmap = {}

    def addController(self, name, **args):
        self.__ctrls[name] = args if args else {}
        return name

    # Note: This method will return the name of the swich, not the switch object
    def addSwitch(self, name, **args):
        self.__switches[name] = args if args else {}
        return name

    def addHost(self, name, **args):
        self.__hosts[name] = args if args else {}
        return name

    def addLink(self, src, dst, **args):
        self.__links[(src, dst)] = args if args else {}
        return (src, dst)

    def getId( self):
        return self.__dId

    def getControllers(self, name=None):
        return self.__cmap.values() if not name else self.__cmap.get(name)

    def getSwitches(self, name=None):
        return self.__smap.values() if not name else self.__smap.get(name)

    def getHosts(self, name=None):
        return self.__hmap.values() if not name else self.__hmap.get(name)

    def injectInto(self, net):
        """ Adds available topology info to a supplied Mininet object. """
        # add switches, hosts, then links to mininet object
        for sw, args in self.__switches.iteritems():
            self.__smap[sw] = net.addSwitch(sw, **args)
        for h, args in self.__hosts.iteritems():
            self.__hmap[h] = net.addHost(h, **args)
        for l, args in self.__links.iteritems():
            src = self.__smap.get(l[0])
            dst = self.__smap.get(l[1])
            net.addLink(src if src else self.__hmap.get(l[0]),
                         dst if dst else self.__hmap.get(l[1]), **args)
        # then controllers
        for c, args in self.__ctrls.iteritems():
            self.__cmap[c] = net.addController(c, **args)

    def start(self):
        """ starts the switches with the correct controller. """
        map(lambda c: c.start(), self.__cmap.values())
        map(lambda s: s.start(self.__cmap.values()), self.__smap.values())

    def build(self, *args):
        """ override for custom topology, similar to Topo """
        pass


class OpticalDomain(Domain):
    """ An emulated optical metro core. It is Domain 0. """
    def build(self):
        for i in range (1,4):
            oean = { "optical.regens": 0 }
            self.addSwitch('OE%s' % i, dpid='0000ffffffffff0%s' % i, annotations=oean, cls=LINCSwitch)

        # ROADM port number OE"1" -> OE'2' = "1"'2'00
        # leaving port number up to 100 open for use by Och port
        an = { "durable": "true" }
        self.addLink('OE1', 'OE2', port1=1200, port2=2100, annotations=an, cls=LINCLink)
        self.addLink('OE2', 'OE3', port1=2300, port2=3200, annotations=an, cls=LINCLink)
        self.addLink('OE3', 'OE1', port1=3100, port2=1300, annotations=an, cls=LINCLink)

class FabricDomain(Domain):
    """
    An emulated CO fabric, which is basically a K(n,m) bipartite graph.

    Each FabricDomain should be given a unique Domain ID (did) to ensure unique
    names and addressing.
    """
    def __init__(self, did):
        Domain.__init__(self, did)

    def build(self):

        # CpQD switches and OVS b/c brokenness.
        sw1 = self.addSwitch('cpqd%s1' % self.getId(), cls=UserSwitch, dpopts='--no-local-port')
        sw2 = self.addSwitch('ovs%s01' % self.getId())

        # make sw2 the tether point       
        self.__tether = sw2

        # sw1-sw2-> to metro core
        self.addLink(sw1, sw2, port2=1)

        # h-sw1
        h = self.addHost('h%s' % self.getId(), cls=IpHost, ip='10.0.0.%s/24' % self.getId(),
                         gateway='10.0.0.254')
        self.addLink(h, sw1)

    def getTether(self):
        """ get the switch name of this fabric facing the core """
        return self.__tether


class IpHost(Host):
    def __init__(self, name, gateway, *args, **kwargs):
        super(IpHost, self).__init__(name, *args, **kwargs)
        self.gateway = gateway

    def config(self, **kwargs):
        Host.config(self, **kwargs)
        mtu = "ifconfig "+self.name+"-eth0 mtu 1490"
        self.cmd(mtu)
        self.cmd('ip route add default via %s' % self.gateway)

def setup(argv):
    domains = []
    ctlsets = sys.argv[1:]

    # the controllers for the optical domain
    d0 = OpticalDomain()
    f0 = FabricDomain(1)
    f1 = FabricDomain(2)
    domains.extend([ d0, f0, f1 ])

    for i in range(len(domains)):
        ctls = ctlsets[i].split(',')
        for c in range(len(ctls)):
            domains[i].addController('c%s%s' % (i, c), controller=RemoteController, ip=ctls[c])

    # netcfg for each domains
    # Note: Separate netcfg for domain0 is created in opticalUtils
    domainCfgs = []
    for i in range (0,len(ctlsets)):
        cfg = {}
        cfg['devices'] = {}
        cfg['ports'] = {}
        cfg['links'] = {}
        domainCfgs.append(cfg)

    # make/setup Mininet object
    net = Mininet()
    for d in domains:
        d.build()
        d.injectInto(net)

    # connect COs to core - sort of hard-wired at this moment
    # adding cross-connect links
    for i in range(1,len(domains)):
        # add 10 cross-connect links between domains
        xcPortNo=2
        ochPortNo=10

        an = { "bandwidth": 10, "durable": "true" }
        net.addLink(domains[i].getTether(), d0.getSwitches('OE%s' % i),
                    port1=xcPortNo, port2=ochPortNo, speed=10000, annotations=an, cls=LINCLink)
 
        xcId = 'of:' + domains[i].getSwitches(name=domains[i].getTether()).dpid + '/' + str(xcPortNo)
        ochId = 'of:' + d0.getSwitches('OE%s' % i).dpid + '/' + str(ochPortNo)
        domainCfgs[i]['ports'][xcId] = {'cross-connect': {'remote': ochId}}

    # fire everything up
    net.build()
    map(lambda x: x.start(), domains)

    # create a minimal copy of the network for configuring LINC.
    cfgnet = Mininet()
    cfgnet.switches = net.switches
    cfgnet.links = net.links
    cfgnet.controllers = d0.getControllers()
    LINCSwitch.bootOE(cfgnet, d0.getSwitches())

    # send netcfg json to each CO-ONOS
    for i in range(1,len(domains)):
        info('*** Pushing Topology.json to CO-ONOS %d\n' % i)
        filename = 'Topology%d.json' % i
        with open(filename, 'w') as outfile:
            json.dump(domainCfgs[i], outfile, indent=4, separators=(',', ': '))

        output = quietRun('%s/tools/test/bin/onos-netcfg %s %s &'\
                           % (LINCSwitch.onosDir,
                              domains[i].getControllers()[0].ip,
                              filename), shell=True)
        # successful output contains the two characters '{}'
        # if there is more output than this, there is an issue
        if output.strip('{}'):
            warn('***WARNING: Could not push topology file to ONOS: %s\n' % output)

    CLI(net)
    net.stop()
    LINCSwitch.shutdownOE()

if __name__ == '__main__':
    setLogLevel('info')
    import sys
    if len(sys.argv) < 4:
        print ("Usage: sudo -E ./ectest.py ctl-set1 ... ctl-set4\n\n",
                "Where ctl-set are comma-separated controller IP's")
    else:
        setup(sys.argv)
