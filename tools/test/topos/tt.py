#!/usr/bin/python
# Launches mininet with Tower topology configuration.
import sys, tower
net = tower.Tower(cip=sys.argv[1])
net.run()
