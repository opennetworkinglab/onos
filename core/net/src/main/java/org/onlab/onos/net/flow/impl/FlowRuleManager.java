package org.onlab.onos.net.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRule.FlowRuleState;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

@Component(immediate = true)
@Service
public class FlowRuleManager
extends AbstractProviderRegistry<FlowRuleProvider, FlowRuleProviderService>
implements FlowRuleService, FlowRuleProviderRegistry {

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

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
    public Iterable<FlowRule> getFlowEntries(DeviceId deviceId) {
        return store.getFlowEntries(deviceId);
    }

    @Override
    public List<FlowRule> applyFlowRules(FlowRule... flowRules) {
        List<FlowRule> entries = new ArrayList<FlowRule>();

        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = new DefaultFlowRule(flowRules[i], FlowRuleState.PENDING_ADD);
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
            FlowRule f = new DefaultFlowRule(flowRules[i], FlowRuleState.PENDING_REMOVE);
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

        @Override
        public void pushFlowMetrics(DeviceId deviceId, Iterable<FlowRule> flowEntries) {
            List<FlowRule> storedRules = Lists.newLinkedList(store.getFlowEntries(deviceId));
            List<FlowRule> switchRules = Lists.newLinkedList(flowEntries);
            Iterator<FlowRule> switchRulesIterator = switchRules.iterator();
            List<FlowRule> extraRules = Lists.newLinkedList();

            while (switchRulesIterator.hasNext()) {
                FlowRule rule = switchRulesIterator.next();
                if (storedRules.remove(rule)) {
                    // we both have the rule let's update some info then.
                    log.info("rule {} is added. {}", rule.id(), rule.state());
                    flowAdded(rule);
                } else {
                    // the device a rule the store does not have
                    extraRules.add(rule);
                }
            }
            for (FlowRule rule : storedRules) {
                // there are rules in the store that aren't on the switch
                flowMissing(rule);
            }
            if (extraRules.size() > 0) {
                log.warn("Device {} has extra flow rules: {}", deviceId, extraRules);
                // TODO do something with this.
            }


        }
    }

}
