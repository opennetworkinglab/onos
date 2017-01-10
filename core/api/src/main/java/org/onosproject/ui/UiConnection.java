/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.ui.model.topo.UiTopoLayout;

/**
 * Abstraction of a user interface session connection.
 */
public interface UiConnection {

    /**
     * Returns the name of the logged-in user for which this connection exists.
     *
     * @return logged in user name
     */
    String userName();

    /**
     * Returns the current layout context.
     *
     * @return current topology layout
     */
    UiTopoLayout currentLayout();

    /**
     * Changes the current layout context to the specified layout.
     *
     * @param topoLayout new topology layout context
     */
    void setCurrentLayout(UiTopoLayout topoLayout);

    /**
     * Returns the current view identifier.
     *
     * @return current view
     */
    String currentView();

    /**
     * Sets the currently selected view.
     *
     * @param viewId view identifier
     */
    void setCurrentView(String viewId);

    /**
     * Sends the specified JSON message to the user interface client.
     * It is assumed that the message is already correctly formatted and
     * ready to send.
     *
     * @param message message to send
     */
    void sendMessage(ObjectNode message);

    /**
     * Composes a message into JSON and sends it to the user interface client.
     *
     * @param type    message (event) type
     * @param payload message payload
     */
    void sendMessage(String type, ObjectNode payload);

}