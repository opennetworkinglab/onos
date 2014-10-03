package org.onlab.onos.net.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.FlowRuleStore;
import org.onlab.onos.net.flow.FlowRuleStoreDelegate;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import com.google.common.collect.Lists;

/**
 * Provides implementation of the flow NB &amp; SB APIs.
 */
@Component(immediate = true)
@Service
public class FlowRuleManager
extends AbstractProviderRegistry<FlowRuleProvider, FlowRuleProviderService>
implements FlowRuleService, FlowRuleProviderRegistry {

    public static final String FLOW_RULE_NULL = "FlowRule cannot be null";
    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<FlowRuleEvent, FlowRuleListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    private final FlowRuleStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final Map<FlowRule, Long> idleTime = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(FlowRuleEvent.class);
        log.info("Stopped");
    }

    @Override
    public Iterable<FlowRule> getFlowEntries(DeviceId deviceId) {
        return store.getFlowEntries(deviceId);
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        for (int i = 0; i < flowRules.length; i++) {
            FlowRule f = flowRules[i];
            final Device device = deviceService.getDevice(f.deviceId());
            final FlowRuleProvider frp = getProvider(device.providerId());
            idleTime.put(f, System.currentTimeMillis());
            store.storeFlowRule(f);
            frp.applyFlowRule(f);
        }
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        FlowRule f;
        FlowRuleProvider frp;
        Device device;
        for (int i = 0; i < flowRules.length; i++) {
            f = flowRules[i];
            device = deviceService.getDevice(f.deviceId());
            frp = getProvider(device.providerId());
            idleTime.remove(f);
            store.deleteFlowRule(f);
            frp.removeFlowRule(f);
        }
    }

    @Override
    public void removeFlowRulesById(ApplicationId id) {
        Iterable<FlowRule> rules =  getFlowRulesById(id);
        FlowRuleProvider frp;
        Device device;

        for (FlowRule f : rules) {
            store.deleteFlowRule(f);
            device = deviceService.getDevice(f.deviceId());
            frp = getProvider(device.providerId());
            frp.removeRulesById(id, f);
        }
    }

    @Override
    public Iterable<FlowRule> getFlowRulesById(ApplicationId id) {
        return store.getFlowEntriesByAppId(id);
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
            FlowRule stored = store.getFlowRule(flowRule);
            if (stored == null) {
                log.info("Rule already evicted from store: {}", flowRule);
                return;
            }
            Device device = deviceService.getDevice(flowRule.deviceId());
            FlowRuleProvider frp = getProvider(device.providerId());
            FlowRuleEvent event = null;
            switch (stored.state()) {
            case ADDED:
            case PENDING_ADD:
                    frp.applyFlowRule(stored);
                break;
            case PENDING_REMOVE:
            case REMOVED:
                event = store.removeFlowRule(flowRule);
                break;
            default:
                break;

            }
            if (event != null) {
                log.debug("Flow {} removed", flowRule);
                post(event);
            }
        }


        private void flowMissing(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            Device device = deviceService.getDevice(flowRule.deviceId());
            FlowRuleProvider frp = getProvider(device.providerId());
            FlowRuleEvent event = null;
            switch (flowRule.state()) {
            case PENDING_REMOVE:
            case REMOVED:
                event = store.removeFlowRule(flowRule);
                frp.removeFlowRule(flowRule);
                break;
            case ADDED:
            case PENDING_ADD:
                frp.applyFlowRule(flowRule);
                break;
            default:
                log.debug("Flow {} has not been installed.", flowRule);
            }

            if (event != null) {
                log.debug("Flow {} removed", flowRule);
                post(event);
            }

        }


        private void extraneousFlow(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            removeFlowRules(flowRule);
            log.debug("Flow {} is on switch but not in store.", flowRule);
        }


        private void flowAdded(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();

            if (idleTime.containsKey(flowRule) &&
                    checkRuleLiveness(flowRule, store.getFlowRule(flowRule))) {

                FlowRuleEvent event = store.addOrUpdateFlowRule(flowRule);
                if (event == null) {
                    log.debug("No flow store event generated.");
                } else {
                    log.debug("Flow {} {}", flowRule, event.type());
                    post(event);
                }
            } else {
                removeFlowRules(flowRule);
            }

        }

        private boolean checkRuleLiveness(FlowRule swRule, FlowRule storedRule) {
            long timeout = storedRule.timeout() * 1000;
            Long currentTime = System.currentTimeMillis();
            if (storedRule.packets() != swRule.packets()) {
                idleTime.put(swRule, currentTime);
                return true;
            }

            if ((currentTime - idleTime.get(swRule)) <= timeout) {
                idleTime.put(swRule, currentTime);
                return true;
            }
            return false;
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

            Iterator<FlowRule> switchRulesIterator = flowEntries.iterator();

            while (switchRulesIterator.hasNext()) {
                FlowRule rule = switchRulesIterator.next();
                if (storedRules.remove(rule)) {
                    // we both have the rule, let's update some info then.
                    flowAdded(rule);
                } else {
                    // the device has a rule the store does not have
                    extraneousFlow(rule);
                }
            }
            for (FlowRule rule : storedRules) {
                // there are rules in the store that aren't on the switch
                flowMissing(rule);

            }
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleStoreDelegate {
        @Override
        public void notify(FlowRuleEvent event) {
            eventDispatcher.post(event);
        }
    }
}
