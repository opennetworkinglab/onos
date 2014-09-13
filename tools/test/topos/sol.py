#!/usr/bin/python
import sys, solar
topo = solar.Solar(cip=sys.argv[1])
topo.run()
