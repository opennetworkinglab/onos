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
package org.onosproject.protocol.restconf.ctl;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.client.ChunkedInput;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.protocol.http.ctl.HttpSBControllerImpl;
import org.onosproject.protocol.rest.RestSBDevice;
import org.onosproject.protocol.restconf.RestConfNotificationEventListener;
import org.onosproject.protocol.restconf.RestConfSBController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of RestConfSBController.
 */
@Component(immediate = true)
@Service
public class RestConfSBControllerImpl extends HttpSBControllerImpl
        implements RestConfSBController {

    private static final Logger log = LoggerFactory
            .getLogger(RestConfSBControllerImpl.class);

    // TODO: for the Ibis release when both RESTCONF server and RESTCONF client
    // fully support root resource discovery, ROOT_RESOURCE constant will be
    // removed and rather the value would get discovered dynamically.
    private static final String ROOT_RESOURCE = "/onos/restconf";

    private static final String RESOURCE_PATH_PREFIX = "/data/";
    private static final String NOTIFICATION_PATH_PREFIX = "/data/";

    private Map<DeviceId, RestConfNotificationEventListener>
                                            restconfNotificationListenerMap = new ConcurrentHashMap<>();
    private Map<DeviceId, GetChunksRunnable> runnableTable = new ConcurrentHashMap<>();

    ExecutorService executor = Executors.newCachedThreadPool();

    @Activate
    public void activate() {
        log.info("RESTCONF SBI Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("RESTCONF SBI Stopped");
        executor.shutdown();
        this.getClientMap().clear();
        this.getDeviceMap().clear();
    }

    @Override
    public Map<DeviceId, RestSBDevice> getDevices() {
        log.trace("RESTCONF SBI::getDevices");
        return super.getDevices();
    }

    @Override
    public RestSBDevice getDevice(DeviceId deviceInfo) {
        log.trace("RESTCONF SBI::getDevice with deviceId");
        return super.getDevice(deviceInfo);
    }

    @Override
    public RestSBDevice getDevice(IpAddress ip, int port) {
        log.trace("RESTCONF SBI::getDevice with ip and port");
        return super.getDevice(ip, port);
    }

    @Override
    public void addDevice(RestSBDevice device) {
        log.trace("RESTCONF SBI::addDevice");
        super.addDevice(device);
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        log.trace("RESTCONF SBI::removeDevice");
        super.removeDevice(deviceId);
    }

    @Override
    public boolean post(DeviceId device, String request, InputStream payload,
                        String mediaType) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.post(device, request, payload, mediaType);
    }

    @Override
    public <T> T post(DeviceId device, String request, InputStream payload,
                      String mediaType, Class<T> responseClass) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.post(device, request, payload, mediaType, responseClass);
    }

    @Override
    public boolean put(DeviceId device, String request, InputStream payload,
                       String mediaType) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.put(device, request, payload, mediaType);
    }

    @Override
    public InputStream get(DeviceId device, String request, String mediaType) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.get(device, request, mediaType);
    }

    @Override
    public boolean patch(DeviceId device, String request, InputStream payload,
                         String mediaType) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.patch(device, request, payload, mediaType);
    }

    @Override
    public boolean delete(DeviceId device, String request, InputStream payload,
                          String mediaType) {
        request = discoverRootResource(device) + RESOURCE_PATH_PREFIX
                + request;
        return super.delete(device, request, payload, mediaType);
    }

    @Override
    public void enableNotifications(DeviceId device, String request,
                                 String mediaType,
                                 RestConfNotificationEventListener listener) {

        request = discoverRootResource(device) + NOTIFICATION_PATH_PREFIX
                + request;

        addNotificationListener(device, listener);

        GetChunksRunnable runnable = new GetChunksRunnable(request, mediaType,
                                                           device);
        runnableTable.put(device, runnable);
        executor.execute(runnable);
    }

    public void stopNotifications(DeviceId device) {

        runnableTable.get(device).terminate();
        runnableTable.remove(device);
        removeNotificationListener(device);
        log.debug("Stop sending notifications for device URI: " + device.uri().toString());

    }

    public class GetChunksRunnable implements Runnable {
        private String request;
        private String mediaType;
        private DeviceId device;

        private volatile boolean running = true;

        public void terminate() {
            running = false;
        }

        /**
         * @param request
         * @param mediaType
         * @param device
         */
        public GetChunksRunnable(String request, String mediaType,
                                 DeviceId device) {
            this.request = request;
            this.mediaType = mediaType;
            this.device = device;
        }

        @Override
        public void run() {
            WebTarget wt = getWebTarget(device, request);
            Response clientResp = wt.request(mediaType).get();
            RestConfNotificationEventListener listener = restconfNotificationListenerMap
                    .get(device);
            final ChunkedInput<String> chunkedInput = (ChunkedInput<String>) clientResp
                    .readEntity(new GenericType<ChunkedInput<String>>() {
                    });

            String chunk;
            // Note that the read() is a blocking operation and the invoking
            // thread is blocked until a new chunk comes. Jersey implementation
            // of this IO operation is in a way that it does not respond to
            // interrupts.
            while (running) {
                chunk = chunkedInput.read();
                if (chunk != null) {
                    if (running) {
                        listener.handleNotificationEvent(device, chunk);
                    } else {
                        log.trace("the requesting client is no more interested "
                                + "to receive such notifications.");
                    }
                } else {
                    log.trace("The received notification chunk is null. do not continue any more.");
                    break;
                }
            }
            log.trace("out of while loop -- end of run");
        }
    }

    public String discoverRootResource(DeviceId device) {
        // FIXME: send a GET command to the device to discover the root resource.
        // The plan to fix this is for the Ibis release when the RESTCONF server and
        // the RESTCONF client both support root resource discovery.
        return ROOT_RESOURCE;
    }

    @Override
    public void addNotificationListener(DeviceId deviceId,
                                        RestConfNotificationEventListener listener) {
        if (!restconfNotificationListenerMap.containsKey(deviceId)) {
            this.restconfNotificationListenerMap.put(deviceId, listener);
        }
    }

    @Override
    public void removeNotificationListener(DeviceId deviceId) {
        this.restconfNotificationListenerMap.remove(deviceId);
    }

}
