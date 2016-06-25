/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.impl;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.constraint.EncapsulationConstraint;
import org.onosproject.net.provider.AbstractProvider;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Point to point intent VirtualNetworkProvider implementation.
 */
@Component(immediate = true)
@Service
public class PtToPtIntentVirtualNetworkProvider extends AbstractProvider
        implements VirtualNetworkProvider {

    private final Logger log = getLogger(PtToPtIntentVirtualNetworkProvider.class);
    private static final String NETWORK_ID_NULL = "Network ID cannot be null";
    private static final String CONNECT_POINT_NULL = "Connect Point cannot be null";
    private static final String INTENT_NULL = "Intent cannot be null";
    private static final String NETWORK_ID = "networkId=";
    protected static final String KEY_FORMAT = NETWORK_ID + "%s, src=%s, dst=%s";
    private static final int MAX_WAIT_COUNT = 30;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkProviderRegistry providerRegistry;

    private VirtualNetworkProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private final InternalPtPtIntentListener intentListener = new InternalPtPtIntentListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected static final String PTPT_INTENT_APPID = "org.onosproject.vnet.intent";
    private ApplicationId appId;

    /**
     * Default constructor.
     */
    public PtToPtIntentVirtualNetworkProvider() {
        super(DefaultVirtualLink.PID);
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(PTPT_INTENT_APPID);

        intentService.addListener(intentListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        intentService.removeListener(intentListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public TunnelId createTunnel(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        checkNotNull(networkId, NETWORK_ID_NULL);
        checkNotNull(src, CONNECT_POINT_NULL);
        checkNotNull(dst, CONNECT_POINT_NULL);
        Key intentKey = encodeKey(networkId, src, dst);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(new EncapsulationConstraint(EncapsulationType.VLAN));

        // TODO Currently there can only be one tunnel/intent between the src and dst across
        // all virtual networks. We may want to support multiple intents between the same src/dst pairs.
        PointToPointIntent intent = PointToPointIntent.builder()
                .key(intentKey)
                .appId(appId)
                .ingressPoint(src)
                .egressPoint(dst)
                .constraints(constraints)
                .build();
        intentService.submit(intent);

        // construct tunnelId from the key
        return TunnelId.valueOf(intentKey.toString());
    }

    @Override
    public void destroyTunnel(NetworkId networkId, TunnelId tunnelId) {
        String key = tunnelId.id();
        Key intentKey = Key.of(key, appId);
        Intent intent = intentService.getIntent(intentKey);
        checkNotNull(intent, INTENT_NULL);
        intentService.withdraw(intent);
    }

    private NetworkId decodeNetworkIdFromKey(Key intentKey) {
        // Extract the network identifier from the intent key
        StringTokenizer tokenizer = new StringTokenizer(intentKey.toString(), ",");
        String networkIdString = tokenizer.nextToken().substring(NETWORK_ID.length());
        return NetworkId.networkId(Integer.valueOf(networkIdString));
    }

    private Key encodeKey(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        String key = String.format(KEY_FORMAT, networkId, src, dst);
        return Key.of(key, appId);
    }

    private class InternalPtPtIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            PointToPointIntent intent = (PointToPointIntent) event.subject();
            Key intentKey = intent.key();

            // Ignore intent events that are not relevant.
            if (!isRelevant(event)) {
                return;
            }

            NetworkId networkId = decodeNetworkIdFromKey(intentKey);
            ConnectPoint src = intent.ingressPoint();
            ConnectPoint dst = intent.egressPoint();

            switch (event.type()) {
                case INSTALLED:
                    providerService.tunnelUp(networkId, src, dst, TunnelId.valueOf(intentKey.toString()));
                    break;
                case WITHDRAWN:
                    intentService.purge(intent);
                    // Fall through and notify the provider service that the tunnel is down
                    // for both WITHDRAWN and FAILED intent event types.
                case FAILED:
                    providerService.tunnelDown(networkId, src, dst, TunnelId.valueOf(intentKey.toString()));
                    break;
                case INSTALL_REQ:
                case CORRUPT:
                case PURGED:
                    break; // Not sure what do with these events, ignore for now.
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(IntentEvent event) {
            if (event.subject() instanceof  PointToPointIntent) {
                PointToPointIntent intent = (PointToPointIntent) event.subject();

                // Only events that are for this appId are relevent.
                return intent.appId().equals(appId);
            }
            return false;
        }
    }
}

