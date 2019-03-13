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
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Device;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.ConsistentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.store.service.*;

import java.util.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Dictionary;
import java.util.Optional;
import java.util.Properties;
import javax.swing.Timer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

//import org.ctpd.closfwd..*;


public  class EndpointHandlerVpdc implements EndpointHandler{

    public EndpointHandlerVpdc(){

    }

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");
    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public void create(Endpoint endpoint, Boolean create){

        // Create flows in leaf23 devices

        log.debug("create-vpdc-start-"+create);
        ClosDeviceService service = get(ClosDeviceService.class);

        Collection<Versioned<Device>> collectionL2L3DevicesId = service.getL2L3Devices().values();
        Iterator<Versioned<Device>> iteratorL2L3 = collectionL2L3DevicesId.iterator();
        log.debug("Number of L2L3: "+collectionL2L3DevicesId.size());

        while(iteratorL2L3.hasNext()){
            DeviceId deviceId = iteratorL2L3.next().value().id();
            log.debug("DeviceId: "+deviceId.toString());
            service.getDriver().installL2L3Flows(endpoint, deviceId, create);
        }
        log.debug("create-vpdc-end-"+create);

    }

}
