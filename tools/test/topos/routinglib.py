#!/usr/bin/python

"""
Libraries for creating L3 topologies with routing protocols.
"""

from mininet.node import Host, OVSBridge
from mininet.nodelib import NAT
from mininet.log import info, debug, error
from mininet.cli import CLI
from ipaddress import ip_network, ip_address, ip_interface
import os

class RoutedHost(Host):
    """Host that can be configured with multiple IP addresses."""
    def __init__(self, name, ips, gateway, *args, **kwargs):
        super(RoutedHost, self).__init__(name, *args, **kwargs)

        self.ips = ips
        self.gateway = gateway

    def config(self, **kwargs):
        Host.config(self, **kwargs)

        self.cmd('ip addr flush dev %s' % self.defaultIntf())
        for ip in self.ips:
            self.cmd('ip addr add %s dev %s' % (ip, self.defaultIntf()))

        self.cmd('ip route add default via %s' % self.gateway)

class Router(Host):
    
    """An L3 router.
    Configures the Linux kernel for L3 forwarding and supports rich interface
    configuration of IP addresses, MAC addresses and VLANs."""
    
    def __init__(self, name, interfaces, *args, **kwargs):
        super(Router, self).__init__(name, **kwargs)

        self.interfaces = interfaces
        
    def config(self, **kwargs):
        super(Host, self).config(**kwargs)
        
        self.cmd('sysctl net.ipv4.ip_forward=1')
        self.cmd('sysctl net.ipv4.conf.all.rp_filter=0')

        for intf, configs in self.interfaces.items():
            self.cmd('ip addr flush dev %s' % intf)
            self.cmd( 'sysctl net.ipv4.conf.%s.rp_filter=0' % intf )
            
            if not isinstance(configs, list):
                configs = [configs]
                
            for attrs in configs:
                # Configure the vlan if there is one    
                if 'vlan' in attrs:
                    vlanName = '%s.%s' % (intf, attrs['vlan'])
                    self.cmd('ip link add link %s name %s type vlan id %s' % 
                             (intf, vlanName, attrs['vlan']))
                    self.cmd('ip link set %s up' % vlanName)
                    addrIntf = vlanName
                else:
                    addrIntf = intf
                    
                # Now configure the addresses on the vlan/native interface
                if 'mac' in attrs:
                    self.cmd('ip link set %s down' % addrIntf)
                    self.cmd('ip link set %s address %s' % (addrIntf, attrs['mac']))
                    self.cmd('ip link set %s up' % addrIntf)
                for addr in attrs['ipAddrs']:
                    self.cmd('ip addr add %s dev %s' % (addr, addrIntf))

class QuaggaRouter(Router):
    
    """Runs Quagga to create a router that can speak routing protocols."""
    
    binDir = '/usr/lib/quagga'
    logDir = '/var/log/quagga'
    
    def __init__(self, name, interfaces,
                 defaultRoute=None,
                 zebraConfFile=None,
                 protocols=[],
                 fpm=None,
                 runDir='/var/run/quagga', *args, **kwargs):
        super(QuaggaRouter, self).__init__(name, interfaces, **kwargs)
        
        self.protocols = protocols
        self.fpm = fpm
        
        for p in self.protocols:
            p.setQuaggaRouter(self)
        
        self.runDir = runDir
        self.defaultRoute = defaultRoute
        
        # Ensure required directories exist
        try:
            original_umask = os.umask(0)
            if (not os.path.isdir(QuaggaRouter.logDir)):
                os.makedirs(QuaggaRouter.logDir, 0777)
            if (not os.path.isdir(self.runDir)):
                os.makedirs(self.runDir, 0777)
        finally:
            os.umask(original_umask)

        self.zebraConfFile = zebraConfFile
        if (self.zebraConfFile is None):
            self.zebraConfFile = '%s/zebrad%s.conf' % (self.runDir, self.name)
            self.generateZebra()
            
        self.socket = '%s/zebra%s.api' % (self.runDir, self.name)
        
        self.zebraPidFile = '%s/zebra%s.pid' % (self.runDir, self.name)

    def generateZebra(self):
        configFile = open(self.zebraConfFile, 'w+')
        configFile.write('log file %s/zebrad%s.log\n' % (QuaggaRouter.logDir, self.name))
        configFile.write('hostname zebra-%s\n' % self.name)
        configFile.write('password %s\n' % 'hello')
        if (self.fpm is not None):
            configFile.write('fpm connection ip %s port 2620' % self.fpm)
        configFile.close()

    def config(self, **kwargs):
        super(QuaggaRouter, self).config(**kwargs)

        self.cmd('%s/zebra -d -f %s -z %s -i %s'
                 % (QuaggaRouter.binDir, self.zebraConfFile, self.socket, self.zebraPidFile))
        
        for p in self.protocols:
            p.config(**kwargs)
        
        if self.defaultRoute:
            self.cmd('ip route add default via %s' % self.defaultRoute)
        
    def terminate(self, **kwargs):
        self.cmd("ps ax | grep '%s' | awk '{print $1}' | xargs kill" 
                 % (self.socket))
        
        for p in self.protocols:
            p.terminate(**kwargs)

        super(QuaggaRouter, self).terminate()
        
class Protocol(object):
    
    """Base abstraction of a protocol that the QuaggaRouter can run."""
        
    def setQuaggaRouter(self, qr):
        self.qr = qr
        
    def config(self, **kwargs):
        pass
    
    def terminate(self, **kwargs):
        pass
        
class BgpProtocol(Protocol):
    
    """Configures and runs the BGP protocol in Quagga."""
    
    def __init__(self, configFile=None, asNum=None, neighbors=[], routes=[], *args, **kwargs):
        self.configFile = configFile
        
        self.asNum = asNum
        self.neighbors = neighbors
        self.routes = routes
            
    def config(self, **kwargs):
        if self.configFile is None:
            self.configFile = '%s/bgpd%s.conf' % (self.qr.runDir, self.qr.name)
            self.generateConfig()
        
        bgpdPidFile = '%s/bgpd%s.pid' % (self.qr.runDir, self.qr.name)
        
        self.qr.cmd('%s/bgpd -d -f %s -z %s -i %s'
                     % (QuaggaRouter.binDir, self.configFile, self.qr.socket, bgpdPidFile))
        
    def generateConfig(self):
        conf = ConfigurationWriter(self.configFile)
                    
        def getRouterId(interfaces):
            intfAttributes = interfaces.itervalues().next()
            print intfAttributes
            if isinstance(intfAttributes, list):
                # Try use the first set of attributes, but if using vlans they might not have addresses
                intfAttributes = intfAttributes[1] if not intfAttributes[0]['ipAddrs'] else intfAttributes[0]
            return intfAttributes['ipAddrs'][0].split('/')[0]
        
        conf.writeLine('log file %s/bgpd%s.log' % (QuaggaRouter.logDir, self.qr.name))
        conf.writeLine('hostname bgp-%s' % self.qr.name);
        conf.writeLine('password %s' % 'sdnip')
        conf.writeLine('!')
        conf.writeLine('router bgp %s' % self.asNum)
        
        conf.indent()
        
        conf.writeLine('bgp router-id %s' % getRouterId(self.qr.interfaces))
        conf.writeLine('timers bgp %s' % '3 9')
        conf.writeLine('!')
        
        for neighbor in self.neighbors:
            conf.writeLine('neighbor %s remote-as %s' % (neighbor['address'], neighbor['as']))
            conf.writeLine('neighbor %s ebgp-multihop' % neighbor['address'])
            conf.writeLine('neighbor %s timers connect %s' % (neighbor['address'], '5'))
            conf.writeLine('neighbor %s advertisement-interval %s' % (neighbor['address'], '5'))
            if 'port' in neighbor:
                conf.writeLine('neighbor %s port %s' % (neighbor['address'], neighbor['port']))
            conf.writeLine('!')
            
        for route in self.routes:
            conf.writeLine('network %s' % route)
        
        conf.close()
    
class OspfProtocol(Protocol):
    
    """Configures and runs the OSPF protocol in Quagga."""
    
    def __init__(self, configFile=None, *args, **kwargs):
        self.configFile = configFile
            
    def config(self, **kwargs):
        if self.configFile is None:
            self.configFile = '%s/ospfd%s.conf' % (self.qr.runDir, self.qr.name)
            self.generateConfig()
        
        ospfPidFile = '%s/ospf%s.pid' % (self.qr.runDir, self.qr.name)
        
        self.qr.cmd('%s/ospfd -d -f %s -z %s -i %s'
                     % (QuaggaRouter.binDir, self.configFile, self.qr.socket, ospfPidFile))
        
    def generateConfig(self):
        conf = ConfigurationWriter(self.configFile)
            
        def getRouterId(interfaces):
            intfAttributes = interfaces.itervalues().next()
            print intfAttributes
            if isinstance(intfAttributes, list):
                # Try use the first set of attributes, but if using vlans they might not have addresses
                intfAttributes = intfAttributes[1] if not intfAttributes[0]['ipAddrs'] else intfAttributes[0]
            return intfAttributes['ipAddrs'][0].split('/')[0]
        
        conf.writeLine('hostname ospf-%s' % self.qr.name);
        conf.writeLine('password %s' % 'hello')
        conf.writeLine('!')
        conf.writeLine('router ospf')
        
        conf.indent()
        
        conf.writeLine('ospf router-id %s' % getRouterId(self.qr.interfaces))
        conf.writeLine('!')
        
        for name, intf in self.qr.interfaces.items():
            for ip in intf['ipAddrs']:
                conf.writeLine('network %s area 0' % ip)
            #if intf['ipAddrs'][0].startswith('192.168'):
            #    writeLine(1, 'passive-interface %s' % name)
            
        conf.close()
        
class PimProtocol(Protocol):
    
    """Configures and runs the PIM protcol in Quagga."""
    
    def __init__(self, configFile=None, *args, **kwargs):
        self.configFile = configFile
        
    def config(self, **kwargs):
        pimPidFile = '%s/pim%s.pid' % (self.qr.runDir, self.qr.name)
                
        self.qr.cmd('%s/pimd -Z -d -f %s -z %s -i %s'
                     % (QuaggaRouter.binDir, self.configFile, self.qr.socket, pimPidFile))
        
class ConfigurationWriter(object):
    
    """Utility class for writing a configuration file."""
    
    def __init__(self, filename):
        self.filename = filename
        self.indentValue = 0;
        
        self.configFile = open(self.filename, 'w+')
    
    def indent(self):
        self.indentValue += 1
        
    def unindent(self):
        if (self.indentValue > 0):
            self.indentValue -= 1
            
    def write(self, string):
        self.configFile.write(string)
    
    def writeLine(self, string):
        intentStr = ''
        for _ in range(0, self.indentValue):
            intentStr += '  '
        self.write('%s%s\n' % (intentStr, string))
        
    def close(self):
        self.configFile.close()

#Backward compatibility for BGP-only use case
class BgpRouter(QuaggaRouter):
    
    """Quagga router running the BGP protocol."""
    
    def __init__(self, name, interfaces,
                 asNum, neighbors, routes=[],
                 defaultRoute=None,
                 quaggaConfFile=None,
                 zebraConfFile=None,
                 *args, **kwargs):
        bgp = BgpProtocol(configFile=quaggaConfFile, asNum=asNum, neighbors=neighbors, routes=routes)
        
        super(BgpRouter, self).__init__(name, interfaces, 
                                        zebraConfFile=zebraConfFile,
                                        defaultRoute=defaultRoute,
                                        protocols=[bgp],
                                        *args, **kwargs)
        
class RouterData(object):
    
    """Internal data structure storing information about a router."""
    
    def __init__(self, index):
        self.index = index;
        self.neighbors = []
        self.interfaces = {}
        self.switches = []
        
    def addNeighbor(self, theirAddress, theirAsNum):
        self.neighbors.append({'address':theirAddress.ip, 'as':theirAsNum})
    
    def addInterface(self, intf, vlan, address):
        if not intf in self.interfaces:
            self.interfaces[intf] = InterfaceData(intf)
            
        self.interfaces[intf].addAddress(vlan, address)
        
    def setSwitch(self, switch):
        self.switches.append(switch)
        
class InterfaceData(object):
    
    """Internal data structure storing information about an interface."""
    
    def __init__(self, number):
        self.number = number
        self.addressesByVlan = {}
        
    def addAddress(self, vlan, address):
        if not vlan in self.addressesByVlan:
            self.addressesByVlan[vlan] = []
            
        self.addressesByVlan[vlan].append(address.with_prefixlen)
        
class RoutedNetwork(object):
    
    """Creates a host behind a router. This is common boilerplate topology
    segment in routed networks."""
    
    @staticmethod
    def build(topology, router, hostName, networks):
        # There's a convention that the router's addresses are already set up,
        # and it has the last address in the network.
        
        def getFirstAddress(network):
            return '%s/%s' % (network[1], network.prefixlen)
        
        defaultRoute = AutonomousSystem.getLastAddress(networks[0]).ip
        
        host = topology.addHost(hostName, cls=RoutedHost,
                                ips=[getFirstAddress(network) for network in networks],
                                gateway=defaultRoute)

        topology.addLink(router, host)

class AutonomousSystem(object):
    
    """Base abstraction of an autonomous system, which implies some internal
    topology and connections to other topology elements (switches/other ASes)."""
    
    psIdx = 1
    
    def __init__(self, asNum, numRouters):
        self.asNum = asNum
        self.numRouters = numRouters
        self.routers = {}
        for i in range(1, numRouters + 1):
            self.routers[i] = RouterData(i)
            
        self.routerNodes={}
            
        self.neighbors=[]
        self.vlanAddresses={}
        
    def peerWith(self, myRouter, myAddress, theirAddress, theirAsNum, intf=1, vlan=None):
        router = self.routers[myRouter]
        
        router.addInterface(intf, vlan, myAddress)
        router.addNeighbor(theirAddress, theirAsNum)

    def getRouter(self, i):
        return self.routerNodes[i]

    @staticmethod
    def generatePeeringAddresses():
        network = ip_network(u'10.0.%s.0/24' % AutonomousSystem.psIdx)
        AutonomousSystem.psIdx += 1
        
        return ip_interface('%s/%s' % (network[1], network.prefixlen)), \
            ip_interface('%s/%s' % (network[2], network.prefixlen))
        
    @staticmethod
    def addPeering(as1, as2, router1=1, router2=1, intf1=1, intf2=1, address1=None, address2=None, useVlans=False):
        vlan = AutonomousSystem.psIdx if useVlans else None
        
        if address1 is None or address2 is None:
            (address1, address2) = AutonomousSystem.generatePeeringAddresses()
            
        as1.peerWith(router1, address1, address2, as2.asNum, intf=intf1, vlan=vlan)
        as2.peerWith(router2, address2, address1, as1.asNum, intf=intf2, vlan=vlan)
    
    @staticmethod
    def getLastAddress(network):
        return ip_interface(network.network_address + network.num_addresses - 2)
    
    @staticmethod
    def getIthAddress(network, i):
        return ip_interface('%s/%s' % (network[i], network.prefixlen))

class BasicAutonomousSystem(AutonomousSystem):

    """Basic autonomous system containing one host and one or more routers
    which peer with other ASes."""

    def __init__(self, num, routes, numRouters=1):
        super(BasicAutonomousSystem, self).__init__(65000+num, numRouters)
        self.num = num
        self.routes = routes
        
    def addLink(self, switch, router=1):
        self.routers[router].setSwitch(switch)

    def build(self, topology):
        self.addRouterAndHost(topology)

    def addRouterAndHost(self, topology):
        
        # TODO implementation is messy and needs to be cleaned up
        
        intfs = {}
        
        router = self.routers[1]
        for i, router in self.routers.items():
        
            #routerName = 'r%i%i' % (self.num, i)
            routerName = 'r%i' % self.num
            if not i==1:
                routerName += ('%i' % i)
                
            hostName = 'h%i' % self.num
        
            for j, interface in router.interfaces.items():
                nativeAddresses = interface.addressesByVlan.pop(None, [])
                peeringIntf = [{'mac' : '00:00:%02x:00:%02x:%02x' % (self.num, i, j),
                               'ipAddrs' : nativeAddresses}]
                
                for vlan, addresses in interface.addressesByVlan.items():
                    peeringIntf.append({'vlan':vlan,
                                        'mac':'00:00:%02x:%02x:%02x:%02x' % (self.num, vlan, i, j),
                                        'ipAddrs':addresses})
                    
                intfs.update({'%s-eth%s' % (routerName, j-1) : peeringIntf})
            
            # Only add the host to the first router for now
            if i==1:
                internalAddresses=[]
                for route in self.routes:
                    internalAddresses.append('%s/%s' % (AutonomousSystem.getLastAddress(route).ip, route.prefixlen))
        
                internalIntf = {'ipAddrs' : internalAddresses}
        
                # This is the configuration of the next interface after all the peering interfaces
                intfs.update({'%s-eth%s' % (routerName, len(router.interfaces.keys())) : internalIntf})
    
            routerNode = topology.addHost(routerName,  
                                  asNum=self.asNum, neighbors=router.neighbors,
                                  routes=self.routes,
                                  cls=BgpRouter, interfaces=intfs)
            
            self.routerNodes[i] = routerNode

            for switch in router.switches:
                topology.addLink(switch, routerNode)

            # Only add the host to the first router for now
            if i==1:
                defaultRoute = internalAddresses[0].split('/')[0]
        
                host = topology.addHost(hostName, cls=RoutedHost,
                                        ips=[self.getFirstAddress(route) for route in self.routes],
                                        gateway=defaultRoute)
        
                topology.addLink(routerNode, host)

    #def getLastAddress(self, network):
    #    return ip_address(network.network_address + network.num_addresses - 2)
    
    def getFirstAddress(self, network):
        return '%s/%s' % (network[1], network.prefixlen)

# TODO fix this AS - doesn't currently work
class RouteServerAutonomousSystem(BasicAutonomousSystem):

    def __init__(self, routerAddress, *args, **kwargs):
        BasicAutonomousSystem.__init__(self, *args, **kwargs)

        self.routerAddress = routerAddress

    def build(self, topology, connectAtSwitch):

        switch = topology.addSwitch('as%isw' % self.num, cls=OVSBridge)

        self.addRouterAndHost(topology, self.routerAddress, switch)

        rsName = 'rs%i' % self.num
        routeServer = topology.addHost(rsName,
                                       self.asnum, self.neighbors,
                                       cls=BgpRouter,
                                       interfaces={'%s-eth0' % rsName : {'ipAddrs':[self.peeringAddress]}})

        topology.addLink(routeServer, switch)
        topology.addLink(switch, connectAtSwitch)
        
class SdnAutonomousSystem(AutonomousSystem):
    
    """Runs the internal BGP speakers needed for ONOS routing apps like
    SDN-IP."""
    
    def __init__(self, onosIps, numBgpSpeakers=1, asNum=65000, externalOnos=True,
                 peerIntfConfig=None, withFpm=False):
        super(SdnAutonomousSystem, self).__init__(asNum, numBgpSpeakers)
        self.onosIps = onosIps
        self.numBgpSpeakers = numBgpSpeakers
        self.peerIntfConfig = peerIntfConfig
        self.withFpm = withFpm
        self.externalOnos= externalOnos
        self.internalPeeringSubnet = ip_network(u'1.1.1.0/24')
        
        for router in self.routers.values():
            # Add iBGP sessions to ONOS nodes
            for onosIp in onosIps:
                router.neighbors.append({'address':onosIp, 'as':asNum, 'port':2000})
                
            # Add iBGP sessions to other BGP speakers
            for i, router2 in self.routers.items():
                if router == router2:
                    continue
                ip = AutonomousSystem.getIthAddress(self.internalPeeringSubnet, 10+i)
                router.neighbors.append({'address':ip.ip, 'as':asNum})
        
    def build(self, topology, connectAtSwitch, controlSwitch):
        
        natIp = AutonomousSystem.getLastAddress(self.internalPeeringSubnet)
        
        for i, router in self.routers.items():
            name = 'bgp%s' % i
            
            ip = AutonomousSystem.getIthAddress(self.internalPeeringSubnet, 10+i)
            eth0 = { 'ipAddrs' : [ str(ip) ] }
            if self.peerIntfConfig is not None:
                eth1 = self.peerIntfConfig
            else:
                nativeAddresses = router.interfaces[1].addressesByVlan.pop(None, [])
                eth1 = [{ 'mac':'00:00:00:00:00:%02x' % i, 
                         'ipAddrs' : nativeAddresses }]
                
                for vlan, addresses in router.interfaces[1].addressesByVlan.items():
                    eth1.append({'vlan':vlan,
                                'mac':'00:00:00:%02x:%02x:00' % (i, vlan),
                                'ipAddrs':addresses})
            
            
            intfs = { '%s-eth0' % name : eth0,
                      '%s-eth1' % name : eth1 }
            
            bgp = topology.addHost( name, cls=BgpRouter, asNum=self.asNum, 
                                    neighbors=router.neighbors,
                                    interfaces=intfs, 
                                    defaultRoute=str(natIp.ip),
                                    fpm=self.onosIps[0] if self.withFpm else None )
            
            topology.addLink( bgp, controlSwitch )
            topology.addLink( bgp, connectAtSwitch )
            
            
        if self.externalOnos:
            nat = topology.addHost('nat', cls=NAT, 
                                   ip='%s/%s' % (natIp.ip, self.internalPeeringSubnet.prefixlen), 
                                   subnet=str(self.internalPeeringSubnet), inNamespace=False);
            topology.addLink(controlSwitch, nat)

        
def generateRoutes(baseRange, numRoutes, subnetSize=None):
    baseNetwork = ip_network(baseRange)
    
    # We need to get at least 2 addresses out of each subnet, so the biggest
    # prefix length we can have is /30
    maxPrefixLength = baseNetwork.max_prefixlen - 2
    
    if subnetSize is not None:
        return list(baseNetwork.subnets(new_prefix=subnetSize))
    
    trySubnetSize = baseNetwork.prefixlen + 1
    while trySubnetSize <= maxPrefixLength and \
            len(list(baseNetwork.subnets(new_prefix=trySubnetSize))) < numRoutes:
        trySubnetSize += 1
        
    if trySubnetSize > maxPrefixLength:
        raise Exception("Can't get enough routes from input parameters")
    
    return list(baseNetwork.subnets(new_prefix=trySubnetSize))[:numRoutes]
    
class RoutingCli( CLI ):
    
    """CLI command that can bring a host up or down. Useful for simulating router failure."""
    
    def do_host( self, line ):
        args = line.split()
        if len(args) != 2:
            error( 'invalid number of args: host <host name> {up, down}\n' )
            return
        
        host = args[ 0 ]
        command = args[ 1 ]
        if host not in self.mn or self.mn.get( host ) not in self.mn.hosts:
            error( 'invalid host: %s\n' % args[ 1 ] )
        else:
            if command == 'up':
                op = 'up'
            elif command == 'down':
                op = 'down'
            else:
                error( 'invalid command: host <host name> {up, down}\n' )
                return

            for intf in self.mn.get( host ).intfList( ):
                intf.link.intf1.ifconfig( op )
                intf.link.intf2.ifconfig( op )
