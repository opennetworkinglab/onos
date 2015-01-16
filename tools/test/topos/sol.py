#!/usr/bin/python
import sys, solar
topo = solar.Solar(cips=sys.argv[1:])
topo.run()
