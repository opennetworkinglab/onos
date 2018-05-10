/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ovsdb.controller.driver;

import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbEventListener;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbNodeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Test Adapter for OvsdbController.
 */
public class OvsdbControllerAdapter implements OvsdbController {
    protected ConcurrentHashMap<OvsdbNodeId, OvsdbClientServiceAdapter> ovsdbClients =
            new ConcurrentHashMap<>();

    @Override
    public void addNodeListener(OvsdbNodeListener listener) {

    }

    @Override
    public void removeNodeListener(OvsdbNodeListener listener) {

    }

    @Override
    public void addOvsdbEventListener(OvsdbEventListener listener) {

    }

    @Override
    public void removeOvsdbEventListener(OvsdbEventListener listener) {

    }

    @Override
    public List<OvsdbNodeId> getNodeIds() {
        long port = 6653;
        return new ArrayList<>(Collections.singletonList(
                new OvsdbNodeId(IpAddress.valueOf("127.0.0.1"), port)));
    }

    @Override
    public OvsdbClientService getOvsdbClient(OvsdbNodeId nodeId) {
        return ovsdbClients.get(nodeId);
    }

    @Override
    public void connect(IpAddress ip, TpPort port) {

    }

    @Override
    public void connect(IpAddress ip, TpPort port, Consumer<Exception> failhandler) {

    }

    @Override
    public void setServerMode(boolean serverMode) {

    }
}
