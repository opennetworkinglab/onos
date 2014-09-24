package org.onlab.onos.foo;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Playground app component.
 */
@Component(immediate = true)
public class FooComponent {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private ClusterEventListener clusterListener = new InnerClusterListener();

    @Activate
    public void activate() {
        clusterService.addListener(clusterListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterService.removeListener(clusterListener);
        log.info("Stopped");
    }

    private class InnerClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            log.info("WOOOOT! {}", event);
        }
    }
}


