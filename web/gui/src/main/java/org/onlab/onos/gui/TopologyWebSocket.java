/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.gui;

import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.net.topology.TopologyVertex;
import org.onlab.osgi.ServiceDirectory;

import java.io.IOException;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyWebSocket implements WebSocket.OnTextMessage, TopologyListener {

    private final ServiceDirectory directory;
    private final TopologyService topologyService;
    private final DeviceService deviceService;

    private Connection connection;

    /**
     * Creates a new web-socket for serving data to GUI topology view.
     *
     * @param directory service directory
     */
    public TopologyWebSocket(ServiceDirectory directory) {
        this.directory = directory;
        topologyService = directory.get(TopologyService.class);
        deviceService = directory.get(DeviceService.class);
    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;

        // Register for topology events...
        if (topologyService != null && deviceService != null) {
            topologyService.addListener(this);

            sendMessage("Yo!!!");

            Topology topology = topologyService.currentTopology();
            TopologyGraph graph = topologyService.getGraph(topology);
            for (TopologyVertex vertex : graph.getVertexes()) {
                sendMessage(deviceService.getDevice(vertex.deviceId()).toString());
            }

            for (TopologyEdge edge : graph.getEdges()) {
                sendMessage(edge.link().toString());
            }

            sendMessage("That's what we're starting with...");

        } else {
            sendMessage("No topology service!!!");
        }
    }

    @Override
    public void onClose(int closeCode, String message) {
        TopologyService topologyService = directory.get(TopologyService.class);
        if (topologyService != null) {
            topologyService.removeListener(this);
        }
    }

    @Override
    public void onMessage(String data) {
        System.out.println("Received: " + data);
    }

    public void sendMessage(String data) {
        try {
            connection.sendMessage(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void event(TopologyEvent event) {
        sendMessage(event.toString());
    }
}

