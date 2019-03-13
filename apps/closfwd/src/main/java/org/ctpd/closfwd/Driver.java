package org.ctpd.closfwd;


import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.Device;
import java.util.List;
import org.ctpd.closfwd.Driver;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Key;

import java.util.Base64;
import java.util.Set;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface Driver {

    public  void installL1Flows(Endpoint endpoint, boolean create);

    public  void installL2L3Flows(Endpoint endpoint, DeviceId deviceId, boolean create);

    public  void installL4Flows(Endpoint endpoint, DeviceId deviceId, boolean create);

    public  void installSpineFlows(Endpoint endpoint,  DeviceId deviceId, boolean create);

    public void installBypassTemporaryFlows(ClientServiceBypassEndpoint endpoint, ServiceEndpoint serviceEndpoint, MacAddress macAddress, IpPrefix ipPrefix, boolean create);

    public  void createIntent(Endpoint ingressEndpoint, Endpoint egressEndpoint, boolean create);

}