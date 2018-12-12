/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.utils.openconfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.channel.DefaultConfig;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.DefaultChannel;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.DefaultLogicalChannelAssignments;

import static org.junit.Assert.assertEquals;

/**
 * UnitTest class for OpenConfigChannelHandler.
 */
public class OpenConfigChannelHandlerTest {
    // parent Handler
    OpenConfigLogicalChannelsHandler parent;

    // expected ResourceId
    ResourceId rid;

    /**
     * Set up method for UnitTest.
     */
    @Before
    public void setUp() {
        // parent Handler set up
        // create <terminal-device xmlns="http://openconfig.net/yang/terminal-device">
        //        </terminal-device>
        OpenConfigTerminalDeviceHandler terminalDevice = new OpenConfigTerminalDeviceHandler();

        // add <logical-channels></logical-channels>
        parent = new OpenConfigLogicalChannelsHandler(terminalDevice);

        // expected ResourceId set up
        rid = ResourceId.builder()
        .addBranchPointSchema("terminal-device", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("logical-channels", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("channel", "http://openconfig.net/yang/terminal-device")
        .addKeyLeaf("index", "http://openconfig.net/yang/terminal-device", 1)
        .build();
    }

    /**
     * UnitTest method for getModelObject.
     */
    @Test
    public void testGetModelObject() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // expected ModelObject
        DefaultChannel modelObject = new DefaultChannel();
        modelObject.index(1);

        assertEquals("[NG]getModelObject:Return is not an expected ModelObject.\n",
                     modelObject, channel.getModelObject());
    }

    /**
     * UnitTest method for setResourceId.
     */
    @Test
    public void testSetResourceId() {
        // call setResourceId
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // get resourceId
        ResourceId resourceId = null;
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("resourceId");
            field.setAccessible(true);
            resourceId = (ResourceId) field.get(channel);
        } catch (NoSuchFieldException e) {
            Assert.fail("[NG]setResourceId:ResourceId does not exist.\n" + e);
        } catch (IllegalAccessException e) {
            Assert.fail("[NG]setResourceId:Access to ResourceId is illegal.\n" + e);
        }

        assertEquals("[NG]setResourceId:Set ResourceId is not an expected one.\n",
                     rid, resourceId);
    }

    /**
     * UnitTest method for getResourceId.
     */
    @Test
    public void testGetResourceId() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        assertEquals("[NG]getResourceId:Return is not an expected ResourceId.\n",
                     rid, channel.getResourceId());
    }

    /**
     * UnitTest method for getResourceIdBuilder.
     */
    @Test
    public void testGetResourceIdBuilder() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        assertEquals("[NG]getResourceIdBuilder:Return is not an expected ResourceIdBuilder.\n",
                     rid, channel.getResourceIdBuilder().build());
    }

    /**
     * UnitTest method for addAnnotation.
     */
    @Test
    public void testAddAnnotation() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // call addAnnotation
        channel.addAnnotation("name", "value");

        // get annotatedNodeInfos
        List<AnnotatedNodeInfo> annotatedNodeInfos = new ArrayList<AnnotatedNodeInfo>();
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("annotatedNodeInfos");
            field.setAccessible(true);
            annotatedNodeInfos = (List<AnnotatedNodeInfo>) field.get(channel);
        } catch (NoSuchFieldException e) {
            Assert.fail("[NG]addAnnotation:List of AnnotatedNodeInfo does not exist.\n" + e);
        } catch (IllegalAccessException e) {
            Assert.fail("[NG]addAnnotation:Access to list of AnnotatedNodeInfo is illegal.\n" + e);
        }

        assertEquals("[NG]addAnnotation:List size of AnnotatedNodeInfo is invalid.\n",
                     1, annotatedNodeInfos.size());
        assertEquals("[NG]addAnnotation:ResourceId of AnnotatedNodeInfo is not an expected one.\n",
                     rid, annotatedNodeInfos.get(0).resourceId());
        assertEquals("[NG]addAnnotation:List size of Annotation is invalid.\n",
                     1, annotatedNodeInfos.get(0).annotations().size());
        assertEquals("[NG]addAnnotation:Name of Annotation is not an expected one.\n",
                     "name", annotatedNodeInfos.get(0).annotations().get(0).name());
        assertEquals("[NG]addAnnotation:Value of Annotation is not an expected one.\n",
                     "value", annotatedNodeInfos.get(0).annotations().get(0).value());
    }

    /**
     * UnitTest method for getAnnotatedNodeInfoList.
     */
    @Test
    public void testGetAnnotatedNodeInfoList() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // set list of AnnotatedNodeInfo
        channel.addAnnotation("name", "value");

        assertEquals("[NG]getAnnotatedNodeInfoList:List size of AnnotatedNodeInfo is invalid.\n",
                     1, channel.getAnnotatedNodeInfoList().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:ResourceId of AnnotatedNodeInfo is not an expected one.\n",
                     rid, channel.getAnnotatedNodeInfoList().get(0).resourceId());
        assertEquals("[NG]getAnnotatedNodeInfoList:List size of Annotation is invalid.\n",
                     1, channel.getAnnotatedNodeInfoList().get(0).annotations().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:Name of Annotation is not an expected one.\n",
                     "name", channel.getAnnotatedNodeInfoList().get(0).annotations().get(0).name());
        assertEquals("[NG]getAnnotatedNodeInfoList:Value of Annotation is not an expected one.\n",
                     "value", channel.getAnnotatedNodeInfoList().get(0).annotations().get(0).value());
    }

    /**
     * UnitTest method for addConfig.
     */
    @Test
    public void testAddConfig() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // call addConfig
        OpenConfigConfigOfChannelHandler configOfChannel =
            new OpenConfigConfigOfChannelHandler(channel);

        // expected ModelObject
        DefaultChannel modelObject = new DefaultChannel();
        modelObject.index(1);
        DefaultConfig config = new DefaultConfig();
        modelObject.config(config);

        assertEquals("[NG]addConfig:ModelObject(Config added) is not an expected one.\n",
                     modelObject, channel.getModelObject());
    }

    /**
     * UnitTest method for addLogicalChannelAssignments.
     */
    @Test
    public void testAddLogicalChannelAssignments() {
        // test Handler
        OpenConfigChannelHandler channel = new OpenConfigChannelHandler(1, parent);

        // call addLogicalChannelAssignments
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(channel);

        // expected ModelObject
        DefaultChannel modelObject = new DefaultChannel();
        modelObject.index(1);
        DefaultLogicalChannelAssignments logicalChannel = new DefaultLogicalChannelAssignments();
        modelObject.logicalChannelAssignments(logicalChannel);

        assertEquals(
        "[NG]addLogicalChannelAssignments:ModelObject(LogicalChannelAssignments added) is not an expected one.\n",
        modelObject, channel.getModelObject());
    }
}
