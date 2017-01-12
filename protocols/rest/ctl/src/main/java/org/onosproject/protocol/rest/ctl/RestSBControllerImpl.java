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

package org.onosproject.protocol.rest.ctl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.http.ctl.HttpSBControllerImpl;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The implementation of RestSBController.
 */
@Component(immediate = true)
@Service
public class RestSBControllerImpl extends HttpSBControllerImpl implements RestSBController {

    private static final Logger log =
            LoggerFactory.getLogger(RestSBControllerImpl.class);

    private final Map<DeviceId, RestSBDevice> proxiedDeviceMap = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        this.getClientMap().clear();
        this.getDeviceMap().clear();
        log.info("Stopped");
    }


    @Override
    public void addProxiedDevice(DeviceId deviceId, RestSBDevice proxy) {
        proxiedDeviceMap.put(deviceId, proxy);
        log.debug("Added device: {} to proxy {}", deviceId, proxy.deviceId());
    }

    @Override
    public void removeProxiedDevice(DeviceId deviceId) {
        log.debug("Removed device: {} from proxy {}", deviceId, proxiedDeviceMap.get(deviceId).deviceId());
        proxiedDeviceMap.remove(deviceId);
    }


    @Override
    public Set<DeviceId> getProxiedDevices(DeviceId proxyId) {
        return ImmutableSet.copyOf(
                proxiedDeviceMap.keySet().stream().filter(
                        v -> proxiedDeviceMap.get(v).deviceId().equals(proxyId)
                ).collect(Collectors.toSet()));
    }

    @Override
    public RestSBDevice getProxySBDevice(DeviceId deviceId) {
        return proxiedDeviceMap.get(deviceId);
    }

    @Override
    protected WebTarget getWebTarget(DeviceId device, String request) {
        DeviceId deviceId = device;
        if (proxiedDeviceMap.containsKey(device)) {
            deviceId = proxiedDeviceMap.get(device).deviceId();
        }
        return super.getWebTarget(deviceId, request);
    }

    @Override
    protected String getUrlString(DeviceId id, String request) {
        DeviceId deviceId = id;
        if (proxiedDeviceMap.containsKey(id)) {
            deviceId = proxiedDeviceMap.get(id).deviceId();
        }

        RestSBDevice device = super.getDeviceMap().get(deviceId);
        if (device != null) {
            if (device.url() != null && !device.url().isEmpty()) {
                return device.protocol() + super.COLON + super.DOUBLESLASH + device.ip().toString() +
                        super.COLON + device.port()
                        + device.url() + request;
            } else {
                return device.protocol() + super.COLON +
                        super.DOUBLESLASH +
                        device.ip().toString() +
                        super.COLON + device.port() + request;
            }
        } else {
            return super.getUrlString(deviceId, request);
        }

    }
}
