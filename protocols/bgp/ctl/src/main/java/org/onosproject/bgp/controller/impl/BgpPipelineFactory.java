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

package org.onosproject.bgp.controller.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.onosproject.bgp.controller.BgpController;

/**
 * Creates a ChannelPipeline for a server-side bgp channel.
 */
public class BgpPipelineFactory
    implements ChannelPipelineFactory, ExternalResourceReleasable {

    private boolean isBgpServ;
    private BgpController bgpController;

    /**
     * Constructor to initialize the values.
     *
     * @param bgpController parent controller
     * @param isBgpServ if it is a server or remote peer
     */
    public BgpPipelineFactory(BgpController bgpController, boolean isBgpServ) {
        super();
        this.isBgpServ = isBgpServ;
        this.bgpController = bgpController;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        BgpChannelHandler handler = new BgpChannelHandler(bgpController);

        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("bgpmessagedecoder", new BgpMessageDecoder());
        pipeline.addLast("bgpmessageencoder", new BgpMessageEncoder());
        if (isBgpServ) {
            pipeline.addLast("PassiveHandler", handler);
        } else {
            pipeline.addLast("ActiveHandler", handler);
        }

        return pipeline;
    }

    @Override
    public void releaseExternalResources() {
        // TODO:
    }
}