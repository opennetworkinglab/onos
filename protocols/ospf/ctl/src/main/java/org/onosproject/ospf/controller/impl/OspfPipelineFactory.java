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
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;

/**
 * Creates a ChannelPipeline for a server-side OSPF channel.
 */
public class OspfPipelineFactory implements ChannelPipelineFactory, ExternalResourceReleasable {

    private static final Timer TIMER = new HashedWheelTimer();
    private Controller controller;
    private ReadTimeoutHandler readTimeoutHandler;
    private OspfArea ospfArea;
    private OspfInterface ospfInterface;
    private int holdTime = 120 * 1000;

    /**
     * Creates an instance of OSPF pipeline factory.
     *
     * @param controller    controller instance.
     * @param ospfArea      OSPF area instance.
     * @param ospfInterface OSPF interface instance.
     */
    public OspfPipelineFactory(Controller controller, OspfArea ospfArea, OspfInterface ospfInterface) {
        super();
        this.controller = controller;
        this.ospfArea = ospfArea;
        this.ospfInterface = ospfInterface;
        readTimeoutHandler = new ReadTimeoutHandler(TIMER, holdTime);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        OspfInterfaceChannelHandler interfaceHandler = new OspfInterfaceChannelHandler(
                controller, ospfArea, ospfInterface);

        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new OspfMessageEncoder(ospfInterface));
        pipeline.addLast("decoder", new OspfMessageDecoder());
        pipeline.addLast("holdTime", readTimeoutHandler);
        pipeline.addLast("interfacehandler", interfaceHandler);

        return pipeline;
    }

    @Override
    public void releaseExternalResources() {
        TIMER.stop();
    }

}