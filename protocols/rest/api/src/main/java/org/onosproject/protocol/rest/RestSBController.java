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

package org.onosproject.protocol.rest;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import java.io.InputStream;
import java.util.Map;

/**
 * Abstraction of an REST controller. Serves as a one stop shop for obtaining
 * Rest southbound devices and (un)register listeners.
 */
public interface RestSBController {

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
     * @param ip   device ip
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
     * Does a REST POST request with specified parameters to the device.
     *
     * @param device    device to make the request to
     * @param request   url of the request
     * @param payload   payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return true if operation returned 200, 201, 202, false otherwise
     */
    boolean post(DeviceId device, String request, InputStream payload, String mediaType);

    /**
     * Does a REST POST request with specified parameters to the device.
     *
     * @param <T>           post return type
     * @param device        device to make the request to
     * @param request       url of the request
     * @param payload       payload of the request as an InputStream
     * @param mediaType     type of content in the payload i.e. application/json
     * @param responseClass the type of response object we are interested in,
     *                      such as String, InputStream.
     * @return Object of type requested via responseClass.
     */
    <T> T post(DeviceId device, String request, InputStream payload,
               String mediaType, Class<T> responseClass);

    /**
     * Does a REST PUT request with specified parameters to the device.
     *
     * @param device    device to make the request to
     * @param request   resource path of the request
     * @param payload   payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return true if operation returned 200, 201, 202, false otherwise
     */
    boolean put(DeviceId device, String request, InputStream payload, String mediaType);

    /**
     * Does a REST GET request with specified parameters to the device.
     *
     * @param device    device to make the request to
     * @param request   url of the request
     * @param mediaType format to retrieve the content in
     * @return an inputstream of data from the reply.
     */
    InputStream get(DeviceId device, String request, String mediaType);

    /**
     * Does a REST PATCH request with specified parameters to the device.
     *
     * @param device    device to make the request to
     * @param request   url of the request
     * @param payload   payload of the request as an InputStream
     * @param mediaType format to retrieve the content in
     * @return true if operation returned 200, 201, 202, false otherwise
     */
    boolean patch(DeviceId device, String request, InputStream payload, String mediaType);

    /**
     * Does a REST DELETE request with specified parameters to the device.
     *
     * @param device    device to make the request to
     * @param request   url of the request
     * @param payload   payload of the request as an InputStream
     * @param mediaType type of content in the payload i.e. application/json
     * @return true if operation returned 200 false otherwise
     */
    boolean delete(DeviceId device, String request, InputStream payload, String mediaType);

}
