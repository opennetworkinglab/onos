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
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private final ClusterEventListener clusterListener = new InnerClusterListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final IntentListener intentListener = new InnerIntentListener();

    @Activate
    public void activate() {
        clusterService.addListener(clusterListener);
        deviceService.addListener(deviceListener);
        intentService.addListener(intentListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterService.removeListener(clusterListener);
        deviceService.removeListener(deviceListener);
        intentService.removeListener(intentListener);
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

    private class InnerIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            String message;
            if (event.type() == IntentEvent.Type.SUBMITTED) {
                message = "WOW! It looks like someone has some intentions: {}";
            } else if (event.type() == IntentEvent.Type.INSTALLED) {
                message = "AWESOME! So far things are going great: {}";
            } else if (event.type() == IntentEvent.Type.WITHDRAWN) {
                message = "HMMM! Ambitions are fading apparently: {}";
            } else {
                message = "CRAP!!! Things are not turning out as intended: {}";
            }
            log.info(message, event.subject());
        }
    }
}


