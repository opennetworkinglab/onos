package org.ctpd.closfwd;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

public class DevicePortTuple {

    public PortNumber inputPort;
    public PortNumber outputPort;
    public DeviceId deviceId;

    public DevicePortTuple(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort)
    {
        this.deviceId = deviceId;
        this.inputPort = inputPort;
        this.outputPort = outputPort;
    }

}
