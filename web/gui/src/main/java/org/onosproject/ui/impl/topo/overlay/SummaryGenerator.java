/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo.overlay;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * May be called upon to generate the summary messages for the topology view
 * in the GUI.
 * <p>
 * It is assumed that if a custom summary generator is installed on the server
 * (as part of a topology overlay), a peer custom summary message handler will
 * be installed on the client side to handle the messages thus generated.
 */
public interface SummaryGenerator {
    /**
     * Generates the payload for the "showSummary" message.
     *
     * @return the message payload
     */
    ObjectNode generateSummary();
}
