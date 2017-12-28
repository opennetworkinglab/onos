import json
import multiprocessing
import os
import random
import re
import socket
import threading
import time
import urllib2
from contextlib import closing
from mininet.log import info, warn
from mininet.node import Switch, Host

SIMPLE_SWITCH_GRPC = 'simple_switch_grpc'
PKT_BYTES_TO_DUMP = 80
VALGRIND_PREFIX = 'valgrind --leak-check=yes'
SWITCH_START_TIMEOUT = 5  # seconds
BMV2_LOG_LINES = 5
BMV2_DEFAULT_DEVICE_ID = 0


def parseBoolean(value):
    if value in ['1', 1, 'true', 'True']:
        return True
    else:
        return False


def pickUnusedPort():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('localhost', 0))
    addr, port = s.getsockname()
    s.close()
    return port


def writeToFile(path, value):
    with open(path, "w") as f:
        f.write(str(value))


def watchDog(sw):
    while True:
        if ONOSBmv2Switch.mininet_exception == 1:
            sw.killBmv2(log=False)
            return
        if sw.stopped:
            return
        with closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as s:
            if s.connect_ex(('127.0.0.1', sw.grpcPort)) == 0:
                time.sleep(1)
            else:
                warn("\n*** WARN: BMv2 instance %s died!\n" % sw.name)
                sw.printBmv2Log()
                print ("-" * 80) + "\n"
                return


class ONOSHost(Host):
    def __init__(self, name, inNamespace=True, **params):
        Host.__init__(self, name, inNamespace=inNamespace, **params)

    def config(self, **params):
        r = super(Host, self).config(**params)
        for off in ["rx", "tx", "sg"]:
            cmd = "/sbin/ethtool --offload %s %s off" \
                  % (self.defaultIntf(), off)
            self.cmd(cmd)
        # disable IPv6
        self.cmd("sysctl -w net.ipv6.conf.all.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.default.disable_ipv6=1")
        self.cmd("sysctl -w net.ipv6.conf.lo.disable_ipv6=1")
        return r


class ONOSBmv2Switch(Switch):
    """BMv2 software switch with gRPC server"""
    # Shared value used to notify to all instances of this class that a Mininet
    # exception occurred. Mininet exception handling doesn't call the stop()
    # method, so the mn process would hang after clean-up since Bmv2 would still
    # be running.
    mininet_exception = multiprocessing.Value('i', 0)

    def __init__(self, name, json=None, debugger=False, loglevel="warn",
                 elogger=False, grpcport=None, cpuport=255,
                 thriftport=None, netcfg=True, dryrun=False, pipeconf="",
                 pktdump=False, valgrind=False, gnmi=False,
                 portcfg=True, onosdevid=None, **kwargs):
        Switch.__init__(self, name, **kwargs)
        self.grpcPort = grpcport
        self.thriftPort = thriftport
        self.cpuPort = cpuport
        self.json = json
        self.debugger = parseBoolean(debugger)
        self.loglevel = loglevel
        # Important: Mininet removes all /tmp/*.log files in case of exceptions.
        # We want to be able to see the bmv2 log if anything goes wrong, hence
        # avoid the .log extension.
        self.logfile = '/tmp/bmv2-%s-log' % self.name
        self.elogger = parseBoolean(elogger)
        self.pktdump = parseBoolean(pktdump)
        self.netcfg = parseBoolean(netcfg)
        self.dryrun = parseBoolean(dryrun)
        self.valgrind = parseBoolean(valgrind)
        self.netcfgfile = '/tmp/bmv2-%s-netcfg.json' % self.name
        self.pipeconfId = pipeconf
        self.injectPorts = parseBoolean(portcfg)
        self.withGnmi = parseBoolean(gnmi)
        self.longitude = kwargs['longitude'] if 'longitude' in kwargs else None
        self.latitude = kwargs['latitude'] if 'latitude' in kwargs else None
        if onosdevid is not None and len(onosdevid) > 0:
            self.onosDeviceId = onosdevid
        else:
            self.onosDeviceId = "device:bmv2:%s" % self.name
        self.logfd = None
        self.bmv2popen = None
        self.stopped = False

        # Remove files from previous executions
        self.cleanupTmpFiles()

    def getSourceIp(self, dstIP):
        """
        Queries the Linux routing table to get the source IP that can talk with
        dstIP, and vice versa.
        """
        ipRouteOut = self.cmd('ip route get %s' % dstIP)
        r = re.search(r"src (\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})", ipRouteOut)
        return r.group(1) if r else None

    def getDeviceConfig(self, srcIP):

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
                    "deviceId": BMV2_DEFAULT_DEVICE_ID,
                    "deviceKeyId": "p4runtime:%s" % self.onosDeviceId
                },
                "bmv2-thrift": {
                    "ip": srcIP,
                    "port": self.thriftPort
                }
            },
            "piPipeconf": {
                "piPipeconfId": self.pipeconfId
            },
            "basic": basicCfg
        }

        if self.withGnmi:
            cfgData["generalprovider"]["gnmi"] = {
                "ip": srcIP,
                "port": self.grpcPort
            }

        if self.injectPorts:
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

            cfgData['ports'] = portData

        return cfgData

    def doOnosNetcfg(self, controllerIP):
        """
        Notifies ONOS about the new device via Netcfg.
        """
        srcIP = self.getSourceIp(controllerIP)
        if not srcIP:
            warn("*** WARN: unable to get switch IP address, won't do netcfg\n")
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
        pm.add_password(None, url,
                        os.environ['ONOS_WEB_USER'],
                        os.environ['ONOS_WEB_PASS'])
        urllib2.install_opener(urllib2.build_opener(
            urllib2.HTTPBasicAuthHandler(pm)))
        # Push config data to controller
        req = urllib2.Request(url, json.dumps(cfgData),
                              {'Content-Type': 'application/json'})
        try:
            f = urllib2.urlopen(req)
            print f.read()
            f.close()
        except urllib2.URLError as e:
            warn("*** WARN: unable to push config to ONOS (%s)\n" % e.reason)

    def start(self, controllers):
        bmv2Args = [SIMPLE_SWITCH_GRPC] + self.grpcTargetArgs()
        if self.valgrind:
            bmv2Args = VALGRIND_PREFIX.split() + bmv2Args

        cmdString = " ".join(bmv2Args)

        if self.dryrun:
            info("\n*** DRY RUN (not executing bmv2)")

        info("\nStarting BMv2 target: %s\n" % cmdString)

        writeToFile("/tmp/bmv2-%s-grpc-port" % self.name, self.grpcPort)
        writeToFile("/tmp/bmv2-%s-thrift-port" % self.name, self.thriftPort)

        try:
            if not self.dryrun:
                # Start the switch
                self.logfd = open(self.logfile, "w")
                self.bmv2popen = self.popen(cmdString,
                                            stdout=self.logfd,
                                            stderr=self.logfd)
                self.waitBmv2Start()
                # We want to be notified if BMv2 dies...
                threading.Thread(target=watchDog, args=[self]).start()

            self.doOnosNetcfg(self.controllerIp(controllers))
        except Exception:
            ONOSBmv2Switch.mininet_exception = 1
            self.killBmv2()
            self.printBmv2Log()
            raise

    def grpcTargetArgs(self):
        if self.grpcPort is None:
            self.grpcPort = pickUnusedPort()
        if self.thriftPort is None:
            self.thriftPort = pickUnusedPort()
        args = ['--device-id %s' % str(BMV2_DEFAULT_DEVICE_ID)]
        for port, intf in self.intfs.items():
            if not intf.IP():
                args.append('-i %d@%s' % (port, intf.name))
        args.append('--thrift-port %s' % self.thriftPort)
        ntfaddr = 'ipc:///tmp/bmv2-%s-notifications.ipc' % self.name
        args.append('--notifications-addr %s' % ntfaddr)
        if self.elogger:
            nanologaddr = 'ipc:///tmp/bmv2-%s-nanolog.ipc' % self.name
            args.append('--nanolog %s' % nanologaddr)
        if self.debugger:
            dbgaddr = 'ipc:///tmp/bmv2-%s-debug.ipc' % self.name
            args.append('--debugger-addr %s' % dbgaddr)
        args.append('--log-console')
        if self.pktdump:
            args.append('--pcap --dump-packet-data %s' % PKT_BYTES_TO_DUMP)
        args.append('-L%s' % self.loglevel)
        if not self.json:
            args.append('--no-p4')
        else:
            args.append(self.json)
        # gRPC target-specific options
        args.append('--')
        args.append('--cpu-port %s' % self.cpuPort)
        args.append('--grpc-server-addr 0.0.0.0:%s' % self.grpcPort)
        return args

    def waitBmv2Start(self):
        # Wait for switch to open gRPC port, before sending ONOS the netcfg.
        # Include time-out just in case something hangs.
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        endtime = time.time() + SWITCH_START_TIMEOUT
        while True:
            result = sock.connect_ex(('127.0.0.1', self.grpcPort))
            if result == 0:
                # The port is open. Let's go! (Close socket first)
                sock.close()
                break
            # Port is not open yet. If there is time, we wait a bit.
            if endtime > time.time():
                time.sleep(0.1)
            else:
                # Time's up.
                raise Exception("Switch did not start before timeout")

    def printBmv2Log(self):
        if os.path.isfile(self.logfile):
            print "-" * 80
            print "%s log (from %s):" % (self.name, self.logfile)
            with open(self.logfile, 'r') as f:
                lines = f.readlines()
                if len(lines) > BMV2_LOG_LINES:
                    print "..."
                for line in lines[-BMV2_LOG_LINES:]:
                    print line.rstrip()

    @staticmethod
    def controllerIp(controllers):
        try:
            # onos.py
            clist = controllers[0].nodes()
        except AttributeError:
            clist = controllers
        assert len(clist) > 0
        return random.choice(clist).IP()

    def killBmv2(self, log=False):
        if self.bmv2popen is not None:
            self.bmv2popen.kill()
        if self.logfd is not None:
            if log:
                self.logfd.write("*** PROCESS TERMINATED BY MININET ***\n")
            self.logfd.close()

    def cleanupTmpFiles(self):
        self.cmd("rm -f /tmp/bmv2-%s-*" % self.name)

    def stop(self, deleteIntfs=True):
        """Terminate switch."""
        self.stopped = True
        self.killBmv2(log=True)
        Switch.stop(self, deleteIntfs)


# Exports for bin/mn
switches = {'onosbmv2': ONOSBmv2Switch}
hosts = {'onoshost': ONOSHost}
