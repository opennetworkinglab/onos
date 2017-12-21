#! /usr/bin/env python

import json
import subprocess
import sys
import time

if len(sys.argv) != 2:
    print "usage: check-masters {node}"
    sys.exit(1)

node = sys.argv[1]

def check_masters():
    nodes_json = json.loads(subprocess.check_output(["onos", node, "nodes", "-j"]))
    inactive_nodes = [n['id'] for n in nodes_json if n['state'] != 'READY']

    masters_json = json.loads(subprocess.check_output(["onos", node, "masters", "-j"]))
    masters = dict([(m['id'], m['devices']) for m in masters_json])

    for inactive_node in inactive_nodes:
        if inactive_node in masters:
            devices = masters[inactive_node]
            if len(devices) > 0:
                return False
    return True

for i in range(10):
    if not check_masters():
        time.sleep(1)
    else:
        sys.exit(0)

sys.exit(1)
