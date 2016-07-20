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

package org.onosproject.pcep.controller.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * Creates a ChannelPipeline for a server-side pcep channel.
 */
public class PcepPipelineFactory
    implements ChannelPipelineFactory, ExternalResourceReleasable {

    protected Controller controller;
    static final Timer TIMER = new HashedWheelTimer();
    protected IdleStateHandler idleHandler;
    protected ReadTimeoutHandler readTimeoutHandler;
    static final int DEFAULT_KEEP_ALIVE_TIME = 30;
    static final int DEFAULT_DEAD_TIME = 120;
    static final int DEFAULT_WAIT_TIME = 60;

    public PcepPipelineFactory(Controller controller) {
        super();
        this.controller = controller;
        this.idleHandler = new IdleStateHandler(TIMER, DEFAULT_DEAD_TIME, DEFAULT_KEEP_ALIVE_TIME, 0);
        this.readTimeoutHandler = new ReadTimeoutHandler(TIMER, DEFAULT_WAIT_TIME);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        PcepChannelHandler handler = new PcepChannelHandler(controller);

        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("pcepmessagedecoder", new PcepMessageDecoder());
        pipeline.addLast("pcepmessageencoder", new PcepMessageEncoder());
        pipeline.addLast("idle", idleHandler);
        pipeline.addLast("waittimeout", readTimeoutHandler);
        pipeline.addLast("handler", handler);
        return pipeline;
    }

    @Override
    public void releaseExternalResources() {
        TIMER.stop();
    }
}
