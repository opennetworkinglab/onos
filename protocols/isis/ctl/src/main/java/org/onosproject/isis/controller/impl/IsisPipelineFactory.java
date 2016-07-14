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
package org.onosproject.isis.controller.impl;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * Creates a ChannelPipeline for a client-side ISIS channel.
 */
public class IsisPipelineFactory implements ChannelPipelineFactory {
    private IsisChannelHandler isisChannelHandler;

    /**
     * Creates an instance of ISIS channel pipeline factory.
     *
     * @param isisChannelHandler ISIS channel handler instance
     */
    public IsisPipelineFactory(IsisChannelHandler isisChannelHandler) {
        this.isisChannelHandler = isisChannelHandler;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new IsisMessageDecoder());
        pipeline.addLast("decoder", new IsisMessageEncoder());
        pipeline.addLast("handler", isisChannelHandler);

        return pipeline;
    }
}