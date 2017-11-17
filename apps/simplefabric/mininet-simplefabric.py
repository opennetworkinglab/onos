#!/usr/bin/python
# Mininet model for Simple Leaf-Spine Network

# Copyright 2017-present Open Networking Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Host, OVSSwitch, RemoteController
from mininet.log import setLogLevel
from mininet.cli import CLI


"Create custom topo."

net = Mininet()

# Add leaf switch and hosts in rack 1
# subnet: 10.0.1.0/24
s10 = net.addSwitch('s10', dpid='0000000000000011')
h11 = net.addHost('h11', mac='00:00:10:00:01:11', ip='10.0.1.11/24', defaultRoute='via 10.0.1.1')
h12 = net.addHost('h12', mac='00:00:10:00:01:12', ip='10.0.1.12/24', defaultRoute='via 10.0.1.1')
h13 = net.addHost('h13', mac='00:00:10:00:01:13', ip='10.0.1.13/24', defaultRoute='via 10.0.1.1')
h14 = net.addHost('h14', mac='00:00:10:00:01:14', ip='10.0.1.14/24', defaultRoute='via 10.0.1.1')
d11 = net.addHost('d11', mac='00:00:10:00:01:a1', ip='10.0.1.111/24', defaultRoute='via 10.0.1.1')
d12 = net.addHost('d12', mac='00:00:10:00:01:a2', ip='10.0.1.112/24', defaultRoute='via 10.0.1.1')
net.addLink(s10, h11)
net.addLink(s10, h12)
net.addLink(s10, h13)
net.addLink(s10, h14)
net.addLink(s10, d11)
net.addLink(s10, d12)

# Add leaf switch and hosts in rack 2
# subnet: 10.0.2.0/24
s20 = net.addSwitch('s20', dpid='0000000000000012')
h21 = net.addHost('h21', mac='00:00:10:00:02:21', ip='10.0.2.21/24', defaultRoute='via 10.0.2.1')
h22 = net.addHost('h22', mac='00:00:10:00:02:22', ip='10.0.2.22/24', defaultRoute='via 10.0.2.1')
h23 = net.addHost('h23', mac='00:00:10:00:02:23', ip='10.0.2.23/24', defaultRoute='via 10.0.2.1')
h24 = net.addHost('h24', mac='00:00:10:00:02:24', ip='10.0.2.24/24', defaultRoute='via 10.0.2.1')
d21 = net.addHost('d21', mac='00:00:10:00:02:b1', ip='10.0.2.221/24', defaultRoute='via 10.0.2.1')
d22 = net.addHost('d22', mac='00:00:10:00:02:b2', ip='10.0.2.222/24', defaultRoute='via 10.0.2.1')
net.addLink(s20, h21)
net.addLink(s20, h22)
net.addLink(s20, h23)
net.addLink(s20, h24)
net.addLink(s20, d21)
net.addLink(s20, d22)

# Add spine switches and nat
# subnet: 10.0.0.0/16
ss1 = net.addSwitch('ss1', dpid='0000000000000021')
ss2 = net.addSwitch('ss2', dpid='0000000000000022')
net.addLink(ss1, s10)
net.addLink(ss1, s20)
net.addLink(ss2, s10)
net.addLink(ss2, s20)

# Add External Router
#h31 = net.addHost('h31', mac='00:00:10:00:00:31', ip='10.0.0.31/24', defaultRoute='via 10.0.0.1')
#h32 = net.addHost('h32', mac='00:00:10:00:00:32', ip='10.0.0.32/24', defaultRoute='via 10.0.0.1')
#net.addLink(ss1, h31);
#net.addLink(ss2, h32);

# Add ONOS/RemoteController; set your ONOS node ip address
net.addController(RemoteController('c1', ip='10.10.108.140'))

# Main
setLogLevel('info')
net.start()

# reveal hosts to switches
#for h in [h11, h12, h13, h14, d11, d12] :
#  net.ping(hosts=[h, h31], timeout='1')
#  net.ping(hosts=[h, h31], timeout='1')
#for h in [h21, h22, h23, h24, d21, d22] :
#  net.ping(hosts=[h, h32], timeout='1')
#  net.ping(hosts=[h, h32], timeout='1')

# do interactive shell
CLI(net)
net.stop()

