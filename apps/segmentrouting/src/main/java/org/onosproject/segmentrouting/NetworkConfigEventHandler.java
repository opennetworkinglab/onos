package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles network config events.
 */
public class NetworkConfigEventHandler {
    private static final Logger log = LoggerFactory.getLogger(NetworkConfigEventHandler.class);
    private final SegmentRoutingManager srManager;
    private final DeviceService deviceService;

    /**
     * Constructs Network Config Event Handler.
     *
     * @param srManager instance of {@link SegmentRoutingManager}
     */
    public NetworkConfigEventHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.deviceService = srManager.deviceService;
    }

    /**
     * Processes vRouter config added event.
     *
     * @param event network config added event
     */
    protected void processVRouterConfigAdded(NetworkConfigEvent event) {
        log.info("Processing vRouter CONFIG_ADDED");
        SegmentRoutingAppConfig config = (SegmentRoutingAppConfig) event.config().get();
        deviceService.getAvailableDevices().forEach(device -> {
            populateVRouter(device.id(), getMacAddresses(config));
        });
    }

    /**
     * Processes vRouter config updated event.
     *
     * @param event network config updated event
     */
    protected void processVRouterConfigUpdated(NetworkConfigEvent event) {
        log.info("Processing vRouter CONFIG_UPDATED");
        SegmentRoutingAppConfig config = (SegmentRoutingAppConfig) event.config().get();
        SegmentRoutingAppConfig prevConfig = (SegmentRoutingAppConfig) event.prevConfig().get();
        deviceService.getAvailableDevices().forEach(device -> {
            Set<MacAddress> macAddresses = getMacAddresses(config);
            Set<MacAddress> prevMacAddresses = getMacAddresses(prevConfig);
            // Avoid removing and re-adding unchanged MAC addresses since
            // FlowObjective does not guarantee the execution order.
            Set<MacAddress> sameMacAddresses = new HashSet<>(macAddresses);
            sameMacAddresses.retainAll(prevMacAddresses);
            macAddresses.removeAll(sameMacAddresses);
            prevMacAddresses.removeAll(sameMacAddresses);

            revokeVRouter(device.id(), prevMacAddresses);
            populateVRouter(device.id(), macAddresses);
        });

    }

    /**
     * Processes vRouter config removed event.
     *
     * @param event network config removed event
     */
    protected void processVRouterConfigRemoved(NetworkConfigEvent event) {
        log.info("Processing vRouter CONFIG_REMOVED");
        SegmentRoutingAppConfig prevConfig = (SegmentRoutingAppConfig) event.prevConfig().get();
        deviceService.getAvailableDevices().forEach(device -> {
            revokeVRouter(device.id(), getMacAddresses(prevConfig));
        });
    }

    /**
     * Populates initial vRouter rules.
     *
     * @param deviceId device ID
     */
    public void initVRouters(DeviceId deviceId) {
        SegmentRoutingAppConfig config =
                srManager.cfgService.getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        populateVRouter(deviceId, getMacAddresses(config));
    }

    private void populateVRouter(DeviceId deviceId, Set<MacAddress> pendingAdd) {
        if (!isEdge(deviceId)) {
            return;
        }
        getVRouterFlowObjBuilders(pendingAdd).forEach(foBuilder -> {
            srManager.flowObjectiveService.
                    filter(deviceId, foBuilder.add(new SRObjectiveContext(deviceId,
                            SRObjectiveContext.ObjectiveType.FILTER)));
        });
    }

    private void revokeVRouter(DeviceId deviceId, Set<MacAddress> pendingRemove) {
        if (!isEdge(deviceId)) {
            return;
        }
        getVRouterFlowObjBuilders(pendingRemove).forEach(foBuilder -> {
            srManager.flowObjectiveService.
                    filter(deviceId, foBuilder.remove(new SRObjectiveContext(deviceId,
                            SRObjectiveContext.ObjectiveType.FILTER)));
        });
    }

    private Set<FilteringObjective.Builder> getVRouterFlowObjBuilders(Set<MacAddress> macAddresses) {
        ImmutableSet.Builder<FilteringObjective.Builder> setBuilder = ImmutableSet.builder();
        macAddresses.forEach(macAddress -> {
            FilteringObjective.Builder fobuilder = DefaultFilteringObjective.builder();
            fobuilder.withKey(Criteria.matchInPort(PortNumber.ANY))
                    .addCondition(Criteria.matchEthDst(macAddress))
                    .permit()
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                    .fromApp(srManager.appId);
            setBuilder.add(fobuilder);
        });
        return setBuilder.build();
    }

    private Set<MacAddress> getMacAddresses(SegmentRoutingAppConfig config) {
        if (config == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(config.vRouterMacs());
    }

    private boolean isEdge(DeviceId deviceId) {
        try {
            if (srManager.deviceConfiguration.isEdgeDevice(deviceId)) {
                return true;
            }
        } catch (DeviceConfigNotFoundException e) { }
        return false;
    }
}
