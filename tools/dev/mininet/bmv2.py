import os
import socket
import re
import json

from mininet.log import info, warn, error
from mininet.node import Switch

if 'ONOS_ROOT' not in os.environ:
    error("ERROR: environment var $ONOS_ROOT not set")
    exit()

BMV2_TARGET = 'simple_switch_grpc'
ONOS_ROOT = os.environ["ONOS_ROOT"]
INIT_BMV2_JSON = '%s/tools/test/p4src/p4c-out/empty.json' % ONOS_ROOT


class ONOSBmv2Switch(Switch):
    """BMv2 software switch with gRPC server"""

    deviceId = 0
    instanceCount = 0

    def __init__(self, name, debugger=False, loglevel="warn", elogger=False, persistent=False,
                 logflush=False, thriftPort=None, grpcPort=None, netcfg=True, **kwargs):
        Switch.__init__(self, name, **kwargs)
        self.thriftPort = ONOSBmv2Switch.pickUnusedPort() if not thriftPort else thriftPort
        self.grpcPort = ONOSBmv2Switch.pickUnusedPort() if not grpcPort else grpcPort
        if self.dpid:
            self.deviceId = int(self.dpid, 0 if 'x' in self.dpid else 16)
        else:
            self.deviceId = ONOSBmv2Switch.deviceId
            ONOSBmv2Switch.deviceId += 1
        self.debugger = debugger
        self.loglevel = loglevel
        self.logfile = '/tmp/bmv2-%d.log' % self.deviceId
        self.elogger = elogger
        self.persistent = persistent
        self.logflush = logflush
        self.netcfg = netcfg
        self.netcfgfile = '/tmp/bmv2-%d-netcfg.json' % self.deviceId
        if persistent:
            self.exectoken = "/tmp/bmv2-%d-exec-token" % self.deviceId
            self.cmd("touch %s" % self.exectoken)
        # Store thrift port for future uses.
        self.cmd("echo %d > /tmp/bmv2-%d-thrift-port" % (self.thriftPort, self.deviceId))
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
        onosDeviceId = "bmv2:%s:%s#%s" % (srcIP, self.grpcPort, self.deviceId)
        cfgData = {"devices": {
            onosDeviceId: {
                "basic": {
                    "name": "bmv2:%s" % self.deviceId
                }
            }
        }}
        with open(self.netcfgfile, 'w') as fp:
            json.dump(cfgData, fp, indent=4)
        out = self.cmd("%s/tools/test/bin/onos-netcfg %s %s"
                       % (ONOS_ROOT, controllerIP, self.netcfgfile))
        if out:
            print out

    def start(self, controllers):
        args = [BMV2_TARGET, '--device-id %s' % str(self.deviceId)]
        for port, intf in self.intfs.items():
            if not intf.IP():
                args.append('-i %d@%s' % (port, intf.name))
        if self.thriftPort:
            args.append('--thrift-port %d' % self.thriftPort)
        if self.elogger:
            nanomsg = 'ipc:///tmp/bmv2-%d-log.ipc' % self.deviceId
            args.append('--nanolog %s' % nanomsg)
        if self.debugger:
            args.append('--debugger')
        args.append('--log-file %s -L%s' % (self.logfile, self.loglevel))
        if self.logflush:
            args.append('--log-flush')

        args.append(INIT_BMV2_JSON)

        # gRPC target-specific options.
        args.append('--')
        args.append('--enable-swap')
        args.append('--grpc-server-addr 0.0.0.0:%d' % self.grpcPort)

        bmv2cmd = " ".join(args)
        info("\nStarting BMv2 target: %s\n" % bmv2cmd)
        if self.persistent:
            # Bash loop to re-exec the switch if it crashes.
            cmdStr = "(while [ -e {} ]; " \
                     "do {} ; " \
                     "sleep 1; " \
                     "done;) &".format(self.exectoken, bmv2cmd)
        else:
            cmdStr = "{} &".format(bmv2cmd)

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
        # wildcard end as BMv2 might create log files with .txt extension
        self.cmd("rm -f /tmp/bmv2-%d.log*" % self.deviceId)
        self.cmd('kill %' + BMV2_TARGET)
        Switch.stop(self, deleteIntfs)


# Exports for bin/mn
switches = {'onosbmv2': ONOSBmv2Switch}
