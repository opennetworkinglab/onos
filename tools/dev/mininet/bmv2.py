import socket

from mininet.log import error, info
from mininet.node import Switch

BMV2_TARGET = 'simple_switch_grpc'

class ONOSBmv2Switch(Switch):
    """BMv2 software switch """

    deviceId = 0
    instanceCount = 0

    def __init__(self, name, debugger=False, loglevel="warn", elogger=False, persistent=False,
                 logflush=False, **kwargs):
        Switch.__init__(self, name, **kwargs)
        self.thriftPort = ONOSBmv2Switch.pickUnusedPort()
        self.grpcPort = ONOSBmv2Switch.pickUnusedPort()
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
        args.append('--no-p4')
        args.append('--')
        args.append('--enable-swap')
        if self.grpcPort:
            args.append('--grpc-server-addr 0.0.0.0:%d' % self.grpcPort)

        bmv2cmd = " ".join(args)
        info("\nStarting BMv2 target: %s\n" % bmv2cmd)
        if self.persistent:
            # Re-exec the switch if it crashes.
            cmdStr = "(while [ -e {} ]; " \
                     "do {} ; " \
                     "sleep 1; " \
                     "done;) &".format(self.exectoken, bmv2cmd)
        else:
            cmdStr = "{} &".format(bmv2cmd)
        self.cmd(cmdStr)

    def stop(self):
        "Terminate switch."
        self.cmd("rm -f /tmp/bmv2-%d-*" % self.deviceId)
        # wildcard end as BMv2 might create log files with .txt extension
        self.cmd("rm -f /tmp/bmv2-%d.log*" % self.deviceId)
        self.cmd('kill %' + BMV2_TARGET)
        self.deleteIntfs()


### Exports for bin/mn
switches = {'onosbmv2': ONOSBmv2Switch}
