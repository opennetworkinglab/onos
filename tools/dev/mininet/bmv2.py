import os
import socket
import re
import json
import urllib2

from mininet.log import info, warn, error
from mininet.node import Switch

if 'ONOS_ROOT' not in os.environ:
    error("ERROR: environment var $ONOS_ROOT not set")
    exit()

BMV2_TARGET = 'simple_switch_grpc'
ONOS_ROOT = os.environ["ONOS_ROOT"]
CPU_PORT = 255


class ONOSBmv2Switch(Switch):
    """BMv2 software switch with gRPC server"""

    deviceId = 0
    instanceCount = 0

    def __init__(self, name, json=None, debugger=False, loglevel="warn", elogger=False,
                 persistent=False, grpcPort=None, netcfg=True, **kwargs):
        Switch.__init__(self, name, **kwargs)
        self.grpcPort = ONOSBmv2Switch.pickUnusedPort() if not grpcPort else grpcPort
        if self.dpid:
            self.deviceId = int(self.dpid, 0 if 'x' in self.dpid else 16)
        else:
            self.deviceId = ONOSBmv2Switch.deviceId
            ONOSBmv2Switch.deviceId += 1
        self.json = json
        self.debugger = debugger
        self.loglevel = loglevel
        self.logfile = '/tmp/bmv2-%d.log' % self.deviceId
        self.elogger = elogger
        self.persistent = persistent
        self.netcfg = netcfg
        self.netcfgfile = '/tmp/bmv2-%d-netcfg.json' % self.deviceId
        if persistent:
            self.exectoken = "/tmp/bmv2-%d-exec-token" % self.deviceId
            self.cmd("touch %s" % self.exectoken)
        # Store thrift port for future uses.
        self.cmd("echo %d > /tmp/bmv2-%d-grpc-port" % (self.grpcPort, self.deviceId))

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

    def doOnosNetcfg(self, controllerIP):
        """
        Notifies ONOS about the new device via Netcfg.
        """
        srcIP = self.getSourceIp(controllerIP)
        if not srcIP:
            warn("WARN: unable to get device IP address, won't do onos-netcfg")
            return
        onosDeviceId = "bmv2:%s" % self.deviceId
        cfgData = {
            "devices": {
                "device:%s" % onosDeviceId: {
                    "generalprovider": {
                        "p4runtime": {
                            "ip": srcIP,
                            "port": self.grpcPort,
                            "deviceId": self.deviceId,
                            "deviceKeyId": "p4runtime:%s" % onosDeviceId
                        }
                    },
                    "piPipeconf": {
                        "piPipeconfId": ""
                    },
                    "basic": {
                        "driver": "bmv2"
                    }
                }
            }
        }
        with open(self.netcfgfile, 'w') as fp:
            json.dump(cfgData, fp, indent=4)
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
        args.append('-L%s' % self.loglevel)
        if not self.json:
            args.append('--no-p4')
        else:
            args.append(self.json)

        # gRPC target-specific options.
        args.append('--')
        args.append('--cpu-port %d' % CPU_PORT)
        args.append('--grpc-server-addr 0.0.0.0:%d' % self.grpcPort)

        bmv2cmd = " ".join(args)
        info("\nStarting BMv2 target: %s\n" % bmv2cmd)

        if self.persistent:
            # Bash loop to re-exec the switch if it crashes.
            bmv2cmd = "(while [ -e {} ]; do {} ; sleep 1; done;)".format(self.exectoken, bmv2cmd)

        cmdStr = "{} > {} 2>&1 &".format(bmv2cmd, self.logfile)

        # Starts the switch.
        out = self.cmd(cmdStr)
        if out:
            print out

        if self.netcfg:
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
