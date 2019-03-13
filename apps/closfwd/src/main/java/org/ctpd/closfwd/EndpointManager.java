package org.ctpd.closfwd;

import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.OltEndpoint;
import org.ctpd.closfwd.VoltEndpoint;
import org.ctpd.closfwd.OltControlEndpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.ServiceEndpoint;
import org.ctpd.closfwd.StorageEndpoint;
import org.ctpd.closfwd.ExternalServiceEndpoint;
import org.ctpd.closfwd.VpdcEndpoint;
import org.ctpd.closfwd.VpdcHostEndpoint;
import org.ctpd.closfwd.ClientServiceBypassEndpoint;

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
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.topology.TopologyService;
// import org.onosproject.store.service.ConsistentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ctpd.closfwd.*;


import java.util.*;


public class EndpointManager{

    //persistir
    //decidir que fows necesita handler por cada tipo de endpoint a partir de una interfaz
    //FlowHandler genera flow especifico de cada de driver

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public void installEndpointFlows(Endpoint endpoint, Boolean create){

        if(endpoint instanceof ServiceEndpoint ){
            log.info("[EnpointManager] Creating Internal Service endpoint "+create);
            EndpointHandlerIntService intService = new EndpointHandlerIntService();
            intService.create(endpoint,create);
            log.info("[EnpointManager] Created Internal Service endpoint "+create);

        }

        if(endpoint instanceof ExternalServiceEndpoint){

            log.info("[EnpointManager] Creating External Service endpoint "+create);
            EndpointHandlerExtService extService = new EndpointHandlerExtService();
            extService.create(endpoint,create);
            log.info("[EnpointManager] Created External Service endpoint "+create);

        }

        if(endpoint instanceof StorageEndpoint ){
            log.info("[EnpointManager] Creating Internal Storage endpoint "+create);
            EndpointHandlerStorage storage = new EndpointHandlerStorage();
            storage.create(endpoint,create);
            log.info("[EnpointManager] Created Internal Storage endpoint "+create);

        }

        if(endpoint instanceof OltControlEndpoint){

            log.info("[EnpointManager] Creating Control Olt endpoint "+create);
            EndpointHandlerOltControl  olt = new EndpointHandlerOltControl();
            olt.create(endpoint,create);
            log.info("[EnpointManager] Created Control Olt endpoint "+create);

        }

        if(endpoint instanceof OltEndpoint){
            log.info("[EnpointManager] Creating Olt user endpoint "+create);
            EndpointHandlerOlt  olt = new EndpointHandlerOlt();
            olt.create(endpoint,create);
            log.info("[EnpointManager] Created Olt user endpoint "+create);

        }

        if(endpoint instanceof VoltEndpoint){
            log.info("[EnpointManager] Creating VOlt endpoint "+create);
            EndpointHandlerVolt  olt = new EndpointHandlerVolt();
            olt.create(endpoint,create);
            log.info("[EnpointManager] Created VOlt endpoint "+create);

        }

        if(endpoint instanceof VpdcHostEndpoint ){
            log.info("[EnpointManager] Creating VPDC_HOST endpoint "+create);
            EndpointHandlerVpdchost vpdchost = new EndpointHandlerVpdchost();
            vpdchost.create(endpoint,create);
            log.info("[EnpointManager] Created VPDC_HOST endpoint "+create);

        }

        if(endpoint instanceof VpdcEndpoint ){
            log.info("[EnpointManager] Creating VPDC endpoint "+create);
            EndpointHandlerVpdc vpdc = new EndpointHandlerVpdc();
            vpdc.create(endpoint,create);
            log.info("[EnpointManager] Created VPDC endpoint "+create);

        }

        if(endpoint instanceof ClientServiceBypassEndpoint ){
            log.info("[EnpointManager] Creating Bypass endpoint "+create);
            EndpointHandlerClientServiceBypass bypass = new EndpointHandlerClientServiceBypass();
            bypass.create(endpoint,create);
            log.info("[EnpointManager] Created Bypass endpoint "+create);

        }
    }
}