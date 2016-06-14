/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.controller.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * Creates a ChannelPipeline for a client-side OSPF channel.
 */
public class OspfPipelineFactory implements ChannelPipelineFactory {
    private OspfInterfaceChannelHandler ospfChannelHandler;

    /**
     * Creates an instance of OSPF channel pipeline factory.
     *
     * @param ospfChannelHandler OSPF channel handler instance
     */
    public OspfPipelineFactory(OspfInterfaceChannelHandler ospfChannelHandler) {
        this.ospfChannelHandler = ospfChannelHandler;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new OspfMessageDecoder());
        pipeline.addLast("decoder", new OspfMessageEncoder());
        pipeline.addLast("handler", ospfChannelHandler);

        return pipeline;
    }
}