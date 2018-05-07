/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config.sync.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.onosproject.d.config.DeviceResourceIds.isUnderDeviceRootNode;
import static org.onosproject.d.config.DeviceResourceIds.toDeviceId;
import static org.onosproject.d.config.DeviceResourceIds.toResourceId;
import static org.onosproject.d.config.sync.operation.SetResponse.response;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigEvent.Type;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.d.config.DataNodes;
import org.onosproject.d.config.DeviceResourceIds;
import org.onosproject.d.config.ResourceIds;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProvider;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProviderRegistry;
import org.onosproject.d.config.sync.DeviceConfigSynchronizationProviderService;
import org.onosproject.d.config.sync.operation.SetRequest;
import org.onosproject.d.config.sync.operation.SetResponse;
import org.onosproject.d.config.sync.operation.SetResponse.Code;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Component to bridge Dynamic Config store and the Device configuration state.
 *
 * <ul>
 * <li> Propagate DynamicConfig service change downward to Device side via provider.
 * <li> Propagate Device triggered change event upward to DyamicConfig service.
 * </ul>
 */
@Beta
@Component(immediate = true)
@Service
public class DynamicDeviceConfigSynchronizer
    extends AbstractProviderRegistry<DeviceConfigSynchronizationProvider,
                                     DeviceConfigSynchronizationProviderService>
    implements DeviceConfigSynchronizationProviderRegistry {

    private static final Logger log = getLogger(DynamicDeviceConfigSynchronizer.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dynConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netcfgService;

    private DynamicConfigListener listener = new InnerDyConListener();

    // FIXME hack for unconsolidated event bug
    private Duration quietPeriod = Duration.ofSeconds(2);
    private long quietUntil = 0;

    @Activate
    public void activate() {
        // TODO start background task to sync Controller and Device?
        dynConfigService.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        dynConfigService.removeListener(listener);
        log.info("Stopped");
    }


    @Override
    protected DeviceConfigSynchronizationProviderService createProviderService(
                                 DeviceConfigSynchronizationProvider provider) {
        return new InternalConfigSynchronizationServiceProvider(provider);
    }

    @Override
    protected DeviceConfigSynchronizationProvider defaultProvider() {
        // TODO return provider instance which can deal with "general" provider?
        return super.defaultProvider();
    }

    /**
     * Proxy to relay Device change event for propagating running "state"
     * information up to dynamic configuration service.
     */
    class InternalConfigSynchronizationServiceProvider
        extends AbstractProviderService<DeviceConfigSynchronizationProvider>
        implements DeviceConfigSynchronizationProviderService {

        protected InternalConfigSynchronizationServiceProvider(DeviceConfigSynchronizationProvider provider) {
            super(provider);
        }

        // TODO API for passive information propagation to be added later on
    }

    /**
     * DynamicConfigListener to trigger active synchronization toward the device.
     */
    class InnerDyConListener implements DynamicConfigListener {

        @Override
        public boolean isRelevant(DynamicConfigEvent event) {
            // TODO NetconfActiveComponent.isRelevant(DynamicConfigEvent)
            // seems to be doing some filtering
            // Logic filtering for L3VPN is probably a demo hack,
            // but is there any portion of it which is really needed?
            // e.g., listen only for device tree events?

            ResourceId path = event.subject();
            // TODO only device tree related event is relevant.
            // 1) path is under device tree
            // 2) path is root, and DataNode contains element under node
            // ...
            return true;
        }

        @Override
        public void event(DynamicConfigEvent event) {
            // Note: removed accumulator in the old code assuming,
            // event accumulation will happen on Device Config Event level.

            // TODO execute off event dispatch thread
            processEventNonBatch(event);
        }

    }

    void processEventNonBatch(DynamicConfigEvent event) {
        if (System.currentTimeMillis() < quietUntil) {
            log.trace("Ignoring {}. Quiet period until {}",
                      event, Tools.defaultOffsetDataTime(quietUntil));
            return;
        }

        ResourceId path = event.subject();
        if (isUnderDeviceRootNode(path)) {
            log.trace("processing event:{}", event);

            DeviceId deviceId = DeviceResourceIds.toDeviceId(path);
            ResourceId deviceRootPath = DeviceResourceIds.toResourceId(deviceId);

            ResourceId absPath = ResourceIds.concat(ResourceIds.ROOT_ID, path);
            ResourceId relPath = ResourceIds.relativize(deviceRootPath, absPath);
            // give me everything Filter
            Filter giveMeEverything = Filter.builder().build();

            DataNode node = dynConfigService.readNode(path, giveMeEverything);
            SetRequest request;
            switch (event.type()) {

            case NODE_ADDED:
            case NODE_REPLACED:
                request = SetRequest.builder().replace(relPath, node).build();
                break;
            case NODE_UPDATED:
                // Event has no pay load, only thing we can do is replace.
                request = SetRequest.builder().replace(relPath, node).build();
                break;
            case NODE_DELETED:
                request = SetRequest.builder().delete(relPath).build();
                break;
            case UNKNOWN_OPRN:
            default:
                log.error("Unexpected event {}, aborting", event);
                return;
            }

            log.info("Dispatching {} request {}", deviceId, request);
            CompletableFuture<SetResponse> response = dispatchRequest(deviceId, request);
            response.whenComplete((resp, e) -> {
                if (e == null) {
                    if (resp.code() == Code.OK) {
                        log.info("{} for {} complete", resp, deviceId);
                    } else {
                        log.warn("{} for {} had problem", resp, deviceId);
                    }
                } else {
                    log.error("Request to {} failed {}", deviceId, response, e);
                }
            });

            // FIXME hack for unconsolidated event bug
            quietUntil = System.currentTimeMillis() + quietPeriod.toMillis();
        } else {
            log.info("Ignored event's ResourceId: {}", event.subject());
        }
    }


    // was sketch to handle case, where event could contain batch of things...
    private void processEvent(DynamicConfigEvent event) {
        // TODO assuming event object will contain batch of (atomic) change event

        // What the new event will contain:
        Type evtType = event.type();

        // Transaction ID, can be null
        TransactionId txId = null;

        // TODO this might change into collection of (path, val_before, val_after)

        ResourceId path = event.subject();
        // data node (can be tree) representing change, it could be incremental update
        DataNode val = null;

        // build per-Device SetRequest
        // val could be a tree, containing multiple Device tree,
        // break them down into per-Device sub-tree
        Map<DeviceId, SetRequest.Builder> requests = new HashMap<>();

        if (isUnderDeviceRootNode(path)) {
            // about single device
            buildDeviceRequest(requests, evtType, path, toDeviceId(path), val);

        } else if (DeviceResourceIds.isRootOrDevicesNode(path)) {
            //  => potentially contain changes spanning multiple Devices
            Map<DeviceId, DataNode> perDevices = perDevices(path, val);

            perDevices.forEach((did, dataNode) -> {
                buildDeviceRequest(requests, evtType, toResourceId(did), did, dataNode);
            });

            // TODO special care is probably required for delete cases
            // especially delete all under devices

        } else {
            log.warn("Event not related to a Device?");
        }


        // TODO assuming event is a batch,
        // potentially containing changes for multiple devices,
        // who will process/coordinate the batch event?


        // TODO loop through per-Device change set
        List<CompletableFuture<SetResponse>> responses =
                requests.entrySet().stream()
                .map(entry -> dispatchRequest(entry.getKey(), entry.getValue().build()))
                .collect(Collectors.toList());

        // wait for all responses
        List<SetResponse> allResults = Tools.allOf(responses).join();
        // TODO deal with partial failure case (multi-device coordination)
        log.info("DEBUG: results: {}", allResults);
    }

    // might make sense to make this public
    CompletableFuture<SetResponse> dispatchRequest(DeviceId devId, SetRequest req) {

        // determine appropriate provider for this Device
        DeviceConfigSynchronizationProvider provider = this.getProvider(devId);

        if (provider == null) {
            // no appropriate provider found
            // return completed future with failed SetResponse
            return completedFuture(response(req,
                                            SetResponse.Code.FAILED_PRECONDITION,
                                            "no provider found for " + devId));
        }

        // dispatch request
        return provider.setConfiguration(devId, req)
                .handle((resp, err) -> {
                    if (err == null) {
                        // set complete
                        log.info("DEBUG: Req:{}, Resp:{}", req, resp);
                        return resp;
                    } else {
                        // fatal error
                        log.error("Fatal error on {}", req, err);
                        return response(req,
                                        SetResponse.Code.UNKNOWN,
                                        "Unknown error " + err);
                    }
                });
    }


    // may eventually reuse with batch event
    /**
     * Build device request about a Device.
     *
     * @param requests map containing request builder to populate
     * @param evtType change request type
     * @param path to {@code val}
     * @param did DeviceId which {@code path} is about
     * @param val changed node to write
     */
    private void buildDeviceRequest(Map<DeviceId, SetRequest.Builder> requests,
                            Type evtType,
                            ResourceId path,
                            DeviceId did,
                            DataNode val) {

        SetRequest.Builder request =
                requests.computeIfAbsent(did, d -> SetRequest.builder());

        switch (evtType) {
        case NODE_ADDED:
        case NODE_REPLACED:
            request.replace(path, val);
            break;

        case NODE_UPDATED:
            // TODO Auto-generated method stub
            request.update(path, val);
            break;

        case NODE_DELETED:
            // TODO Auto-generated method stub
            request.delete(path);
            break;

        case UNKNOWN_OPRN:
        default:
            log.warn("Ignoring unexpected {}", evtType);
            break;
        }
    }

    /**
     * Breaks down tree {@code val} into per Device subtree.
     *
     * @param path pointing to {@code val}
     * @param val tree which contains only 1 Device.
     * @return Device node relative DataNode for each DeviceId
     * @throws IllegalArgumentException
     */
    private static Map<DeviceId, DataNode> perDevices(ResourceId path, DataNode val) {
        if (DeviceResourceIds.isUnderDeviceRootNode(path)) {
            // - if path is device root or it's subtree, path alone is sufficient
            return ImmutableMap.of(DeviceResourceIds.toDeviceId(path), val);

        } else if (DeviceResourceIds.isRootOrDevicesNode(path)) {
            // - if path is "/" or devices, it might be constructible from val tree
            final Collection<DataNode> devicesChildren;
            if (DeviceResourceIds.isRootNode(path)) {
                // root
                devicesChildren = DataNodes.childOnlyByName(val, DeviceResourceIds.DEVICES_NAME)
                            .map(dn -> DataNodes.children(dn))
                            .orElse(ImmutableList.of());
            } else {
                // devices
                devicesChildren = DataNodes.children(val);
            }

            return devicesChildren.stream()
                    // TODO use full schemaId for filtering when ready
                    .filter(dn -> dn.key().schemaId().name().equals(DeviceResourceIds.DEVICE_NAME))
                    .collect(Collectors.toMap(dn -> DeviceResourceIds.toDeviceId(dn.key()),
                                              dn -> dn));

        }
        throw new IllegalArgumentException(path + " not related to Device");
    }

}
