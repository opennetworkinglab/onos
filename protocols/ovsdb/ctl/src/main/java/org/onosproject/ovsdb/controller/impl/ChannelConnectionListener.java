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
package org.onosproject.ovsdb.controller.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.onosproject.ovsdb.controller.driver.OvsdbProviderService;

/**
 * The listener class. Handles when the node disconnect.
 */
public class ChannelConnectionListener implements ChannelFutureListener {

    private final OvsdbProviderService providerService;

    /**
     * Constructor from a OvsdbProviderService providerService.
     *
     * @param providerService the providerService to use
     */
    public ChannelConnectionListener(OvsdbProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public void operationComplete(ChannelFuture arg0) {
        providerService.nodeRemoved();
    }
}
