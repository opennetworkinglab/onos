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
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.logicalchannelassignments.assignment.DefaultConfig;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmentconfig.AssignmentTypeEnum;

import static org.junit.Assert.assertEquals;

/**
 * UnitTest class for OpenConfigConfigOfAssignmentHandler.
 */
public class OpenConfigConfigOfAssignmentHandlerTest {
    // parent Handler
    OpenConfigAssignmentHandler parent;

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
        OpenConfigLogicalChannelsHandler logicalChannels =
            new OpenConfigLogicalChannelsHandler(terminalDevice);

        // add <channel><index>1</index></channel>
        OpenConfigChannelHandler channel =
            new OpenConfigChannelHandler(1, logicalChannels);

        // add <logical-channel-assignments></logical-channel-assignments>
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(channel);

        // add <assignment><index>2</index></assignment>
        parent = new OpenConfigAssignmentHandler(2, logicalChannelAssignments);

        // expected ResourceId set up
        rid = ResourceId.builder()
        .addBranchPointSchema("terminal-device", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("logical-channels", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("channel", "http://openconfig.net/yang/terminal-device")
        .addKeyLeaf("index", "http://openconfig.net/yang/terminal-device", 1)
        .addBranchPointSchema("logical-channel-assignments", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("assignment", "http://openconfig.net/yang/terminal-device")
        .addKeyLeaf("index", "http://openconfig.net/yang/terminal-device", 2)
        .addBranchPointSchema("config", "http://openconfig.net/yang/terminal-device")
        .build();
    }

    /**
     * UnitTest method for getModelObject.
     */
    @Test
    public void testGetModelObject() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // expected ModelObject
        DefaultConfig modelObject = new DefaultConfig();

        assertEquals("[NG]getModelObject:Return is not an expected ModelObject.\n",
                     modelObject, config.getModelObject());
    }

    /**
     * UnitTest method for setResourceId.
     */
    @Test
    public void testSetResourceId() {
        // call setResourceId
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // get resourceId
        ResourceId resourceId = null;
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("resourceId");
            field.setAccessible(true);
            resourceId = (ResourceId) field.get(config);
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
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        assertEquals("[NG]getResourceId:Return is not an expected ResourceId.\n",
                     rid, config.getResourceId());
    }

    /**
     * UnitTest method for getResourceIdBuilder.
     */
    @Test
    public void testGetResourceIdBuilder() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        assertEquals("[NG]getResourceIdBuilder:Return is not an expected ResourceIdBuilder.\n",
                     rid, config.getResourceIdBuilder().build());
    }

    /**
     * UnitTest method for addAnnotation.
     */
    @Test
    public void testAddAnnotation() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // call addAnnotation
        config.addAnnotation("name", "value");

        // get annotatedNodeInfos
        List<AnnotatedNodeInfo> annotatedNodeInfos = new ArrayList<AnnotatedNodeInfo>();
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("annotatedNodeInfos");
            field.setAccessible(true);
            annotatedNodeInfos = (List<AnnotatedNodeInfo>) field.get(config);
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
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // set list of AnnotatedNodeInfo
        config.addAnnotation("name", "value");

        assertEquals("[NG]getAnnotatedNodeInfoList:List size of AnnotatedNodeInfo is invalid.\n",
                     1, config.getAnnotatedNodeInfoList().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:ResourceId of AnnotatedNodeInfo is not an expected one.\n",
                     rid, config.getAnnotatedNodeInfoList().get(0).resourceId());
        assertEquals("[NG]getAnnotatedNodeInfoList:List size of Annotation is invalid.\n",
                     1, config.getAnnotatedNodeInfoList().get(0).annotations().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:Name of Annotation is not an expected one.\n",
                     "name", config.getAnnotatedNodeInfoList().get(0).annotations().get(0).name());
        assertEquals("[NG]getAnnotatedNodeInfoList:Value of Annotation is not an expected one.\n",
                     "value", config.getAnnotatedNodeInfoList().get(0).annotations().get(0).value());
    }

    /**
     * UnitTest method for addIndex.
     */
    @Test
    public void testAddIndex() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // call addIndex
        config.addIndex(Long.valueOf(3));

        // expected ModelObject
        DefaultConfig modelObject = new DefaultConfig();
        modelObject.index(3);

        assertEquals("[NG]addIndex:ModelObject(Index added) is not an expected one.\n",
                     modelObject, config.getModelObject());
    }

    /**
     * UnitTest method for addAssignmentType.
     */
    @Test
    public void testAddAssignmentType() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // call addAssignmentType
        config.addAssignmentType(AssignmentTypeEnum.LOGICAL_CHANNEL);

        // expected ModelObject
        DefaultConfig modelObject = new DefaultConfig();
        modelObject.assignmentType(AssignmentTypeEnum.LOGICAL_CHANNEL);

        assertEquals("[NG]addAssignmentType:ModelObject(AssignmentType added) is not an expected one.\n",
                     modelObject, config.getModelObject());
    }

    /**
     * UnitTest method for addLogicalChannel.
     */
    @Test
    public void testAddLogicalChannel() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // call addLogicalChannel
        config.addLogicalChannel("name");

        // expected ModelObject
        DefaultConfig modelObject = new DefaultConfig();
        modelObject.logicalChannel("name");

        assertEquals("[NG]addLogicalChannel:ModelObject(LogicalChannel added) is not an expected one.\n",
                     modelObject, config.getModelObject());
    }

    /**
     * UnitTest method for addAllocation.
     */
    @Test
    public void testAddAllocation() {
        // test Handler
        OpenConfigConfigOfAssignmentHandler config = new OpenConfigConfigOfAssignmentHandler(parent);

        // call addAllocation
        config.addAllocation(BigDecimal.valueOf(4));

        // expected ModelObject
        DefaultConfig modelObject = new DefaultConfig();
        modelObject.allocation(BigDecimal.valueOf(4));

        assertEquals("[NG]addAllocation:ModelObject(Allocation added) is not an expected one.\n",
                     modelObject, config.getModelObject());
    }
}
