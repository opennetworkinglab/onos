package org.onlab.onos.optical.provisioner;

import java.util.Iterator;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.CoreService;
import org.onlab.onos.net.ConnectPoint;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceService;

import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;

import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpticalPathProvisioner listens event notifications from the Intent F/W.
 * It generates one or more opticalConnectivityIntent(s) and submits (or withdraws) to Intent F/W
 * for adding/releasing capacity at the packet layer.
 *
 */

@Component(immediate = true)
public class OpticalPathProvisioner {

    protected static final Logger log = LoggerFactory
            .getLogger(OpticalPathProvisioner.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    // protected LinkResourceService resourceService;

    private ApplicationId appId;
    private static long intentId = 0x9000000;

    protected IdGenerator<IntentId> intentIdGenerator;

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onlab.onos.optical");
        log.info("Starting optical path provisoning...");
    }

    @Deactivate
    protected void deactivate() {
        intentService.removeListener(pathProvisioner);
    }

    public class InternalOpticalPathProvisioner implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            switch (event.type()) {
                case SUBMITTED:
                    break;
                case INSTALLED:
                    break;
                case FAILED:
                    log.info("intent {} failed, calling optical path provisioning APP.", event.subject());
                    setuplightpath(event.subject());
                    break;
                case WITHDRAWN:
                    log.info("intent {} withdrawn.", event.subject());
                    teardownLightpath(event.subject());
                    break;
                default:
                    break;
            }
        }

        private void setuplightpath(Intent intent) {
           // TODO: considering user policies and optical reach, may generate more OpticalConnectivityIntents
           if (!intent.equals(PointToPointIntent.class)) {
               return;
           }

           PointToPointIntent pktIntent = (PointToPointIntent) intent;
           if (pktIntent.ingressPoint() == null || pktIntent.egressPoint() == null) {
               return;
           }

           // Set<Port> port1 = deviceService.getPorts(pktIntent.ingressPoint().deviceId());

           Set<Link> srcLink = linkService.getLinks(pktIntent.ingressPoint());
           Set<Link> dstLink = linkService.getLinks(pktIntent.egressPoint());

           if (srcLink.isEmpty() || dstLink.isEmpty()) {
               return;
           }

           Iterator<Link> itrSrc = srcLink.iterator();
           Iterator<Link> itrDst = dstLink.iterator();

           if (!itrSrc.next().annotations().value("linkType").equals("PktOptLink")) {
               return;
           }
           if (!itrDst.next().annotations().value("linkType").equals("PktOptLink")) {
               return;
           }

           ConnectPoint srcWdmPoint = itrSrc.next().dst();
           ConnectPoint dstWdmPoint = itrDst.next().dst();

           OpticalConnectivityIntent opticalIntent =
                    new OpticalConnectivityIntent(new IntentId(intentId++),
                    srcWdmPoint,
                    dstWdmPoint);

           log.info(opticalIntent.toString());
           intentService.submit(opticalIntent);
          }

        private void teardownLightpath(Intent intent) {
          // TODO: tear down the idle lightpath if the utilization is close to zero.
        }

    }

}
