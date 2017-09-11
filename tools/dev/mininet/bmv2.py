import os
import socket
import re
import json
import urllib2

import time

from mininet.log import info, warn, error
from mininet.node import Switch, Host

if 'ONOS_ROOT' not in os.environ:
    error("ERROR: environment var $ONOS_ROOT not set")
    exit()

BMV2_TARGET = 'simple_switch_grpc'
ONOS_ROOT = os.environ["ONOS_ROOT"]
CPU_PORT = 255
PKT_BYTES_TO_DUMP = 80
VALGRIND_PREFIX = 'valgrind --leak-check=yes'
VALGRIND_SLEEP = 10  # seconds


def parseBoolean(value):
    if value in ['1', 1, 'true', 'True']:
        return True
    else:
        return False


class ONOSHost(Host):
    def __init__(self, name, inNamespace=True, **params):
        Host.__init__(self, name, inNamespace=inNamespace, **params)

    def config(self, **params):
        r = super(Host, self).config(**params)
        for off in ["rx", "tx", "sg"]:
            cmd = "/sbin/ethtool --offload %s %s off" % (self.defaultIntf(), off)
            self.cmd(cmd)
        # disable IPv6
        self.cmd("sysctl -w net.ipv6.conf.all.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.default.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.lo.disable_ipv6=1")
        return r


class ONOSBmv2Switch(Switch):
    """BMv2 software switch with gRPC server"""

    deviceId = 0
    instanceCount = 0

    def __init__(self, name, json=None, debugger=False, loglevel="warn", elogger=False,
                 persistent=False, grpcPort=None, thriftPort=None, netcfg=True, dryrun=False,
                 pipeconfId="", pktdump=False, valgrind=False, **kwargs):
        Switch.__init__(self, name, **kwargs)
        self.grpcPort = ONOSBmv2Switch.pickUnusedPort() if not grpcPort else grpcPort
        self.thriftPort = ONOSBmv2Switch.pickUnusedPort() if not thriftPort else thriftPort
        if self.dpid:
            self.deviceId = int(self.dpid, 0 if 'x' in self.dpid else 16)
        else:
            self.deviceId = ONOSBmv2Switch.deviceId
            ONOSBmv2Switch.deviceId += 1
        self.json = json
        self.debugger = parseBoolean(debugger)
        self.loglevel = loglevel
        self.logfile = '/tmp/bmv2-%d.log' % self.deviceId
        self.elogger = parseBoolean(elogger)
        self.pktdump = parseBoolean(pktdump)
        self.persistent = parseBoolean(persistent)
        self.netcfg = parseBoolean(netcfg)
        self.dryrun = parseBoolean(dryrun)
        self.valgrind = parseBoolean(valgrind)
        self.netcfgfile = '/tmp/bmv2-%d-netcfg.json' % self.deviceId
        self.pipeconfId = pipeconfId
        if persistent:
            self.exectoken = "/tmp/bmv2-%d-exec-token" % self.deviceId
            self.cmd("touch %s" % self.exectoken)
        # Store thrift port for future uses.
        self.cmd("echo %d > /tmp/bmv2-%d-grpc-port" % (self.grpcPort, self.deviceId))

        if 'longitude' in kwargs:
            self.longitude = kwargs['longitude']
        else:
            self.longitude = None

        if 'latitude' in kwargs:
            self.latitude = kwargs['latitude']
        else:
            self.latitude = None

        self.onosDeviceId = "device:bmv2:%d" % self.deviceId

    @classmethod
    def pickUnusedPort(cls):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('localhost', 0))
        addr, port = s.getsockname()
        s.close()
        return port

    def getSourceIp(self, dstIP):
        """
        Queries the Linux routing table to get the source IP that can talk with dstIP, and vice
        versa.
        """
        ipRouteOut = self.cmd('ip route get %s' % dstIP)
        r = re.search(r"src (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})", ipRouteOut)
        return r.group(1) if r else None

    def getDeviceConfig(self, srcIP):
        portData = {}
        portId = 1
        for intfName in self.intfNames():
            if intfName == 'lo':
                continue
            portData[str(portId)] = {
                "number": portId,
                "name": intfName,
                "enabled": True,
                "removed": False,
                "type": "copper",
                "speed": 10000
            }
            portId += 1

        basicCfg = {
            "driver": "bmv2"
        }

        if self.longitude and self.latitude:
            basicCfg["longitude"] = self.longitude
            basicCfg["latitude"] = self.latitude

        cfgData = {
            "generalprovider": {
                "p4runtime": {
                    "ip": srcIP,
                    "port": self.grpcPort,
                    "deviceId": self.deviceId,
                    "deviceKeyId": "p4runtime:%s" % self.onosDeviceId
                }
            },
            "piPipeconf": {
                "piPipeconfId": self.pipeconfId
            },
            "basic": basicCfg,
            "ports": portData
        }

        return cfgData

    def doOnosNetcfg(self, controllerIP):
        """
        Notifies ONOS about the new device via Netcfg.
        """
        srcIP = self.getSourceIp(controllerIP)
        if not srcIP:
            warn("WARN: unable to get device IP address, won't do onos-netcfg")
            return

        cfgData = {
            "devices": {
                self.onosDeviceId: self.getDeviceConfig(srcIP)
            }
        }
        with open(self.netcfgfile, 'w') as fp:
            json.dump(cfgData, fp, indent=4)

        if not self.netcfg:
            # Do not push config to ONOS.
            return

        # Build netcfg URL
        url = 'http://%s:8181/onos/v1/network/configuration/' % controllerIP
        # Instantiate password manager for HTTP auth
        pm = urllib2.HTTPPasswordMgrWithDefaultRealm()
        pm.add_password(None, url, os.environ['ONOS_WEB_USER'], os.environ['ONOS_WEB_PASS'])
        urllib2.install_opener(urllib2.build_opener(urllib2.HTTPBasicAuthHandler(pm)))
        # Push config data to controller
        req = urllib2.Request(url, json.dumps(cfgData), {'Content-Type': 'application/json'})
        try:
            f = urllib2.urlopen(req)
            print f.read()
            f.close()
        except urllib2.URLError as e:
            warn("WARN: unable to push config to ONOS (%s)" % e.reason)

    def start(self, controllers):
        args = [BMV2_TARGET, '--device-id %s' % str(self.deviceId)]
        for port, intf in self.intfs.items():
            if not intf.IP():
                args.append('-i %d@%s' % (port, intf.name))
        if self.elogger:
            nanomsg = 'ipc:///tmp/bmv2-%d-log.ipc' % self.deviceId
            args.append('--nanolog %s' % nanomsg)
        if self.debugger:
            args.append('--debugger')
        args.append('--log-console')
        if self.pktdump:
            args.append('--pcap --dump-packet-data %d' % PKT_BYTES_TO_DUMP)
        args.append('-L%s' % self.loglevel)
        args.append('--thrift-port %d' % self.thriftPort)
        if not self.json:
            args.append('--no-p4')
        else:
            args.append(self.json)

        # gRPC target-specific options.
        args.append('--')
        args.append('--cpu-port %d' % CPU_PORT)
        args.append('--grpc-server-addr 0.0.0.0:%d' % self.grpcPort)

        bmv2cmd = " ".join(args)
        if self.valgrind:
            bmv2cmd = "%s %s" % (VALGRIND_PREFIX, bmv2cmd)
        if self.dryrun:
            info("\n*** DRY RUN (not executing bmv2)")
        info("\nStarting BMv2 target: %s\n" % bmv2cmd)

        if self.persistent:
            # Bash loop to re-exec the switch if it crashes.
            bmv2cmd = "(while [ -e {} ]; do {} ; sleep 1; done;)".format(self.exectoken, bmv2cmd)

        cmdStr = "{} > {} 2>&1 &".format(bmv2cmd, self.logfile)

        # Starts the switch.
        if not self.dryrun:
            out = self.cmd(cmdStr)
            if out:
                print out
            if self.netcfg and self.valgrind:
                # With valgrind, it takes some time before the gRPC server is available.
                # Wait before pushing the netcfg.
                info("\n*** Waiting %d seconds before pushing the config to ONOS...\n" % VALGRIND_SLEEP)
                time.sleep(VALGRIND_SLEEP)

        try:  # onos.py
            clist = controllers[0].nodes()
        except AttributeError:
            clist = controllers
        assert len(clist) > 0
        cip = clist[0].IP()
        self.doOnosNetcfg(cip)

    def stop(self, deleteIntfs=True):
        """Terminate switch."""
        self.cmd("rm -f /tmp/bmv2-%d-*" % self.deviceId)
        self.cmd("rm -f /tmp/bmv2-%d.log*" % self.deviceId)
        self.cmd('kill %' + BMV2_TARGET)
        Switch.stop(self, deleteIntfs)


# Exports for bin/mn
switches = {'onosbmv2': ONOSBmv2Switch}
hosts = {'onoshost': ONOSHost}
