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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisProcess;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for IsisPipelineFactory.
 */
public class IsisPipelineFactoryTest {

    private IsisPipelineFactory isisPipelineFactory;
    private IsisChannelHandler isisChannelHandler;
    private List<IsisProcess> isisProcessList = new ArrayList<>();
    private Controller controller;
    private ChannelPipeline channelPipeline;
    private DefaultIsisProcess isisProcess;
    private String processId = "1";
    private DefaultIsisInterface isisInterface;
    private List<IsisInterface> isisInterfaces = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        controller = new Controller();
        isisProcess = new DefaultIsisProcess();
        isisInterface = new DefaultIsisInterface();
        isisInterfaces.add(isisInterface);
        isisProcess.setIsisInterfaceList(isisInterfaces);
        isisProcess.setProcessId(processId);
        isisProcessList.add(isisProcess);
        isisChannelHandler = new IsisChannelHandler(controller, isisProcessList);
        isisPipelineFactory = new IsisPipelineFactory(isisChannelHandler);
    }

    @After
    public void tearDown() throws Exception {
        controller = null;
        isisChannelHandler = null;
        isisPipelineFactory = null;
    }

    /**
     * Tests getPipeline() method.
     */
    @Test
    public void testGetPipeline() throws Exception {
        channelPipeline = isisPipelineFactory.getPipeline();
        assertThat(channelPipeline, is(instanceOf(ChannelPipeline.class)));
    }
}