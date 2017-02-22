/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.provider.lisp.mapping.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispMessageListener;
import org.onosproject.lisp.ctl.LispRouterId;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.lisp.msg.protocols.LispMapNotify;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispMapReply;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingProvider;
import org.onosproject.mapping.MappingProviderRegistry;
import org.onosproject.mapping.MappingProviderService;
import org.onosproject.mapping.MappingStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.lisp.mapping.util.MappingEntryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.mapping.MappingStore.Type.MAP_CACHE;
import static org.onosproject.mapping.MappingStore.Type.MAP_DATABASE;

/**
 * Provider which uses a LISP controller to manage EID-RLOC mapping.
 */
@Component(immediate = true)
public class LispMappingProvider extends AbstractProvider implements MappingProvider {

    private static final Logger log = LoggerFactory.getLogger(LispMappingProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LispController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MappingProviderRegistry providerRegistry;

    protected MappingProviderService providerService;

    private static final String SCHEME_NAME = "lisp";
    private static final String MAPPING_PROVIDER_PACKAGE =
                                "org.onosproject.lisp.provider.mapping";

    private final InternalMappingProvider listener = new InternalMappingProvider();

    /**
     * Creates a LISP mapping provider with the supplier identifier.
     */
    public LispMappingProvider() {
        super(new ProviderId(SCHEME_NAME, MAPPING_PROVIDER_PACKAGE));
    }

    @Activate
    public void activate() {

        providerService = providerRegistry.register(this);

        // listens all LISP router related events
        controller.addRouterListener(listener);

        // listens all LISP control message
        controller.addMessageListener(listener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        providerRegistry.unregister(this);

        // stops listening all LISP router related events
        controller.removeRouterListener(listener);

        // stops listening all LISP control messages
        controller.removeMessageListener(listener);

        providerService = null;

        log.info("Stopped");
    }

    /**
     * A listener for LISP router events and control messages.
     */
    private class InternalMappingProvider implements LispRouterListener,
                                                     LispMessageListener {

        @Override
        public void routerAdded(LispRouterId routerId) {

        }

        @Override
        public void routerRemoved(LispRouterId routerId) {

        }

        @Override
        public void routerChanged(LispRouterId routerId) {

        }

        @Override
        public void handleIncomingMessage(LispRouterId routerId, LispMessage msg) {

        }

        @Override
        public void handleOutgoingMessage(LispRouterId routerId, LispMessage msg) {
            if (providerService == null) {
                // We are shutting down, nothing to be done
                return;
            }

            DeviceId deviceId = DeviceId.deviceId(routerId.toString());
            switch (msg.getType()) {

                case LISP_MAP_REPLY:
                    LispMapReply reply = (LispMapReply) msg;
                    processMappings(deviceId, reply.getMapRecords(), MAP_CACHE);
                    break;

                case LISP_MAP_NOTIFY:
                    LispMapNotify notify = (LispMapNotify) msg;
                    processMappings(deviceId, notify.getMapRecords(), MAP_DATABASE);
                    break;

                default:
                    log.warn("Unhandled message type: {}", msg.getType());
            }
        }

        /**
         * Converts map records into mapping, notifies to provider.
         *
         * @param deviceId device identifier
         * @param records  a collection of map records
         * @param type     MappingStore type
         */
        private void processMappings(DeviceId deviceId,
                                     List<LispMapRecord> records,
                                     MappingStore.Type type) {
            records.forEach(r -> {
                MappingEntry me = new MappingEntryBuilder(deviceId, r).build();
                providerService.mappingAdded(me, type);
            });
        }
    }
}
