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
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import javax.servlet.http.HttpServletRequest;

/**
 * Web socket servlet capable of creating various sockets for the user interface.
 */
public class GuiWebSocketServlet extends WebSocketServlet {

    private ServiceDirectory directory = new DefaultServiceDirectory();

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {

        return new TopologyWebSocket(directory);
    }

}
