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

package org.onosproject.incubator.net.virtual.impl.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualGroupProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider to handle Group for virtual network.
 */
@Component(service = VirtualGroupProvider.class)
public class DefaultVirtualGroupProvider extends AbstractVirtualProvider
        implements VirtualGroupProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualProviderRegistryService providerRegistryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    private InternalGroupEventListener internalGroupEventListener;

    /**
     * Creates a virtual provider with the supplied identifier.
     */
    public DefaultVirtualGroupProvider() {
        super(new ProviderId("vnet-group", "org.onosproject.virtual.of-group"));
    }

    @Activate
    public void activate() {
        providerRegistryService.registerProvider(this);

        internalGroupEventListener = new InternalGroupEventListener();
        groupService.addListener(internalGroupEventListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        groupService.removeListener(internalGroupEventListener);
        providerRegistryService.unregisterProvider(this);
    }

    @Modified
    protected void modified(ComponentContext context) {
    }

    @Override
    public void performGroupOperation(NetworkId networkId, DeviceId deviceId, GroupOperations groupOps) {
        for (GroupOperation groupOperation: groupOps.operations()) {
            switch (groupOperation.opType()) {
                case ADD:
                    //TODO: devirtualize + groupAdd
                    log.info("Group Add is not supported, yet");
                    break;
                case MODIFY:
                    //TODO: devirtualize + groupMod
                    log.info("Group Modify is not supported, yet");
                    break;
                case DELETE:
                    //TODO: devirtualize + groupDel
                    log.info("Group Delete is not supported, yet");
                    break;
                default:
                    log.error("Unsupported Group operation");
                    return;
            }
        }
    }

    private class InternalGroupEventListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            switch (event.type()) {
                //TODO: virtualize + notify to virtual provider service
                case GROUP_ADD_REQUESTED:
                case GROUP_UPDATE_REQUESTED:
                case GROUP_REMOVE_REQUESTED:
                case GROUP_ADDED:
                case GROUP_UPDATED:
                case GROUP_REMOVED:
                case GROUP_ADD_FAILED:
                case GROUP_UPDATE_FAILED:
                case GROUP_REMOVE_FAILED:
                case GROUP_BUCKET_FAILOVER:
                default:
                    break;
            }
        }
    }
}
