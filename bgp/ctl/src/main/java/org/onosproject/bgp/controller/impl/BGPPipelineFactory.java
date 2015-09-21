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
 */

package org.onosproject.bgp.controller.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Creates a ChannelPipeline for a server-side bgp channel.
 */
public class BGPPipelineFactory
    implements ChannelPipelineFactory, ExternalResourceReleasable {

    static final Timer TIMER = new HashedWheelTimer();
    protected ReadTimeoutHandler readTimeoutHandler;
    BGPControllerImpl bgpCtrlImpl;

    /**
     * Constructor to initialize the values.
     *
     * @param ctrlImpl parent ctrlImpl
     * @param isServBgp if it is a server or not
     */
    public BGPPipelineFactory(BGPControllerImpl ctrlImpl, boolean isServBgp) {
        super();
        bgpCtrlImpl = ctrlImpl;
        /* hold time*/
        readTimeoutHandler = new ReadTimeoutHandler(TIMER, bgpCtrlImpl.getConfig().getHoldTime());
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        BGPChannelHandler handler = new BGPChannelHandler(bgpCtrlImpl);

        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("bgpmessagedecoder", new BGPMessageDecoder());
        pipeline.addLast("bgpmessageencoder", new BGPMessageEncoder());
        pipeline.addLast("holdTime", readTimeoutHandler);
        pipeline.addLast("PassiveHandler", handler);

        return pipeline;
    }

    @Override
    public void releaseExternalResources() {
        TIMER.stop();
    }
}