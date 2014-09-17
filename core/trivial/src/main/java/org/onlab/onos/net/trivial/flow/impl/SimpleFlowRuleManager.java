package org.onlab.onos.net.trivial.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@Component(immediate = true)
@Service
public class SimpleFlowRuleManager
extends AbstractProviderRegistry<FlowRuleProvider, FlowRuleProviderService>
implements FlowRuleService, FlowRuleProviderRegistry {

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    private final SimpleFlowRuleStore store = new SimpleFlowRuleStore();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(FlowRuleEvent.class);
        log.info("Stopped");
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        return store.getFlowEntries(deviceId);
    }

    @Override
    public List<FlowEntry> applyFlowRules(FlowRule... flowRules) {
        List<FlowEntry> entries = new ArrayList<FlowEntry>();

        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[i];
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            entries.add(store.storeFlowRule(f));
            frp.applyFlowRule(f);
        }

        return entries;
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[i];
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            store.removeFlowRule(f);
            frp.removeFlowRule(f);
        }

    }

    @Override
    public void addListener(FlowRuleListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(FlowRuleListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    protected FlowRuleProviderService createProviderService(
            FlowRuleProvider provider) {
        return new InternalFlowRuleProviderService(provider);
    }

    private class InternalFlowRuleProviderService
            extends AbstractProviderService<FlowRuleProvider>
            implements FlowRuleProviderService {

        protected InternalFlowRuleProviderService(FlowRuleProvider provider) {
            super(provider);
        }

        @Override
        public void flowRemoved(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            FlowRuleEvent event = store.removeFlowRule(flowRule);

            if (event != null) {
                log.debug("Flow {} removed", flowRule);
                post(event);
            }
        }

        @Override
        public void flowMissing(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            // TODO Auto-generated method stub

        }

        @Override
        public void flowAdded(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();

            FlowRuleEvent event = store.addOrUpdateFlowRule(flowRule);
            if (event == null) {
                log.debug("Flow {} updated", flowRule);
            } else {
                log.debug("Flow {} added", flowRule);
                post(event);
            }
        }

        // Posts the specified event to the local event dispatcher.
        private void post(FlowRuleEvent event) {
            if (event != null) {
                eventDispatcher.post(event);
            }
        }
    }

}
