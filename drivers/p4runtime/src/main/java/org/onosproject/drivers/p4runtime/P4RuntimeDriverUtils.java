/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * Utility class for the P4Runtime driver.
 */
final class P4RuntimeDriverUtils {

    private static final String DEVICE_ID_PARAM = "device_id=";

    private static final Logger log = LoggerFactory.getLogger(P4RuntimeDriverUtils.class);

    private P4RuntimeDriverUtils() {
        // Hide constructor.
    }

    /**
     * Returns an instance of the interpreter implementation for this device,
     * null if an interpreter cannot be retrieved.
     *
     * @param handler driver handler
     * @return interpreter or null
     */
    static PiPipelineInterpreter getInterpreter(DriverHandler handler) {
        final DeviceId deviceId = handler.data().deviceId();
        final Device device = handler.get(DeviceService.class).getDevice(deviceId);
        if (device == null) {
            log.warn("Unable to find device {}, cannot get interpreter", deviceId);
            return null;
        }
        if (!device.is(PiPipelineInterpreter.class)) {
            log.warn("Unable to get interpreter for {}, missing behaviour",
                     deviceId);
            return null;
        }
        return device.as(PiPipelineInterpreter.class);
    }

    static Long extractP4DeviceId(URI uri) {
        if (uri == null) {
            return null;
        }
        String[] segments = uri.getRawQuery().split("&");
        try {
            for (String s : segments) {
                if (s.startsWith(DEVICE_ID_PARAM)) {
                    return Long.parseUnsignedLong(
                            URLDecoder.decode(
                                    s.substring(DEVICE_ID_PARAM.length()), "utf-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to decode P4Runtime-internal device_id from URI {}: {}",
                      uri, e.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid P4Runtime-internal device_id in URI {}: {}",
                      uri, e.toString());
        }
        log.error("Missing P4Runtime-internal device_id in URI {}", uri);
        return null;
    }
}
