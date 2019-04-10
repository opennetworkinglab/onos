/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.onlpdemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import gnmi.Gnmi;
import org.onlab.util.SharedExecutors;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * Extends the ONOS GUI to display various ONLP device data.
 */
@Component(immediate = true, service = OnlpDemoManager.class)
public class OnlpDemoManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String EXTENSION_ID = "onlpdemo";
    private static final String OVERLAY_ID = "od-overlay";

    private static final String OVERLAY_VIEW_ID = "odTopov";
    private static final String TABLE_VIEW_ID = "onlp";

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiViewHidden(OVERLAY_VIEW_ID),
            new UiViewHidden(TABLE_VIEW_ID)
    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(new OnlpDemoViewMessageHandler(new GnmiOnlpDataSource()));

    // Factory for UI topology overlays
    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(new UiTopoOverlay(OVERLAY_ID));

    // Application UI extension
    protected UiExtension extension =
            new UiExtension.Builder(getClass().getClassLoader(), uiViews)
                    .resourcePath(EXTENSION_ID)
                    .messageHandlerFactory(messageHandlerFactory)
                    .topoOverlayFactory(topoOverlayFactory)
                    .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GnmiController gnmiController;

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }


    public class OnlpData {
        String id;
        String presence;
        String vendor;
        String modelNumber;
        String serialNumber;
        String formFactor;

        OnlpData(String id, String presence, String vendor, String modelNumber,
                 String serialNumber, String formFactor) {
            this.id = id;
            this.presence = presence;
            this.vendor = vendor;
            this.modelNumber = modelNumber;
            this.serialNumber = serialNumber;
            this.formFactor = formFactor;
        }
    }

    public interface OnlpDataSource {
        List<OnlpData> getData(DeviceId deviceId);
    }


    public class GnmiOnlpDataSource implements OnlpDataSource {

        private Map<DeviceId, List<OnlpData>> cache = Maps.newConcurrentMap();

        GnmiOnlpDataSource() {
            SharedExecutors.getTimer().schedule(new TimerTask() {
                @Override
                public void run() {
                    cache.keySet().forEach(GnmiOnlpDataSource.this::fetchData);
                }
            }, 5000, 5000);
        }

        @Override
        public List<OnlpData> getData(DeviceId deviceId) {
            return cache.computeIfAbsent(deviceId, k -> ImmutableList.of());
        }

        private void fetchData(DeviceId deviceId) {
            ImmutableList.Builder<OnlpData> builder = ImmutableList.builder();
            GnmiClient gnmiClient = gnmiController.get(deviceId);
            deviceService.getPorts(deviceId)
                    .forEach(port -> builder.add(getOnlpData(gnmiClient, port)));
            cache.put(deviceId, builder.build());
        }

        private OnlpData getOnlpData(GnmiClient gnmiClient, Port port) {
            CompletableFuture<Gnmi.GetResponse> prReq = gnmiClient.get(fieldRequest(port, "present"));
            CompletableFuture<Gnmi.GetResponse> veReq = gnmiClient.get(fieldRequest(port, "vendor"));
            CompletableFuture<Gnmi.GetResponse> snReq = gnmiClient.get(fieldRequest(port, "serial-no"));
            CompletableFuture<Gnmi.GetResponse> vpReq = gnmiClient.get(fieldRequest(port, "vendor-part"));
            CompletableFuture<Gnmi.GetResponse> ffReq = gnmiClient.get(fieldRequest(port, "form-factor"));

            return new OnlpData("sfp-" + port.number().name().replaceFirst("/[0-9]", ""),
                                value(prReq).equals("PRESENT") ? "*" : "",
                                value(veReq), value(vpReq), value(snReq), value(ffReq));
        }

        private String value(CompletableFuture<Gnmi.GetResponse> req) {
            Gnmi.GetResponse response = Futures.getUnchecked(req);
            return response.getNotificationList().isEmpty() ?
                    "" : response.getNotification(0).getUpdate(0).getVal().getStringVal().trim();
        }

        private Gnmi.GetRequest fieldRequest(Port port, String field) {
            Gnmi.Path path = Gnmi.Path.newBuilder()
                    .addElem(Gnmi.PathElem.newBuilder().setName("components").build())
                    .addElem(Gnmi.PathElem.newBuilder().setName("component").putKey("name",
                                                                                    port.number().name()).build())
                    .addElem(Gnmi.PathElem.newBuilder().setName("transceiver").build())
                    .addElem(Gnmi.PathElem.newBuilder().setName("state").build())
                    .addElem(Gnmi.PathElem.newBuilder().setName(field).build())
                    .build();
            return Gnmi.GetRequest.newBuilder()
                    .addPath(path)
                    .setType(Gnmi.GetRequest.DataType.ALL)
                    .setEncoding(Gnmi.Encoding.PROTO)
                    .build();
        }
    }

}
