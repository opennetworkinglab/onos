package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.sdnip.RouteUpdate.Type;
import org.onlab.onos.sdnip.bgp.BgpRouteEntry;
import org.onlab.onos.sdnip.bgp.BgpSessionManager;
import org.onlab.onos.sdnip.config.SdnIpConfigReader;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;

/**
 * Component for the SDN-IP peering application.
 */
@Component(immediate = true)
@Service
public class SdnIp implements SdnIpService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private SdnIpConfigReader config;
    private PeerConnectivityManager peerConnectivity;
    private Router router;
    private BgpSessionManager bgpSessionManager;

    @Activate
    protected void activate() {
        log.debug("SDN-IP started");

        config = new SdnIpConfigReader();
        config.init();

        InterfaceService interfaceService = new HostServiceBasedInterfaceService(hostService);

        peerConnectivity = new PeerConnectivityManager(config, interfaceService, intentService);
        peerConnectivity.start();

        router = new Router(intentService, hostService, config, interfaceService);
        router.start();

        bgpSessionManager = new BgpSessionManager(router);
        bgpSessionManager.startUp(2000); // TODO

        // TODO need to disable link discovery on external ports

        router.update(new RouteUpdate(Type.UPDATE, new RouteEntry(
                IpPrefix.valueOf("172.16.20.0/24"),
                IpAddress.valueOf("192.168.10.1"))));
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Collection<BgpRouteEntry> getBgpRoutes() {
        return bgpSessionManager.getBgpRoutes();
    }

    @Override
    public Collection<RouteEntry> getRoutes() {
        return router.getRoutes();
    }

    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
