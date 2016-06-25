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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfPipelineFactory.
 */
public class OspfPipelineFactoryTest {

    private OspfPipelineFactory ospfPipelineFactory;
    private ChannelPipeline channelPipeline;

    @Before
    public void setUp() throws Exception {
        ospfPipelineFactory = new OspfPipelineFactory(new Controller(), new OspfAreaImpl(), new OspfInterfaceImpl());

    }

    @After
    public void tearDown() throws Exception {
        ospfPipelineFactory = null;
        channelPipeline = null;
    }

    /**
     * Tests getPipeline() method.
     */
    @Test
    public void testGetPipeline() throws Exception {
        channelPipeline = ospfPipelineFactory.getPipeline();
        assertThat(channelPipeline, is(notNullValue()));
    }

    /**
     * Tests releaseExternalResources() method.
     */
    @Test
    public void testReleaseExternalResources() throws Exception {
        ospfPipelineFactory.releaseExternalResources();
        assertThat(channelPipeline, is(nullValue()));
    }
}