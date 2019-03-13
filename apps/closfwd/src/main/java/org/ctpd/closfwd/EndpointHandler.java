package org.ctpd.closfwd;

import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.DevicePortTuple;
// import org.ctpd.closfwd..PathData;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.topology.TopologyService;

// import org.onosproject.store.service.ConsistentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public interface EndpointHandler{

    public void create(Endpoint endpoint, Boolean create);

}




