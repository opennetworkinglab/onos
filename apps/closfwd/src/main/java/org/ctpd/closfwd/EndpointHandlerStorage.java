package org.ctpd.closfwd;

import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;


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



public  class EndpointHandlerStorage implements EndpointHandler{

    // ClosDeviceService service;

    public EndpointHandlerStorage(){
        // this.service = service;
    }

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");
    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public void create(Endpoint endpoint, Boolean create){

        log.debug("create-internal-service-start-"+create);

        ClosDeviceService service = get(ClosDeviceService.class);

        // Create flows in leaf23 devices

        Collection<Versioned<Device>> collectionL2L3DevicesId = service.getL2L3Devices().values();
        Iterator<Versioned<Device>> iteratorL2L3 = collectionL2L3DevicesId.iterator();
        log.debug("Number of L2L3: "+collectionL2L3DevicesId.size());

        while(iteratorL2L3.hasNext()){
            DeviceId deviceId = iteratorL2L3.next().value().id();
            log.debug("DeviceId: "+deviceId.toString());
            service.getDriver().installL2L3Flows(endpoint, deviceId, create);
        }

        // Create flows in spine devices

        Collection<Versioned<Device>> collectionSpinesId = service.getSpineDevices().values();
        Iterator<Versioned<Device>> iteratorSpines = collectionSpinesId.iterator();
        log.debug("Number of spines: "+collectionSpinesId.size());

        while(iteratorSpines.hasNext()){
            DeviceId deviceId = iteratorSpines.next().value().id();
            log.debug("DeviceId: "+deviceId.toString());
            if(create){
                service.getDriver().installSpineFlows(endpoint, deviceId, create);
            }
            log.debug("spine-flows-created");

        }

        // Create flows in leaf4 devices

        Collection<Versioned<Device>> collectionL4DevicesId = service.getL4Devices().values();
        Iterator<Versioned<Device>> iteratorL4 = collectionL4DevicesId.iterator();
        log.debug("Number of L4: "+collectionL4DevicesId.size());

        while(iteratorL4.hasNext()){
            List<PortNumber> ports = new ArrayList<>();
            DeviceId deviceId = iteratorL4.next().value().id();
            log.debug("DeviceId: "+deviceId.toString());

            service.getDriver().installL4Flows(endpoint, deviceId, create);
        }

        log.debug("create-internal-service-end-"+create);
    }

}
