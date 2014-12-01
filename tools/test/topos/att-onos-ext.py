#!/usr/bin/python

import sys

from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.link import TCLink

from attmplsext import AttMplsTopoExt

setLogLevel( 'info' )

def run(controllers=[ '127.0.0.1' ]):
    net = Mininet( topo=AttMplsTopoExt(), link=TCLink, build=False, autoSetMacs=True )
    ctrl_count = 0
    for controllerIP in controllers:
        net.addController( 'c%d' % ctrl_count, RemoteController, ip=controllerIP )
    net.build()
    net.start()
    CLI( net )
    net.stop()

if __name__ == '__main__':
    if len( sys.argv ) > 1:
        controllers = sys.argv[ 1: ]
    else:
        print 'Usage: att-onos-ext.py <c0 IP> <c1 IP> ...'
        exit( 1 )
    run( controllers )
