/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.flow.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.onlab.util.PredictableExecutor;
import org.onlab.util.PredictableExecutor.PickyRunnable;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleStore;
import org.onosproject.net.flow.FlowRuleStoreDelegate;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEvent;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchRequest;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.ALLOW_EXTRANEOUS_RULES;
import static org.onosproject.net.OsgiPropertyConstants.ALLOW_EXTRANEOUS_RULES_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.IMPORT_EXTRANEOUS_RULES;
import static org.onosproject.net.OsgiPropertyConstants.IMPORT_EXTRANEOUS_RULES_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.POLL_FREQUENCY;
import static org.onosproject.net.OsgiPropertyConstants.POLL_FREQUENCY_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.PURGE_ON_DISCONNECTION;
import static org.onosproject.net.OsgiPropertyConstants.PURGE_ON_DISCONNECTION_DEFAULT;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_ADD_REQUESTED;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVE_REQUESTED;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.FLOWRULE_READ;
import static org.onosproject.security.AppPermission.Type.FLOWRULE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the flow NB &amp; SB APIs.
 */
@Component(
        immediate = true,
        service = {
                FlowRuleService.class,
                FlowRuleProviderRegistry.class
        },
        property = {
                ALLOW_EXTRANEOUS_RULES + ":Boolean=" + ALLOW_EXTRANEOUS_RULES_DEFAULT,
                IMPORT_EXTRANEOUS_RULES + ":Boolean=" + IMPORT_EXTRANEOUS_RULES_DEFAULT,
                PURGE_ON_DISCONNECTION + ":Boolean=" + PURGE_ON_DISCONNECTION_DEFAULT,
                POLL_FREQUENCY + ":Integer=" + POLL_FREQUENCY_DEFAULT
        }
)
public class FlowRuleManager
        extends AbstractListenerProviderRegistry<FlowRuleEvent, FlowRuleListener,
        FlowRuleProvider, FlowRuleProviderService>
        implements FlowRuleService, FlowRuleProviderRegistry {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String FLOW_RULE_NULL = "FlowRule cannot be null";

    /** Allow flow rules in switch not installed by ONOS. */
    private boolean allowExtraneousRules = ALLOW_EXTRANEOUS_RULES_DEFAULT;

    /** Allow to import flow rules in switch not installed by ONOS. */
    private boolean importExtraneousRules = IMPORT_EXTRANEOUS_RULES_DEFAULT;

    /** Purge entries associated with a device when the device goes offline. */
    private boolean purgeOnDisconnection = PURGE_ON_DISCONNECTION_DEFAULT;

    /** Frequency (in seconds) for polling flow statistics via fallback provider. */
    private int fallbackFlowPollFrequency = POLL_FREQUENCY_DEFAULT;

    private final FlowRuleStoreDelegate delegate = new InternalStoreDelegate();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    private final FlowRuleDriverProvider driverProvider = new FlowRuleDriverProvider();

    protected ExecutorService deviceInstallers = Executors.newFixedThreadPool(32,
            groupedThreads("onos/flowservice", "device-installer-%d", log));

    protected ExecutorService operationsService = new PredictableExecutor(32,
            groupedThreads("onos/flowservice", "operations-%d", log));

    private IdGenerator idGenerator;

    private final Map<Long, FlowOperationsProcessor> pendingFlowOperations = new ConcurrentHashMap<>();

    private NodeId local;

    private Random randomGenerator = new Random();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netCfgService;

    @Activate
    public void activate(ComponentContext context) {
        store.setDelegate(delegate);
        eventDispatcher.addSink(FlowRuleEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        cfgService.registerProperties(getClass());
        modified(context);
        idGenerator = coreService.getIdGenerator(FLOW_OP_TOPIC);
        local = clusterService.getLocalNode().id();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        driverProvider.terminate();
        deviceService.removeListener(deviceListener);
        cfgService.unregisterProperties(getClass(), false);
        deviceInstallers.shutdownNow();
        operationsService.shutdownNow();
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(FlowRuleEvent.class);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context != null) {
            readComponentConfiguration(context);
        }
        driverProvider.init(new InternalFlowRuleProviderService(driverProvider),
                            deviceService, mastershipService, fallbackFlowPollFrequency);
    }

    @Override
    protected FlowRuleProvider defaultProvider() {
        return driverProvider;
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, ALLOW_EXTRANEOUS_RULES);
        if (flag == null) {
            log.info("AllowExtraneousRules is not configured, " +
                             "using current value of {}", allowExtraneousRules);
        } else {
            allowExtraneousRules = flag;
            log.info("Configured. AllowExtraneousRules is {}",
                     allowExtraneousRules ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, IMPORT_EXTRANEOUS_RULES);
        if (flag == null) {
            log.info("ImportExtraneousRules is not configured, " +
                             "using current value of {}", importExtraneousRules);
        } else {
            importExtraneousRules = flag;
            log.info("Configured. importExtraneousRules is {}",
                     importExtraneousRules ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, PURGE_ON_DISCONNECTION);
        if (flag == null) {
            log.info("PurgeOnDisconnection is not configured, " +
                             "using current value of {}", purgeOnDisconnection);
        } else {
            purgeOnDisconnection = flag;
            log.info("Configured. PurgeOnDisconnection is {}",
                     purgeOnDisconnection ? "enabled" : "disabled");
        }

        String s = get(properties, POLL_FREQUENCY);
        if (isNullOrEmpty(s)) {
            log.info("fallbackFlowPollFrequency is not configured, " +
                             "using current value of {} seconds",
                     fallbackFlowPollFrequency);
        } else {
            try {
                fallbackFlowPollFrequency = Integer.parseInt(s);
                log.info("Configured. FallbackFlowPollFrequency is {} seconds",
                         fallbackFlowPollFrequency);
            } catch (NumberFormatException e) {
                log.warn("Configured fallbackFlowPollFrequency value " +
                                 "is not a number, using current value of {} seconds",
                         fallbackFlowPollFrequency);
            }
        }
    }

    @Override
    public int getFlowRuleCount() {
        checkPermission(FLOWRULE_READ);
        return store.getFlowRuleCount();
    }

    @Override
    public int getFlowRuleCount(DeviceId deviceId) {
        checkPermission(FLOWRULE_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getFlowRuleCount(deviceId);
    }

    @Override
    public int getFlowRuleCount(DeviceId deviceId, FlowEntry.FlowEntryState state) {
        checkPermission(FLOWRULE_READ);
        checkNotNull(deviceId, "Device ID cannot be null");
        return store.getFlowRuleCount(deviceId, state);
    }

    @Override
    public FlowEntry getFlowEntry(FlowRule rule) {
        checkPermission(FLOWRULE_READ);
        checkNotNull(rule, FLOW_RULE_NULL);
        return store.getFlowEntry(rule);
    }

    @Override
    public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
        checkPermission(FLOWRULE_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getFlowEntries(deviceId);
    }

    @Override
    public void applyFlowRules(FlowRule... flowRules) {
        checkPermission(FLOWRULE_WRITE);

        apply(buildFlowRuleOperations(true, null, flowRules));
    }

    @Override
    public void purgeFlowRules(DeviceId deviceId) {
        checkPermission(FLOWRULE_WRITE);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        store.purgeFlowRule(deviceId);
    }

    @Override
    public void purgeFlowRules(DeviceId deviceId, ApplicationId appId) {
        checkPermission(FLOWRULE_WRITE);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(appId, "Application ID cannot be null!");

        store.purgeFlowRules(deviceId, appId);
    }

    @Override
    public void removeFlowRules(FlowRule... flowRules) {
        checkPermission(FLOWRULE_WRITE);

        apply(buildFlowRuleOperations(false, null, flowRules));
    }

    @Override
    public void removeFlowRulesById(ApplicationId id) {
        checkPermission(FLOWRULE_WRITE);

        Set<FlowRule> flowEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(d.id())) {
                if (flowEntry.appId() == id.id()) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        removeFlowRules(Iterables.toArray(flowEntries, FlowRule.class));
    }

    @Override
    public Iterable<FlowEntry> getFlowEntriesById(ApplicationId id) {
        checkPermission(FLOWRULE_READ);

        Set<FlowEntry> flowEntries = Sets.newHashSet();
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(d.id())) {
                if (flowEntry.appId() == id.id()) {
                    flowEntries.add(flowEntry);
                }
            }
        }
        return flowEntries;
    }

    @Override
    public Iterable<FlowRule> getFlowRulesByGroupId(ApplicationId appId, short groupId) {
        checkPermission(FLOWRULE_READ);

        Set<FlowRule> matches = Sets.newHashSet();
        long toLookUp = ((long) appId.id() << 16) | groupId;
        for (Device d : deviceService.getDevices()) {
            for (FlowEntry flowEntry : store.getFlowEntries(d.id())) {
                if ((flowEntry.id().value() >>> 32) == toLookUp) {
                    matches.add(flowEntry);
                }
            }
        }
        return matches;
    }

    @Override
    public void apply(FlowRuleOperations ops) {
        checkPermission(FLOWRULE_WRITE);
        if (ops.stripeKey().isEmpty()) {
            // Null means that we don't care about the in-order processing
            // this approach maximizes the throughput but it can introduce
            // consistency issues as the original order between conflictual
            // writes is not maintained. If conflictual writes can be easily
            // handled using different stages, this is the approach to use.
            operationsService.execute(new FlowOperationsProcessor(ops));
        } else {
            // Following approach is suggested when it is hard to handle
            // conflictual writes in the same FlowRuleOperations object. Apps
            // may know there are conflictual writes but it could be hard to
            // encapsulate them in the same object using different stages (above
            // all if they are stimulated by different events). In this case,
            // the probabilistic accumulation may help but it is brittle and based
            // on the probability that a given event happens in a specific time.
            // For this reason we have introduced PredictableFlowOperationsProcessor
            // which uses the striped key (provided by the apps) to serialize the ops
            // on the same executor.
            operationsService.execute(new PredictableFlowOperationsProcessor(ops));
        }
    }

    @Override
    protected FlowRuleProviderService createProviderService(
            FlowRuleProvider provider) {
        return new InternalFlowRuleProviderService(provider);
    }

    @Override
    protected synchronized FlowRuleProvider getProvider(ProviderId pid) {
        log.warn("should not be calling getProvider(ProviderId)");
        return super.getProvider(pid);
    }

    /**
     * {@inheritDoc}
     * if the Device does not support {@link FlowRuleProgrammable}.
     */
    @Override
    protected synchronized FlowRuleProvider getProvider(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        // if device supports FlowRuleProgrammable,
        // use FlowRuleProgrammable via FlowRuleDriverProvider
        return Optional.ofNullable(deviceService.getDevice(deviceId))
                .filter(dev -> dev.is(FlowRuleProgrammable.class))
                .<FlowRuleProvider>map(x -> driverProvider)
                .orElseGet(() -> super.getProvider(deviceId));
    }

    private class InternalFlowRuleProviderService
            extends AbstractProviderService<FlowRuleProvider>
            implements FlowRuleProviderService {

        final Map<FlowEntry, Long> firstSeen = Maps.newConcurrentMap();
        final Map<FlowEntry, Long> lastSeen = Maps.newConcurrentMap();


        protected InternalFlowRuleProviderService(FlowRuleProvider provider) {
            super(provider);
        }

        @Override
        public void flowRemoved(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);
            checkValidity();
            lastSeen.remove(flowEntry);
            firstSeen.remove(flowEntry);
            FlowEntry stored = store.getFlowEntry(flowEntry);
            if (stored == null) {
                log.debug("Rule already evicted from store: {}", flowEntry);
                return;
            }
            if (flowEntry.reason() == FlowEntry.FlowRemoveReason.HARD_TIMEOUT) {
                ((DefaultFlowEntry) stored).setState(FlowEntry.FlowEntryState.REMOVED);
            }
            FlowRuleProvider frp = getProvider(flowEntry.deviceId());
            FlowRuleEvent event = null;
            switch (stored.state()) {
                case ADDED:
                case PENDING_ADD:
                    frp.applyFlowRule(stored);
                    break;
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(stored);
                    break;
                default:
                    break;

            }
            if (event != null) {
                log.debug("Flow {} removed", flowEntry);
                post(event);
            }
        }


        private void flowMissing(FlowEntry flowRule, boolean isFlowOnlyInStore) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            FlowRuleProvider frp = getProvider(flowRule.deviceId());
            FlowRuleEvent event = null;
            switch (flowRule.state()) {
                case PENDING_REMOVE:
                case REMOVED:
                    event = store.removeFlowRule(flowRule);
                    log.debug("Flow {} removed", flowRule);
                    break;
                case ADDED:
                case PENDING_ADD:
                    event = store.pendingFlowRule(flowRule);
                    if (isFlowOnlyInStore) {
                        // Publishing RULE_ADD_REQUESTED event facilitates
                        // preparation of statistics for the concerned rule
                        if (event == null) {
                            event = new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADD_REQUESTED, flowRule);
                        }
                    }
                    try {
                        frp.applyFlowRule(flowRule);
                    } catch (UnsupportedOperationException e) {
                        log.warn("Unsupported operation", e);
                        if (flowRule instanceof DefaultFlowEntry) {
                            //FIXME modification of "stored" flow entry outside of store
                            ((DefaultFlowEntry) flowRule).setState(FlowEntry.FlowEntryState.FAILED);
                        }
                    }
                    break;
                default:
                    log.debug("Flow {} has not been installed.", flowRule);
            }

            if (event != null) {
                post(event);
            }
        }

        private void extraneousFlow(FlowRule flowRule) {
            checkNotNull(flowRule, FLOW_RULE_NULL);
            checkValidity();
            // getProvider is customized to favor driverProvider
            FlowRuleProvider frp = getProvider(flowRule.deviceId());
            frp.removeFlowRule(flowRule);
            log.debug("Flow {} is on switch but not in store.", flowRule);
        }

        private boolean handleExistingFlow(FlowEntry flowEntry) {
            checkNotNull(flowEntry, FLOW_RULE_NULL);
            checkValidity();
            FlowEntry storedEntry = store.getFlowEntry(flowEntry);
            if (storedEntry != null) {
                // Flow rule is still valid, let's try to update the stats
                if (storedEntry.state() != FlowEntry.FlowEntryState.PENDING_REMOVE &&
                        checkRuleLiveness(flowEntry, storedEntry)) {
                    if (!shouldHandle(flowEntry.deviceId())) {
                        return false;
                    }
                    FlowRuleEvent event = store.addOrUpdateFlowRule(flowEntry);
                    // Something went wrong or there is no master or the device
                    // is not available better check if it is the latter cases
                    if (event == null) {
                        log.debug("No flow store event generated for addOrUpdate of {}", flowEntry);
                        return false;
                    } else {
                        log.trace("Flow {} {}", flowEntry, event.type());
                        post(event);
                    }
                } else if (storedEntry.state() == FlowEntry.FlowEntryState.PENDING_REMOVE) {
                    // Store is already in sync, let's re-issue flow removal only
                    log.debug("Removing {} from the device", flowEntry);
                    FlowRuleProvider frp = getProvider(flowEntry.deviceId());
                    frp.removeFlowRule(flowEntry);
                } else if (!checkRuleLiveness(flowEntry, storedEntry)) {
                    // Update store first as the flow entry is expired. Then,
                    // as consequence of this a flow removal will be sent.
                    log.debug("Removing {}", flowEntry);
                    removeFlowRules(flowEntry);
                }
            } else {
                // It was already removed or there is no master
                // better check if it is the latter
                return false;
            }
            return true;
        }

        private boolean checkRuleLiveness(FlowEntry swRule, FlowEntry storedRule) {
            if (storedRule == null) {
                return false;
            }
            if (storedRule.isPermanent()) {
                return true;
            }

            final long timeout = storedRule.timeout() * 1000L;
            final long currentTime = System.currentTimeMillis();

            // Checking flow with hardTimeout
            if (storedRule.hardTimeout() != 0) {
                if (!firstSeen.containsKey(storedRule)) {
                    // First time rule adding
                    firstSeen.put(storedRule, currentTime);
                } else {
                    Long first = firstSeen.get(storedRule);
                    final long hardTimeout = storedRule.hardTimeout() * 1000L;
                    if ((currentTime - first) > hardTimeout) {
                        return false;
                    }
                }
            }

            if (storedRule.packets() != swRule.packets() || storedRule.bytes() != swRule.bytes()) {
                lastSeen.put(storedRule, currentTime);
                return true;
            }
            if (!lastSeen.containsKey(storedRule)) {
                // checking for the first time
                lastSeen.put(storedRule, storedRule.lastSeen());
                // Use following if lastSeen attr. was removed.
                //lastSeen.put(storedRule, currentTime);
            }
            Long last = lastSeen.get(storedRule);

            // concurrently removed? let the liveness check fail
            return last != null && (currentTime - last) <= timeout;
        }

        @Override
        public void pushFlowMetrics(DeviceId deviceId, Iterable<FlowEntry> flowEntries) {
            pushFlowMetricsInternal(deviceId, flowEntries, true);
        }

        @Override
        public void pushFlowMetricsWithoutFlowMissing(DeviceId deviceId, Iterable<FlowEntry> flowEntries) {
            pushFlowMetricsInternal(deviceId, flowEntries, false);
        }

        private boolean shouldHandle(DeviceId deviceId) {
            NodeId master = mastershipService.getMasterFor(deviceId);
            return Objects.equals(local, master) && deviceService.isAvailable(deviceId);
        }

        private void pushFlowMetricsInternal(DeviceId deviceId, Iterable<FlowEntry> flowEntries,
                                             boolean useMissingFlow) {
            Map<FlowEntry, FlowEntry> storedRules = Maps.newHashMap();
            store.getFlowEntries(deviceId).forEach(f -> storedRules.put(f, f));
            NodeId master;
            boolean done;

            // Processing flow rules
            for (FlowEntry rule : flowEntries) {
                try {
                    FlowEntry storedRule = storedRules.remove(rule);
                    if (storedRule != null) {
                        if (storedRule.exactMatch(rule)) {
                            // we both have the rule, let's update some info then.
                            done = handleExistingFlow(rule);
                            if (!done) {
                                // Mastership change can occur during this iteration
                                if (!shouldHandle(deviceId)) {
                                    log.warn("Tried to update the flow stats while the node was not the master" +
                                            " or the device {} was not available", deviceId);
                                    return;
                                }
                            }
                        } else {
                            // Mastership change can occur during this iteration
                            if (!shouldHandle(deviceId)) {
                                log.warn("Tried to update the flows while the node was not the master" +
                                        " or the device {} was not available", deviceId);
                                return;
                            }
                            // the two rules are not an exact match - remove the
                            // switch's rule and install our rule
                            extraneousFlow(rule);
                            flowMissing(storedRule, false);
                        }
                    } else {
                        // the device has a rule the store does not have
                        if (!allowExtraneousRules) {
                            // Mastership change can occur during this iteration
                            if (!shouldHandle(deviceId)) {
                                log.warn("Tried to remove flows while the node was not the master" +
                                        " or the device {} was not available", deviceId);
                                return;
                            }
                            extraneousFlow(rule);
                        } else if (importExtraneousRules) { // Stores the rule, if so is indicated
                            FlowRuleEvent flowRuleEvent = store.addOrUpdateFlowRule(rule);
                            if (flowRuleEvent == null) {
                                // Mastership change can occur during this iteration
                                if (!shouldHandle(deviceId)) {
                                    log.warn("Tried to import flows while the node was not the master" +
                                            " or the device {} was not available", deviceId);
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Can't process added or extra rule {} for device {}:{}",
                             rule, deviceId, e);
                }
            }

            // DO NOT reinstall
            if (useMissingFlow) {
                for (FlowEntry rule : storedRules.keySet()) {
                    // Mastership change can occur during this iteration
                    if (!shouldHandle(deviceId)) {
                        log.warn("Tried to install missing rules while the node was not the master" +
                                " or the device {} was not available", deviceId);
                        return;
                    }
                    try {
                        // there are rules in the store that aren't on the switch
                        log.debug("Adding the rule that is present in store but not on switch : {}", rule);
                        flowMissing(rule, true);
                    } catch (Exception e) {
                        log.warn("Can't add missing flow rule:", e);
                    }
                }
            }
        }

        @Override
        public void batchOperationCompleted(long batchId, CompletedBatchOperation operation) {
            store.batchOperationComplete(FlowRuleBatchEvent.completed(
                    new FlowRuleBatchRequest(batchId, Collections.emptySet()),
                    operation
            ));
        }

        @Override
        public void pushTableStatistics(DeviceId deviceId,
                                        List<TableStatisticsEntry> tableStats) {
            store.updateTableStatistics(deviceId, tableStats);
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements FlowRuleStoreDelegate {


        // TODO: Right now we only dispatch events at individual flowEntry level.
        // It may be more efficient for also dispatch events as a batch.
        @Override
        public void notify(FlowRuleBatchEvent event) {
            final FlowRuleBatchRequest request = event.subject();
            switch (event.type()) {
                case BATCH_OPERATION_REQUESTED:
                    // Request has been forwarded to MASTER Node, and was
                    request.ops().forEach(
                            op -> {
                                switch (op.operator()) {
                                    case ADD:
                                        post(new FlowRuleEvent(RULE_ADD_REQUESTED, op.target()));
                                        break;
                                    case REMOVE:
                                        post(new FlowRuleEvent(RULE_REMOVE_REQUESTED, op.target()));
                                        break;
                                    case MODIFY:
                                        //TODO: do something here when the time comes.
                                        break;
                                    default:
                                        log.warn("Unknown flow operation operator: {}", op.operator());
                                }
                            }
                    );

                    DeviceId deviceId = event.deviceId();
                    FlowRuleBatchOperation batchOperation = request.asBatchOperation(deviceId);
                    // getProvider is customized to favor driverProvider
                    FlowRuleProvider flowRuleProvider = getProvider(deviceId);
                    if (flowRuleProvider != null) {
                        log.trace("Sending {} flow rules to {}", batchOperation.size(), deviceId);
                        flowRuleProvider.executeBatch(batchOperation);
                    }

                    break;

                case BATCH_OPERATION_COMPLETED:
                    // Operation completed, let's retrieve the processor and trigger the callback
                    FlowOperationsProcessor fops = pendingFlowOperations.remove(
                            event.subject().batchId());
                    if (fops != null) {
                        if (event.result().isSuccess()) {
                            fops.satisfy(event.deviceId());
                        } else {
                            fops.fail(event.deviceId(), event.result().failedItems());
                        }
                    } else {
                        log.warn("Unable to find flow operations processor for batch: {}", event.subject().batchId());
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private static FlowRuleBatchEntry.FlowRuleOperation mapOperationType(FlowRuleOperation.Type input) {
        switch (input) {
            case ADD:
                return FlowRuleBatchEntry.FlowRuleOperation.ADD;
            case MODIFY:
                return FlowRuleBatchEntry.FlowRuleOperation.MODIFY;
            case REMOVE:
                return FlowRuleBatchEntry.FlowRuleOperation.REMOVE;
            default:
                throw new UnsupportedOperationException("Unknown flow rule type " + input);
        }
    }

    private class FlowOperationsProcessor implements Runnable {
        // Immutable
        protected final FlowRuleOperations fops;

        // Mutable
        protected final List<Set<FlowRuleOperation>> stages;
        protected final Set<DeviceId> pendingDevices = new HashSet<>();
        protected boolean hasFailed = false;

        FlowOperationsProcessor(FlowRuleOperations ops) {
            this.stages = Lists.newArrayList(ops.stages());
            this.fops = ops;
        }

        @Override
        public synchronized void run() {
            if (!stages.isEmpty()) {
                process(stages.remove(0));
            } else if (!hasFailed) {
                fops.callback().onSuccess(fops);
            }
        }

        protected void process(Set<FlowRuleOperation> ops) {
            Multimap<DeviceId, FlowRuleBatchEntry> perDeviceBatches = ArrayListMultimap.create();

            for (FlowRuleOperation op : ops) {
                perDeviceBatches.put(op.rule().deviceId(),
                                     new FlowRuleBatchEntry(mapOperationType(op.type()), op.rule()));
            }
            pendingDevices.addAll(perDeviceBatches.keySet());

            for (DeviceId deviceId : perDeviceBatches.keySet()) {
                long id = idGenerator.getNewId();
                final FlowRuleBatchOperation b = new FlowRuleBatchOperation(perDeviceBatches.get(deviceId),
                                                                            deviceId, id);
                pendingFlowOperations.put(id, this);
                deviceInstallers.execute(() -> store.storeBatch(b));
            }
        }

        synchronized void satisfy(DeviceId devId) {
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.execute(this);
            }
        }

        synchronized void fail(DeviceId devId, Set<? extends FlowRule> failures) {
            hasFailed = true;
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                operationsService.execute(this);
            }

            FlowRuleOperations.Builder failedOpsBuilder = FlowRuleOperations.builder();
            failures.forEach(failedOpsBuilder::add);

            fops.callback().onError(failedOpsBuilder.build());
        }
    }

    // Provides in-order processing in the local instance. The main difference with its
    // ancestor is that the runnable ends when all the stages have been processed. Instead,
    // its ancestor ends as soon as one stage has been processed and cannot guarantee in-order
    // processing between subsequent stages and a new FlowRuleOperation (having the same key).
    private class PredictableFlowOperationsProcessor extends FlowOperationsProcessor implements PickyRunnable {

        private static final int WAIT_TIMEOUT = 5000;
        private static final int WAIT_ATTEMPTS = 3;

        PredictableFlowOperationsProcessor(FlowRuleOperations ops) {
            super(ops);
        }

        @Override
        public void run() {
            try {
                while (!stages.isEmpty()) {
                    process(stages.remove(0));
                    synchronized (this) {
                        // Batch in flights - let's wait
                        int attempts = 0;
                        while (!pendingDevices.isEmpty() && attempts < WAIT_ATTEMPTS) {
                            this.wait(WAIT_TIMEOUT);
                            attempts++;
                        }
                        // Something wrong, we cannot block all the pipeline
                        if (attempts == WAIT_ATTEMPTS) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Interrupted case
                if (log.isTraceEnabled()) {
                    log.trace("Interrupted while waiting for {} stages to be completed",
                            stages.size());
                }
            }

            synchronized (this) {
                if (stages.isEmpty() && !hasFailed && pendingDevices.isEmpty()) {
                    // No error and it is done, signal success to the apps
                    fops.callback().onSuccess(fops);
                } else {
                    // It was interrupted or there is a failure - signal error.
                    // This may introduce a duplicate error in some cases but
                    // better than nothing and keeping the apps blocked forever.
                    FlowRuleOperations.Builder failedOpsBuilder = FlowRuleOperations.builder();
                    if (!stages.isEmpty()) {
                        stages.remove(0).forEach(flowRuleOperation -> failedOpsBuilder.add(
                                flowRuleOperation.rule()));
                    }
                    fops.callback().onError(failedOpsBuilder.build());
                }
            }
        }

        @Override
        synchronized void satisfy(DeviceId devId) {
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                this.notifyAll();
            }
        }

        @Override
        synchronized void fail(DeviceId devId, Set<? extends FlowRule> failures) {
            hasFailed = true;
            pendingDevices.remove(devId);
            if (pendingDevices.isEmpty()) {
                this.notifyAll();
            }

            FlowRuleOperations.Builder failedOpsBuilder = FlowRuleOperations.builder();
            failures.forEach(failedOpsBuilder::add);

            fops.callback().onError(failedOpsBuilder.build());
        }

        @Override
        public int hint() {
            return fops.stripeKey().orElse(randomGenerator.nextInt());
        }
    }

    @Override
    public Iterable<TableStatisticsEntry> getFlowTableStatistics(DeviceId deviceId) {
        checkPermission(FLOWRULE_READ);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getTableStatistics(deviceId);
    }

    @Override
    public long getActiveFlowRuleCount(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getActiveFlowRuleCount(deviceId);
    }

    @Override
    public void applyFlowRules(int key, FlowRule... flowRules) {
        checkPermission(FLOWRULE_WRITE);

        apply(buildFlowRuleOperations(true, key, flowRules));
    }

    @Override
    public void removeFlowRules(int key, FlowRule... flowRules) {
        checkPermission(FLOWRULE_WRITE);

        apply(buildFlowRuleOperations(false, key, flowRules));
    }

    private FlowRuleOperations buildFlowRuleOperations(boolean add, Integer key, FlowRule... flowRules) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        for (FlowRule flowRule : flowRules) {
            if (add) {
                builder.add(flowRule);
            } else {
                builder.remove(flowRule);
            }
        }
        if (key != null) {
            builder.striped(key);
        }
        return builder.build();
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    DeviceId deviceId = event.subject().id();
                    if (!deviceService.isAvailable(deviceId)) {
                        BasicDeviceConfig cfg = netCfgService.getConfig(deviceId, BasicDeviceConfig.class);
                        // if purgeOnDisconnection is set for the device or it's a global configuration
                        // lets remove the flows. Priority is given to the per device flag
                        boolean purge = cfg != null && cfg.isPurgeOnDisconnectionConfigured() ?
                                cfg.purgeOnDisconnection() : purgeOnDisconnection;
                        if (purge) {
                            log.info("PurgeOnDisconnection is requested for device {}, " +
                                             "removing flows", deviceId);
                            store.purgeFlowRule(deviceId);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
