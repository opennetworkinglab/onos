package org.onlab.onos.net.trivial.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

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

@Component(immediate = true)
@Service
public class SimpleFlowRuleManager
extends AbstractProviderRegistry<FlowRuleProvider, FlowRuleProviderService>
implements FlowRuleService, FlowRuleProviderRegistry {

    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
    listenerRegistry = new AbstractListenerRegistry<>();

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
        //TODO: store rules somewhere and return them here
        return null;
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[0];
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            //TODO: store rules somewhere
            frp.applyFlowRule(f);
        }


    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[0];
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            //TODO: remove stored rules from wherever they are
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
            // TODO Auto-generated method stub

        }

        @Override
        public void flowMissing(FlowRule flowRule) {
            // TODO Auto-generated method stub

        }

        @Override
        public void flowAdded(FlowRule flowRule) {
            // TODO Auto-generated method stub

        }

    }

}
