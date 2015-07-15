#!/usr/bin/python

import sys

from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.node import RemoteController

from rftesttopo import ReactiveForwardingTestTopo

setLogLevel( 'info' )

def pingloop( net ):
    setLogLevel( 'error' )
    try:
        while True:
            net.ping()
    finally:
        setLogLevel( 'info' )

def run(controllers=[ '127.0.0.1' ]):
    Mininet.pingloop = pingloop
    net = Mininet( topo=ReactiveForwardingTestTopo(), build=False, autoSetMacs=True )
    ctrl_count = 0
    for controllerIP in controllers:
        net.addController( 'c%d' % ctrl_count, RemoteController, ip=controllerIP )
	ctrl_count = ctrl_count + 1
    net.build()
    net.start()
    CLI( net )
    net.stop()

if __name__ == '__main__':
    if len( sys.argv ) > 1:
        controllers = sys.argv[ 1: ]
    else:
        print 'Usage: rf-test.py <c0 IP> <c1 IP> ...'
        exit( 1 )
    run( controllers )
