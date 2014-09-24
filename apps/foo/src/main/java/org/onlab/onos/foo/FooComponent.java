package org.onlab.onos.foo;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final ClusterEventListener clusterListener = new InnerClusterListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();

    @Activate
    public void activate() {
        clusterService.addListener(clusterListener);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterService.removeListener(clusterListener);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    private class InnerClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            log.info("WOOOOT! {}", event);
        }
    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            log.info("YEEEEHAAAAW! {}", event);
        }
    }
}


