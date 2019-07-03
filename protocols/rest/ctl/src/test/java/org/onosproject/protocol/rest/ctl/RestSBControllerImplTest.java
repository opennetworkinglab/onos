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

package org.onosproject.protocol.rest.ctl;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.protocol.rest.RestSBEventListener;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Basic testing for RestSBController.
 */
public class RestSBControllerImplTest extends JerseyTest {
    private static final String SAMPLE_PAYLOAD = "{ \"msg\": \"ONOS Rocks!\" }";

    RestSBControllerImpl controller;

    RestSBDevice device1;
    RestSBDevice device2;

    /**
     * Mockup of an arbitrary device.
     */
    @Path("testme")
    public static class HelloResource {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response post(InputStream payload) throws IOException {
            String responseText = IOUtils.toString(payload, StandardCharsets.UTF_8);
            if (responseText.equalsIgnoreCase(SAMPLE_PAYLOAD)) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }

        @POST
        @Path("testpostreturnstring")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response postReturnString(InputStream payload) throws IOException {
            String responseText = IOUtils.toString(payload, StandardCharsets.UTF_8);
            if (responseText.equalsIgnoreCase(SAMPLE_PAYLOAD)) {
                return Response.ok().entity("OK").build();
            }
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Failed").build();
        }

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response put(InputStream payload) throws IOException {
            String responseText = IOUtils.toString(payload, StandardCharsets.UTF_8);
            if (responseText.equalsIgnoreCase(SAMPLE_PAYLOAD)) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }

        @PATCH
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response patch(InputStream payload) throws IOException {
            String responseText = IOUtils.toString(payload, StandardCharsets.UTF_8);
            if (responseText.equalsIgnoreCase(SAMPLE_PAYLOAD)) {
                return Response.ok().build();
            }
            return Response.status(Response.Status.EXPECTATION_FAILED).build();
        }

        @GET
        public String getHello() {
            return SAMPLE_PAYLOAD;
        }

        @DELETE
        public int delete() {
            return Response.Status.NO_CONTENT.getStatusCode();
        }

        @GET
        @Path("server-sent-events")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context SseEventSink eventSink, @Context Sse sse) throws InterruptedException {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        // ... code that waits 0.1 second
                            Thread.sleep(100L);
                        final OutboundSseEvent event = sse.newEventBuilder()
                                .id(String.valueOf(i))
                                .name("message-to-rest-sb")
                                .data(String.class, "Test message " + i + "!")
                                .build();
                        eventSink.send(event);
                        System.out.println("Message " + i + " sent");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

    @Override
    protected Application configure() {
        set(TestProperties.CONTAINER_PORT, 18080);
        return new ResourceConfig(HelloResource.class);
    }

    @Before
    public void setUpTest() {
        controller = new RestSBControllerImpl();
        TestUtils.setField(controller, "eventDispatcher", new TestEventDispatcher());
        controller.activate();
        device1 = new DefaultRestSBDevice(IpAddress.valueOf("127.0.0.1"), 18080, "foo", "bar", "http", null, true);
        device2 = new DefaultRestSBDevice(IpAddress.valueOf("127.0.0.2"), 18080, "foo1", "bar2", "http", null, true);
        controller.addDevice(device1);

    }

    @Test
    public void basics() {
        assertTrue("Device1 non added", controller.getDevices().containsValue(device1));
        assertEquals("Device1 added but with wrong key", controller.getDevices()
                .get(device1.deviceId()), device1);
        assertEquals("Incorrect Get Device by ID", controller.getDevice(device1.deviceId()), device1);
        assertEquals("Incorrect Get Device by IP, Port", controller.getDevice(device1.ip(), device1.port()), device1);
        controller.addDevice(device2);
        assertTrue("Device2 non added", controller.getDevices().containsValue(device2));
        controller.removeDevice(device2.deviceId());
        assertFalse("Device2 not removed", controller.getDevices().containsValue(device2));
    }

    /**
     * Tests the post function of the REST SB Controller.
     */
    @Test
    public void testPost() {
        InputStream payload = new ByteArrayInputStream(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        int response = controller.post(device1.deviceId(), "/testme", payload, MediaType.APPLICATION_JSON_TYPE);
        assertEquals(HttpURLConnection.HTTP_OK, response);
    }

    /**
     * Tests the put function of the REST SB Controller.
     */
    @Test
    public void testPut() {
        InputStream payload = new ByteArrayInputStream(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        int response = controller.put(device1.deviceId(), "/testme", payload, MediaType.APPLICATION_JSON_TYPE);
        assertEquals(HttpURLConnection.HTTP_OK, response);
    }

    @Test
    public void testPatch() {
        InputStream payload = new ByteArrayInputStream(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        int response = controller.patch(device1.deviceId(), "/testme", payload, MediaType.APPLICATION_JSON_TYPE);
        assertEquals(HttpURLConnection.HTTP_OK, response);
    }

    /**
     * Tests the delete function of the REST SB Controller.
     */
    @Test
    public void testDelete() {
        int response = controller.delete(device1.deviceId(), "/testme", null, null);
        assertEquals(HttpURLConnection.HTTP_OK, response);
    }

    /**
     * Tests the get function of the REST SB Controller.
     */
    @Test
    public void testGet() throws IOException {
        InputStream payload = controller.get(device1.deviceId(), "/testme", MediaType.APPLICATION_JSON_TYPE);
        String responseText = IOUtils.toString(payload, StandardCharsets.UTF_8);
        assertEquals(SAMPLE_PAYLOAD, responseText);
    }

    /**
     * Tests the post function of the REST SB Controller.
     */
    @Test
    public void testPostReturnString() {
        InputStream payload = new ByteArrayInputStream(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        String result = controller.post(device1.deviceId(), "/testme/testpostreturnstring",
                payload, MediaType.APPLICATION_JSON_TYPE, String.class);
        assertEquals("OK", result);
    }

    /**
     * Tests the low level getServerSentEvents function of the REST SB Controller.
     *
     * Note: If the consumer throws an error it will not be propagated back up
     * to here - instead the source will go in to error and no more callbacks
     * will be executed
     */
    @Test
    public void testGetServerSentEvents() {
        Consumer<InboundSseEvent> sseEventConsumer = (event) -> {
            System.out.println("ServerSentEvent received: " + event);
            assertEquals("message-to-rest-sb", event.getName());
            // Just to show it works we stop before the last message is sent
            if (Integer.parseInt(event.getId()) == 8) {
                controller.cancelServerSentEvents(device1.deviceId());
            }
        };

        Consumer<Throwable> sseError = (error) -> {
            System.err.println(error);
            controller.cancelServerSentEvents(device1.deviceId());
            //fail(error.toString()); //Does nothing as it's in lambda scope
        };

        int response = controller.getServerSentEvents(device1.deviceId(),
                "/testme/server-sent-events",
                sseEventConsumer,
                sseError
        );
        assertEquals(204, response);
    }

    /**
     * Test of cancelling of events from a device - in this case there should not be any.
     */
    @Test
    public void testCancelServerSentEvents() {
        assertEquals(404, controller.cancelServerSentEvents(device1.deviceId()));
    }

    /**
     * Test the high level API for Server Sent Events.
     */
    @Test
    public void testStartServerSentEvents() {
        AtomicInteger listener1Count = new AtomicInteger();
        AtomicInteger listener2Count = new AtomicInteger();

        RestSBEventListener listener1 = event -> {
            System.out.println("Event on Lsnr1: " + event);
            listener1Count.incrementAndGet();
            if (Integer.parseInt(event.getId()) == 8) {
                controller.cancelServerSentEvents(device1.deviceId());
            }
        };

        RestSBEventListener listener2 = event -> {
            listener2Count.incrementAndGet();
            System.out.println("Event on Lsnr2: " + event);
        };

        controller.addListener(listener1);
        controller.addListener(listener2);

        controller.startServerSentEvents(device1.deviceId(), "/testme/server-sent-events");

        controller.removeListener(listener1);
        controller.removeListener(listener2);

        assertEquals(9, listener1Count.get());
        assertEquals(9, listener2Count.get());
    }
}