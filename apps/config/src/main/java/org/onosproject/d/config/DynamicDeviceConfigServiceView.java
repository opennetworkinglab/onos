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
package org.onosproject.d.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.d.config.ResourceIds.ROOT_ID;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import org.onosproject.config.ForwardingDynamicConfigService;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;

/**
 * DynamicConfigService interface to provide a viewport
 * under specified Device's tree.
 */
@Beta
public class DynamicDeviceConfigServiceView
        extends ForwardingDynamicConfigService
        implements DynamicConfigService {

    private static final Logger log = getLogger(DynamicDeviceConfigServiceView.class);

    private final DeviceId deviceId;

    // absolute ResourceId
    private ResourceId deviceRoot;

    /**
     * original to wrapped listener.
     */
    private final Map<DynamicConfigListener, DynamicConfigListener> wrapped =
            Collections.synchronizedMap(new IdentityHashMap<>());

    protected DynamicDeviceConfigServiceView(DynamicConfigService delegate, DeviceId deviceId) {
        super(delegate);
        this.deviceId = checkNotNull(deviceId);
        this.deviceRoot = DeviceResourceIds.toResourceId(this.deviceId);
    }

    /**
     * Create a DynamicDeviceConfigServiceView for specified {@code deviceId}.
     *
     * @param service reference to actual DynamicConfigService.
     * @param deviceId of the Device to provide view port about.
     * @return DynamicDeviceConfigServiceView
     */
    public static DynamicDeviceConfigServiceView deviceView(DynamicConfigService service,
                                                            DeviceId deviceId) {
        return new DynamicDeviceConfigServiceView(service, deviceId);
    }

    @Override
    public void createNode(ResourceId path, DataNode node) {
        super.createNode(toAbsoluteId(path), node);
    }

    @Override
    public DataNode readNode(ResourceId path, Filter filter) {
        // FIXME transform filter? is it possible?
        return super.readNode(toAbsoluteId(path), filter);
    }

    @Override
    public Boolean nodeExist(ResourceId path) {
        return super.nodeExist(toAbsoluteId(path));
    }

    @Override
    public void updateNode(ResourceId path, DataNode node) {
        super.updateNode(toAbsoluteId(path), node);
    }

    @Override
    public void replaceNode(ResourceId path, DataNode node) {
        super.replaceNode(toAbsoluteId(path), node);
    }

    @Override
    public void deleteNode(ResourceId path) {
        super.deleteNode(toAbsoluteId(path));
    }

    @Override
    public CompletableFuture<RpcOutput> invokeRpc(RpcInput input) {
        return super.invokeRpc(new RpcInput(toAbsoluteId(input.id()), input.data()));
    }

    @Override
    public void addListener(DynamicConfigListener listener) {
        super.addListener(wrapped.computeIfAbsent(listener,
                                                  DynamicDeviceConfigListener::new));
    }

    @Override
    public void removeListener(DynamicConfigListener listener) {
        DynamicConfigListener w = wrapped.remove(listener);
        if (w != null) {
            super.removeListener(w);
        }
    }

    private ResourceId toAbsoluteId(ResourceId path) {
        checkArgument(!path.nodeKeys().contains(DeviceResourceIds.ROOT_NODE),
                      "%s was already absolute path", path);

        return ResourceIds.concat(deviceRoot, path);
    }

    private ResourceId toDeviceRelativeId(ResourceId path) {
        // case: absolute
        if (ResourceIds.startsWithRootNode(path)) {
            return ResourceIds.relativize(deviceRoot, path);
        }
        // case: root relative
        if (DeviceResourceIds.isUnderDeviceRootNode(path)) {
            //                                        TODO not efficient
            return ResourceIds.relativize(deviceRoot, ResourceIds.concat(ROOT_ID, path));
        }
        throw new IllegalArgumentException(path + " was not absolute device path");
    }

    class DynamicDeviceConfigListener implements DynamicConfigListener {

        private final DynamicConfigListener lsnr;

        DynamicDeviceConfigListener(DynamicConfigListener lsnr) {
            this.lsnr = checkNotNull(lsnr);
        }

        private DynamicConfigEvent deviceEvent(DynamicConfigEvent event) {
            return new DynamicConfigEvent(event.type(),
                                          toDeviceRelativeId(event.subject()));
        }

        @Override
        public boolean isRelevant(DynamicConfigEvent event) {
            // FIXME ignore event irrelevant to current device
            return lsnr.isRelevant(deviceEvent(event));
        }

        @Override
        public void event(DynamicConfigEvent event) {
            lsnr.event(deviceEvent(event));
        }

    }
}
