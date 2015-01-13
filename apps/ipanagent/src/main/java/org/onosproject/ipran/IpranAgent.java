package org.onosproject.ipran;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.slf4j.Logger;

@Component(immediate = true)
public class IpranAgent {
    private static final String IPRAN_APP = "org.onosproject.ipran";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;
    
    private LeadershipEventListener leadershipEventListener =
            new InnerLeadershipEventListener();
    private ApplicationId appId;
    private ControllerNode localControllerNode;
    private IpranConnector ipranConnector;

    
    @Activate
    protected void activate() {
        log.info("SDN-IP started");

        appId = coreService.registerApplication(IPRAN_APP);

        localControllerNode = clusterService.getLocalNode();
        ipranConnector = new IpranConnector();
        ipranConnector.start();
        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());
    }

    @Deactivate
    protected void deactivate() {
        ipranConnector.stop();
        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);

        log.info("IPRAN Stopped");
    }
  /**
   * A listener for Topology Events.
   */
  private class InnerTopologyEventListener
    implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            // TODO Auto-generated method stub
            log.debug("Topology Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);
            
        }
    }
    /**
     * A listener for Leadership Events.
     */
    private class InnerLeadershipEventListener
        implements LeadershipEventListener {
        @Override
        public void event(LeadershipEvent event) {
            log.debug("Leadership Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);

            if (!event.subject().topic().equals(appId.name())) {
                return;         // Not our topic: ignore
            }
            if (!event.subject().leader().equals(
                        localControllerNode.id())) {
                return;         // The event is not about this instance: ignore
            }

            switch (event.type()) {
            case LEADER_ELECTED:
                log.info("IPRAN Leader Elected");
                break;
            case LEADER_BOOTED:
                log.info("IPRAN Leader Lost Election");
                break;
            case LEADER_REELECTED:
                break;
            default:
                break;
            }
        }
    }
}
