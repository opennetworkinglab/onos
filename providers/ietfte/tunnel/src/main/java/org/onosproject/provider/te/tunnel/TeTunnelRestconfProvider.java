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

package org.onosproject.provider.te.tunnel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.protocol.restconf.RestConfSBController;
import org.onosproject.protocol.restconf.RestconfNotificationEventListener;
import org.onosproject.provider.te.utils.DefaultJsonCodec;
import org.onosproject.provider.te.utils.YangCompositeEncodingImpl;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetunnel.api.TeTunnelProviderService;
import org.onosproject.tetunnel.api.TeTunnelService;
import org.onosproject.tetunnel.api.tunnel.DefaultTeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.Tunnels;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.IetfTeTypes;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.provider.te.utils.CodecTools.jsonToString;
import static org.onosproject.provider.te.utils.CodecTools.toJson;
import static org.onosproject.tetopology.management.api.TeTopology.BIT_MERGED;
import static org.onosproject.teyang.utils.tunnel.TunnelConverter.buildIetfTe;
import static org.onosproject.teyang.utils.tunnel.TunnelConverter.yang2TeTunnel;
import static org.onosproject.yms.ych.YangProtocolEncodingFormat.JSON;
import static org.onosproject.yms.ych.YangResourceIdentifierType.URI;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REPLY;


/**
 * Provider which uses RESTCONF to do cross-domain tunnel creation/deletion/
 * update/deletion and so on operations on the domain networks.
 */

@Component(immediate = true)
public class TeTunnelRestconfProvider extends AbstractProvider
        implements TunnelProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String SCHEMA = "ietf";
    private static final String IETF = "ietf";
    private static final String TE = "te";
    private static final int DEFAULT_INDEX = 1;
    private static final String TUNNELS = "tunnels";
    private static final String TUNNELS_URL = IETF + ":" + TE + "/" + TUNNELS;
    private static final String IETF_NOTIFICATION_URI = "netconf";
    private static final String MEDIA_TYPE_JSON = "json";

    private static final String SHOULD_IN_ONE = "Tunnel should be setup in one topo";
    private static final String PROVIDER_ID = "org.onosproject.provider.ietf";
    private static final String RESTCONF_ROOT = "/onos/restconf";
    private static final String TE_TUNNEL_KEY = "TeTunnelKey";

    //private final RestconfNotificationEventListener listener =
    //        new InternalTunnelNotificationListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestConfSBController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelProviderService providerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelProviderRegistry tunnelProviderRegistry;

    private YangCodecHandler codecHandler;

    @Activate
    public void activate() {
        tunnelProviderRegistry.register(this);
        codecHandler = ymsService.getYangCodecHandler();
        codecHandler.addDeviceSchema(IetfTe.class);
        codecHandler.addDeviceSchema(IetfTeTypes.class);
        codecHandler.registerOverriddenCodec(new DefaultJsonCodec(ymsService),
                                             YangProtocolEncodingFormat.JSON);
        collectInitialTunnels();
        subscribe();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        tunnelProviderRegistry.unregister(this);
        unsubscribe();
        log.info("Stopped");

    }

    public TeTunnelRestconfProvider() {
        super(new ProviderId(SCHEMA, PROVIDER_ID));
    }

    private void collectInitialTunnels() {
        for (DeviceId deviceId : controller.getDevices().keySet()) {
            ObjectNode jsonNodes = executeGetRequest(deviceId);
            if (jsonNodes == null) {
                continue;
            }
            ObjectNode tunnelsNode = (ObjectNode) jsonNodes.get(TUNNELS);
            if (tunnelsNode == null) {
                continue;
            }
            Tunnels teTunnels = getYangTunnelsObject(tunnelsNode);
            if (teTunnels == null) {
                continue;
            }
            updateTeTunnels(teTunnels);
        }
    }

    private void subscribe() {
        for (DeviceId deviceId : controller.getDevices().keySet()) {
            try {
                if (!controller.isNotificationEnabled(deviceId)) {
                    controller.enableNotifications(deviceId, IETF_NOTIFICATION_URI,
                                                   "application/json",
                                                   new InternalTunnelNotificationListener());
                } else {
                    controller.addNotificationListener(deviceId,
                                                       new InternalTunnelNotificationListener());
                }
            } catch (Exception e) {
                log.error("Failed to subscribe for {} : {}", deviceId,
                          e.getMessage());
            }
        }
    }

    private void unsubscribe() {
        controller.getDevices()
                .keySet()
                .forEach(deviceId -> controller
                        .removeNotificationListener(deviceId,
                                                    new InternalTunnelNotificationListener()));
    }

    @Override
    public void setupTunnel(Tunnel tunnel, Path path) {
        TeTunnel teTunnel = tunnelService.getTeTunnel(tunnel.tunnelId());
        long tid = teTunnel.srcNode().topologyId();
        checkState(tid == teTunnel.dstNode().topologyId(), SHOULD_IN_ONE);
        setupTunnel(getOwnDevice(tid), tunnel, path);
    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        if (!tunnel.annotations().keys().contains(TE_TUNNEL_KEY)) {
            log.warn("No tunnel key info in tunnel {}", tunnel);
            return;
        }

        String teTunnelKey = tunnel.annotations().value(TE_TUNNEL_KEY);

        Optional<TeTunnel> optTunnel = tunnelService.getTeTunnels()
                .stream()
                .filter(t -> t.teTunnelKey().toString().equals(teTunnelKey))
                .findFirst();

        if (!optTunnel.isPresent()) {
            log.warn("No te tunnel map to tunnel {}", tunnel);
            return;
        }

        IetfTe ietfTe = buildIetfTe(optTunnel.get(), true);

        YangCompositeEncoding encoding = codecHandler.
                encodeCompositeOperation(RESTCONF_ROOT, null, ietfTe,
                                         JSON, EDIT_CONFIG_REQUEST);
        String identifier = encoding.getResourceIdentifier();
        String resourceInformation = encoding.getResourceInformation();

        if (srcElement == null) {
            log.error("Can't find remote device for tunnel : {}", tunnel);
            return;
        }
        controller.post((DeviceId) srcElement, identifier,
                        new ByteArrayInputStream(resourceInformation.getBytes()),
                        MEDIA_TYPE_JSON, ObjectNode.class);
    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {
        //TODO implement release tunnel method
    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        //TODO implement release tunnel with src method
    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        //TODO implement update tunnel method

    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        //TODO implement update tunnel with src method
    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {
        //TODO implement tunnel add method when te tunnel app merged to core
        return null;
    }

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        //TODO implement tunnel remove method when te tunnel app merged to core

    }

    @Override
    public void tunnelUpdated(TunnelDescription tunnel) {
        //TODO implement tunnel update method when te tunnel app merged to core
    }

    @Override
    public Tunnel tunnelQueryById(TunnelId tunnelId) {
        return null;
    }

    private ObjectNode executeGetRequest(DeviceId deviceId) {
        //the request url is ietf-te:te/tunnels
        //the response node will begin with tunnels
        //be careful here to when get the tunnels data
        InputStream resultStream =
                controller.get(deviceId, TUNNELS_URL, MEDIA_TYPE_JSON);
        return toJson(resultStream);
    }

    private Tunnels getYangTunnelsObject(ObjectNode tunnelsNode) {
        checkNotNull(tunnelsNode, "Input object node should not be null");

        YangCompositeEncoding yce =
                new YangCompositeEncodingImpl(URI,
                                              TUNNELS_URL,
                                              jsonToString(tunnelsNode));

        Object yo = codecHandler.decode(yce, JSON, QUERY_REPLY);

        if (yo == null) {
            log.error("YMS decoder returns null");
            return null;
        }
        IetfTe ietfTe = null;
        Tunnels tunnels = null;
        if (yo instanceof List) {
            List<Object> list = (List<Object>) yo;
            ietfTe = (IetfTe) list.get(DEFAULT_INDEX);
        }
        if (ietfTe != null && ietfTe.te() != null) {
            tunnels = ietfTe.te().tunnels();
        }
        return tunnels;
    }

    private void updateTeTunnels(Tunnels tunnels) {
        TeTopologyKey key = getTopologyKey();

        tunnels.tunnel().forEach(tunnel -> {
            DefaultTeTunnel teTunnel = yang2TeTunnel(tunnel, key);
            providerService.updateTeTunnel(teTunnel);
        });
    }

    private TeTopologyKey getTopologyKey() {
        TeTopologyKey key = null;
        Optional<TeTopology> teTopology = topologyService.teTopologies()
                .teTopologies()
                .values()
                .stream()
                .filter(topology -> topology.flags().get(BIT_MERGED))
                .findFirst();
        if (teTopology.isPresent()) {
            TeTopology topology = teTopology.get();
            key = topology.teTopologyId();
        }
        return key;
    }

    private DeviceId getOwnDevice(long topologyId) {
        DeviceId deviceId = null;
        Optional<TeTopology> topoOpt = topologyService.teTopologies()
                .teTopologies()
                .values()
                .stream()
                .filter(tp -> tp.teTopologyId().topologyId() == topologyId)
                .findFirst();

        if (topoOpt.isPresent()) {
            deviceId = topoOpt.get().ownerId();
        }
        return deviceId;
    }


    private class InternalTunnelNotificationListener implements
            RestconfNotificationEventListener {

        @Override
        public void handleNotificationEvent(DeviceId deviceId, Object eventJsonString) {
            ObjectNode response = toJson((String) eventJsonString);
            if (response == null) {
                return;
            }
            JsonNode teNode = response.get(TE);
            if (teNode == null) {
                log.error("Illegal te json object from {}", deviceId);
                return;
            }
            JsonNode tunnelsNode = teNode.get(TUNNELS);
            if (tunnelsNode == null) {
                log.error("Illegal tunnel json object from {}", deviceId);
                return;
            }

            Tunnels tunnels = getYangTunnelsObject((ObjectNode) tunnelsNode);
            if (tunnels == null) {
                return;
            }
            updateTeTunnels(tunnels);
        }
    }
}
