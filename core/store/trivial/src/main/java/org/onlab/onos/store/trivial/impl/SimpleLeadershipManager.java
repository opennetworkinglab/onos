package org.onlab.onos.store.trivial.impl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.Leadership;
import org.onlab.onos.cluster.LeadershipEvent;
import org.onlab.onos.cluster.LeadershipEvent.Type;
import org.onlab.onos.cluster.LeadershipEventListener;
import org.onlab.onos.cluster.LeadershipService;

/**
 * A trivial implementation of the leadership service.
 * <p></p>
 * The service is not distributed, so it can assume there's a single leadership
 * contender. This contender is always granted leadership whenever it asks.
 */
@Component(immediate = true)
@Service
public class SimpleLeadershipManager implements LeadershipService {

    private Set<LeadershipEventListener> listeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    private Map<String, Boolean> elections = new ConcurrentHashMap<>();

    @Override
    public ControllerNode getLeader(String path) {
        return elections.get(path) ? clusterService.getLocalNode() : null;
    }

    @Override
    public void runForLeadership(String path) {
        elections.put(path, true);
        for (LeadershipEventListener listener : listeners) {
            listener.event(new LeadershipEvent(Type.LEADER_ELECTED,
                    new Leadership(path, clusterService.getLocalNode(), 0)));
        }
    }

    @Override
    public void withdraw(String path) {
        elections.remove(path);
        for (LeadershipEventListener listener : listeners) {
            listener.event(new LeadershipEvent(Type.LEADER_BOOTED,
                    new Leadership(path, clusterService.getLocalNode(), 0)));
        }
    }

    @Override
    public Map<String, Leadership> getLeaderBoard() {
        throw new UnsupportedOperationException("I don't know what to do." +
                                                        " I wish you luck.");
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        listeners.remove(listener);
    }

}
