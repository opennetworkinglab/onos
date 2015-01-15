package org.onosproject.ipran;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.netty.Endpoint;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.SncFlowRuleEntry;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.serializers.DecodeTo;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Component(immediate = true)
public class IpranAgent {
    private static final String IPRAN_APP = "org.onosproject.ipran";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    
    private LeadershipEventListener leadershipEventListener =
            new InnerLeadershipEventListener();

    private TopologyListener topologyListener =
            new InnerTopologyEventListener();

    private IpranSessionListener sessionListener =
            new InnerSessionEventListener();

    private ApplicationId appId;
    private ControllerNode localControllerNode;
    private IpranSession ipranConnector;
    // Stores all incoming route updates in a queue.
    private BlockingQueue<Collection<SncFlowRuleEntry>> flowUpdatesQueue;
    private static final int DEFAULT_IPRAN_PORT = 2000;
    private  ExecutorService flowUpdatesExecutor;
    
    @Activate
    protected void activate() {
        log.info("SDN-IP started");

        appId = coreService.registerApplication(IPRAN_APP);

        localControllerNode = clusterService.getLocalNode();
        flowUpdatesQueue = new LinkedBlockingQueue<>();
        ipranConnector = new IpranSession();
        ipranConnector.start();
        topologyService.addListener(topologyListener);
        leadershipService.addListener(leadershipEventListener);
        ipranConnector.addListener(sessionListener);
        leadershipService.runForLeadership(appId.name());
        flowUpdatesExecutor = Executors.newSingleThreadExecutor(
                                            new ThreadFactoryBuilder()
                                            .setNameFormat("ipran-flow-updates-%d").build());
        pocessFlowUpdate();
    }

    private void pocessFlowUpdate() {
        flowUpdatesExecutor.execute(new Runnable() {
            @Override
            public void run() {
                doUpdatesThread();
            }
        });
    }
    private void doUpdatesThread() {
        boolean interrupted = false;
        try {
            while (!interrupted) {
                try {
                    Collection<SncFlowRuleEntry> flowUpdates =
                            flowUpdatesQueue.take();
                    /* here should make some change:
                     * first, use batch service interface
                     * second, add asynchronous transaction
                     */
                    for (SncFlowRuleEntry flow : flowUpdates){
                            flowService.applySncBatch(flow);
                    }
                } catch (InterruptedException e) {
                    log.debug("Interrupted while taking from updates queue", e);
                    interrupted = true;
                } catch (Exception e) {
                    log.debug("exception", e);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
    @Deactivate
    protected void deactivate() {
        ipranConnector.stop();
        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);
        topologyService.removeListener(topologyListener);
        ipranConnector.removeListener(sessionListener);
        flowUpdatesExecutor.shutdown();
        flowUpdatesQueue.clear();
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
            Endpoint host = new Endpoint(localControllerNode.id().toString(), DEFAULT_IPRAN_PORT);
            ListenableFuture<byte[]> responseFuture = ipranConnector
                    .sendAndRecvMsg(host, IpranSession.messageType.TOPO_CHANGED.toString(), null);
            Futures.transform(responseFuture, new DecodeTo<String>((StoreSerializer) IpranSession.SERIALIZER));
            return;
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
    /**
     * A listener for flowRule downstream Events.
     */
    private class InnerSessionEventListener
        implements IpranSessionListener {
        @Override
        public void update(Collection<SncFlowRuleEntry> flowUpdates) {
            // TODO Auto-generated method stub
            try {
                flowUpdatesQueue.put(flowUpdates);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                log.debug("Interrupted while putting on flowUpdatesQueue", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
