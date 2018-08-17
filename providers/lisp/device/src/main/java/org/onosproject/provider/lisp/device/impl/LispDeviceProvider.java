/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.provider.lisp.device.impl;

import com.google.common.base.Preconditions;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import org.onlab.packet.ChassisId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.lisp.ctl.LispController;
import org.onosproject.lisp.ctl.LispRouterId;
import org.onosproject.lisp.ctl.LispRouterListener;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provider which uses an LISP controller to detect device.
 */
@Component(immediate = true)
public class LispDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger log = LoggerFactory.getLogger(LispDeviceProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LispController controller;

    private static final String APP_NAME = "org.onosproject.lisp";
    private static final String SCHEME_NAME = "lisp";
    private static final String DEVICE_PROVIDER_PACKAGE = "org.onosproject.lisp.provider.device";

    private static final String MANUFACTURER = "IETF";
    private static final String HARDWARE_VERSION = "LISP Reference Router";
    private static final String SOFTWARE_VERSION = "1.0";
    private static final String SERIAL_NUMBER = "unknown";
    private static final String IS_NULL_MSG = "LISP device info is null";
    private static final String IPADDRESS = "ipaddress";
    private static final String LISP = "lisp";

    protected DeviceProviderService providerService;
    private InternalLispRouterListener routerListener = new InternalLispRouterListener();

    private ApplicationId appId;

    /**
     * Creates a LISP device provider.
     */
    public LispDeviceProvider() {
        super(new ProviderId(SCHEME_NAME, DEVICE_PROVIDER_PACKAGE));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        controller.addRouterListener(routerListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.getRouters().forEach(router -> controller.disconnectRouter(
                            new LispRouterId(router.routerId()), true));
        controller.removeRouterListener(routerListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        log.info("Triggering probe on device {}", deviceId);
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO: need to provide a way to send probe message to LISP router,
        // to check the device reachability.
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        log.info("This operation is irrelevant for LISP router");
    }

    /**
     * Adds a LISP router into device store.
     */
    private void connectDevice(LispRouterId routerId) {
        DeviceId deviceId = getDeviceId(routerId.id().toString());
        Preconditions.checkNotNull(deviceId, IS_NULL_MSG);

        // formulate LISP router object
        ChassisId cid = new ChassisId();
        String ipAddress = routerId.id().toString();
        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(IPADDRESS, ipAddress)
                .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                .build();
        DeviceDescription deviceDescription = new DefaultDeviceDescription(
                deviceId.uri(),
                Device.Type.ROUTER,
                MANUFACTURER, HARDWARE_VERSION,
                SOFTWARE_VERSION, SERIAL_NUMBER,
                cid, false,
                annotations);
        if (deviceService.getDevice(deviceId) == null) {
            providerService.deviceConnected(deviceId, deviceDescription);
        }
        checkAndUpdateDevice(deviceId, deviceDescription);
    }

    /**
     * Checks whether a specified device is available.
     *
     * @param deviceId          device identifier
     * @param deviceDescription device description
     */
    private void checkAndUpdateDevice(DeviceId deviceId, DeviceDescription deviceDescription) {
        if (deviceService.getDevice(deviceId) == null) {
            log.warn("LISP router {} has not been added to store", deviceId);
        } else {
            boolean isReachable = isReachable(deviceId);
            if (isReachable && !deviceService.isAvailable(deviceId)) {
                // TODO: handle the mastership logic
            } else if (!isReachable && deviceService.isAvailable(deviceId)) {
                providerService.deviceDisconnected(deviceId);
            }
        }
    }

    /**
     * Listener for LISP router events.
     */
    private class InternalLispRouterListener implements LispRouterListener {

        @Override
        public void routerAdded(LispRouterId routerId) {
            connectDevice(routerId);
            log.debug("LISP router {} added to core.", routerId);
        }

        @Override
        public void routerRemoved(LispRouterId routerId) {
            Preconditions.checkNotNull(routerId, IS_NULL_MSG);

            DeviceId deviceId = getDeviceId(routerId.id().toString());
            if (deviceService.getDevice(deviceId) != null) {
                providerService.deviceDisconnected(deviceId);
                log.debug("LISP router {} removed from LISP controller", deviceId);
            } else {
                log.warn("LISP router {} does not exist in the store, " +
                         "or it may already have been removed", deviceId);
            }
        }

        @Override
        public void routerChanged(LispRouterId routerId) {

        }
    }

    /**
     * Obtains the DeviceId contains IP address of LISP router.
     *
     * @param ip IP address
     * @return DeviceId device identifier
     */
    private DeviceId getDeviceId(String ip) {
        try {
            return DeviceId.deviceId(new URI(LISP, ip, null));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to build deviceID for device "
                    + ip, e);
        }
    }
}
