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

package org.onosproject.protocol.http;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.rest.RestSBDevice;

/**
 * Abstraction of an HTTP controller. Serves as a one stop shop for obtaining
 * HTTP southbound devices and (un)register listeners.
 */
public interface HttpSBController {

    /**
     * Returns all the devices known to this controller.
     *
     * @return map of devices
     */
    Map<DeviceId, RestSBDevice> getDevices();

    /**
     * Returns a device by node identifier.
     *
     * @param deviceInfo node identifier
     * @return RestSBDevice rest device
     */
    RestSBDevice getDevice(DeviceId deviceInfo);

    /**
     * Returns a device by Ip and Port.
     *
     * @param ip device ip
     * @param port device port
     * @return RestSBDevice rest device
     */
    RestSBDevice getDevice(IpAddress ip, int port);

    /**
     * Adds a device to the device map.
     *
     * @param device to be added
     */
    void addDevice(RestSBDevice device);

    /**
     * Removes the device from the devices map.
     *
     * @param deviceId to be removed
     */
    void removeDevice(DeviceId deviceId);

    /**
     * Does a HTTP POST request with specified parameters to the device.
     *
     * @param device device to make the request to
     * @param request url of the request
     * @param payload payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return status Commonly used status codes defined by HTTP
     */
    int post(DeviceId device, String request, InputStream payload, MediaType mediaType);

    /**
     * Does a HTTP PUT request with specified parameters to the device.
     *
     * @param device device to make the request to
     * @param request resource path of the request
     * @param payload payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return status Commonly used status codes defined by HTTP
     */
    int put(DeviceId device, String request, InputStream payload, MediaType mediaType);

    /**
     * Does a HTTP PATCH request with specified parameters to the device.
     *
     * @param device device to make the request to
     * @param request url of the request
     * @param payload payload of the request as an InputStream
     * @param mediaType format to retrieve the content in
     * @return status Commonly used status codes defined by HTTP
     */
    int patch(DeviceId device, String request, InputStream payload, MediaType mediaType);

    /**
     * Does a HTTP DELETE request with specified parameters to the device.
     *
     * @param device device to make the request to
     * @param request url of the request
     * @param payload payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return status Commonly used status codes defined by HTTP
     */
    int delete(DeviceId device, String request, InputStream payload, MediaType mediaType);

    /**
    *
    * Does a HTTP GET request with specified parameters to the device.
    *
    * @param device device to make the request to
    * @param request url of the request
    * @param mediaType format to retrieve the content in
    * @return an inputstream of data from the reply.
    */
    InputStream get(DeviceId device, String request, MediaType mediaType);

    /**
     * Does a HTTP POST request with specified parameters to the device and
     * extracts an object of type T from the response entity field.
     *
     * @param <T> post return type
     * @param device device to make the request to
     * @param request url of the request
     * @param payload payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @param responseClass the type of response object we are interested in,
     *            such as String, InputStream.
     * @return Object of type requested via responseClass.
     */
     <T> T post(DeviceId device, String request, InputStream payload, MediaType mediaType, Class<T> responseClass);

    /**
     * Does a HTTP GET against a Server Sent Events (SSE_INBOUND) resource on the device.
     *
     * This is a low level function that can take callbacks.
     * For a higher level function that emits events based on this callback
     * see startServerSentEvents() in the RestSBController
     *
     * @param deviceId device to make the request to
     * @param request url of the request
     * @param onEvent A consumer of inbound SSE_INBOUND events
     * @param onError A consumer of inbound SSE_INBOUND errors
     * @return status Commonly used status codes defined by HTTP
     */
     int getServerSentEvents(DeviceId deviceId, String request,
                             Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError);

    /**
     * Cancels a Server Sent Events listener to a device.
     *
     * @param deviceId device to cancel the listener for
     * @return status Commonly used status codes defined by HTTP
     */
    int cancelServerSentEvents(DeviceId deviceId);
}
