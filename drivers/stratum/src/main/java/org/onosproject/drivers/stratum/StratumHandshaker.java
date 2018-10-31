/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.stratum;

import io.grpc.StatusRuntimeException;
import org.onosproject.drivers.gnmi.GnmiHandshaker;
import org.onosproject.drivers.p4runtime.P4RuntimeHandshaker;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of DeviceHandshaker for Stratum device.
 */
public class StratumHandshaker extends AbstractHandlerBehaviour implements DeviceHandshaker {

    private static final Logger log = LoggerFactory.getLogger(StratumHandshaker.class);
    private static final int DEFAULT_DEVICE_REQ_TIMEOUT = 10;

    private P4RuntimeHandshaker p4RuntimeHandshaker;
    private GnmiHandshaker gnmiHandshaker;
    private DeviceId deviceId;

    public StratumHandshaker() {
        p4RuntimeHandshaker = new P4RuntimeHandshaker();
        gnmiHandshaker = new GnmiHandshaker();
    }

    @Override
    public void setHandler(DriverHandler handler) {
        super.setHandler(handler);
        p4RuntimeHandshaker.setHandler(handler);
        gnmiHandshaker.setHandler(handler);
    }

    @Override
    public void setData(DriverData data) {
        super.setData(data);
        p4RuntimeHandshaker.setData(data);
        gnmiHandshaker.setData(data);
        deviceId = data.deviceId();
    }

    @Override
    public CompletableFuture<Boolean> isReachable() {
        return p4RuntimeHandshaker.isReachable()
                .thenCombine(gnmiHandshaker.isReachable(), Boolean::logicalAnd);
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        p4RuntimeHandshaker.roleChanged(newRole);
        // gNMI doesn't support mastership handling.
    }

    @Override
    public MastershipRole getRole() {
        return p4RuntimeHandshaker.getRole();
    }

    @Override
    public void addDeviceAgentListener(ProviderId providerId, DeviceAgentListener listener) {
        p4RuntimeHandshaker.addDeviceAgentListener(providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(ProviderId providerId) {
        p4RuntimeHandshaker.removeDeviceAgentListener(providerId);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return p4RuntimeHandshaker.connect()
                .thenCombine(gnmiHandshaker.connect(), Boolean::logicalAnd);
    }

    @Override
    public boolean isConnected() {
        final CompletableFuture<Boolean> p4runtimeConnected =
                CompletableFuture.supplyAsync(p4RuntimeHandshaker::isConnected);
        final CompletableFuture<Boolean> gnmiConnected =
                CompletableFuture.supplyAsync(gnmiHandshaker::isConnected);

        try {
            return p4runtimeConnected
                    .thenCombine(gnmiConnected, Boolean::logicalAnd)
                    .get(DEFAULT_DEVICE_REQ_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Exception while checking connectivity on {}", data().deviceId());
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof StatusRuntimeException) {
                final StatusRuntimeException grpcError = (StatusRuntimeException) cause;
                log.warn("Error while checking connectivity on {}: {}", deviceId, grpcError.getMessage());
            } else {
                log.error("Exception while checking connectivity on {}", deviceId, e.getCause());
            }
        } catch (TimeoutException e) {
            log.error("Operation TIMEOUT while checking connectivity on {}", deviceId);
        }
        return false;
    }

    @Override
    public CompletableFuture<Boolean> disconnect() {
        return p4RuntimeHandshaker.disconnect()
                .thenCombine(gnmiHandshaker.disconnect(), Boolean::logicalAnd);
    }
}
