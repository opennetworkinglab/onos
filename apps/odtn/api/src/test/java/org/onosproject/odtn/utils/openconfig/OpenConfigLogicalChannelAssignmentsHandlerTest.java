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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.DefaultLogicalChannelAssignments;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.logicalchannelassignments.DefaultAssignment;

import static org.junit.Assert.assertEquals;

/**
 * UnitTest class for OpenConfigLogicalChannelAssignmentsHandler.
 */
public class OpenConfigLogicalChannelAssignmentsHandlerTest {
    // parent Handler
    OpenConfigChannelHandler parent;

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
        parent = new OpenConfigChannelHandler(1, logicalChannels);

        // expected ResourceId set up
        rid = ResourceId.builder()
        .addBranchPointSchema("terminal-device", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("logical-channels", "http://openconfig.net/yang/terminal-device")
        .addBranchPointSchema("channel", "http://openconfig.net/yang/terminal-device")
        .addKeyLeaf("index", "http://openconfig.net/yang/terminal-device", 1)
        .addBranchPointSchema("logical-channel-assignments", "http://openconfig.net/yang/terminal-device")
        .build();
    }

    /**
     * UnitTest method for getModelObject.
     */
    @Test
    public void testGetModelObject() {
        // test Handler
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        // expected ModelObject
        DefaultLogicalChannelAssignments modelObject = new DefaultLogicalChannelAssignments();

        assertEquals("[NG]getModelObject:Return is not an expected ModelObject.\n",
                     modelObject, logicalChannelAssignments.getModelObject());
    }

    /**
     * UnitTest method for setResourceId.
     */
    @Test
    public void testSetResourceId() {
        // call setResourceId
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        // get resourceId
        ResourceId resourceId = null;
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("resourceId");
            field.setAccessible(true);
            resourceId = (ResourceId) field.get(logicalChannelAssignments);
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
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        assertEquals("[NG]getResourceId:Return is not an expected ResourceId.\n",
                     rid, logicalChannelAssignments.getResourceId());
    }

    /**
     * UnitTest method for getResourceIdBuilder.
     */
    @Test
    public void testGetResourceIdBuilder() {
        // test Handler
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        assertEquals("[NG]getResourceIdBuilder:Return is not an expected ResourceIdBuilder.\n",
                     rid, logicalChannelAssignments.getResourceIdBuilder().build());
    }

    /**
     * UnitTest method for addAnnotation.
     */
    @Test
    public void testAddAnnotation() {
        // test Handler
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        // call addAnnotation
        logicalChannelAssignments.addAnnotation("name", "value");

        // get annotatedNodeInfos
        List<AnnotatedNodeInfo> annotatedNodeInfos = new ArrayList<AnnotatedNodeInfo>();
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("annotatedNodeInfos");
            field.setAccessible(true);
            annotatedNodeInfos = (List<AnnotatedNodeInfo>) field.get(logicalChannelAssignments);
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
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        // set list of AnnotatedNodeInfo
        logicalChannelAssignments.addAnnotation("name", "value");

        assertEquals("[NG]getAnnotatedNodeInfoList:List size of AnnotatedNodeInfo is invalid.\n",
                     1, logicalChannelAssignments.getAnnotatedNodeInfoList().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:ResourceId of AnnotatedNodeInfo is not an expected one.\n",
                     rid, logicalChannelAssignments.getAnnotatedNodeInfoList().get(0).resourceId());
        assertEquals("[NG]getAnnotatedNodeInfoList:List size of Annotation is invalid.\n",
                     1, logicalChannelAssignments.getAnnotatedNodeInfoList().get(0).annotations().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:Name of Annotation is not an expected one.\n",
                     "name", logicalChannelAssignments.getAnnotatedNodeInfoList().get(0).annotations().get(0).name());
        assertEquals("[NG]getAnnotatedNodeInfoList:Value of Annotation is not an expected one.\n",
                     "value", logicalChannelAssignments.getAnnotatedNodeInfoList().get(0).annotations().get(0).value());
    }

    /**
     * UnitTest method for addAssignment.
     */
    @Test
    public void testAddAssignment() {
        // test Handler
        OpenConfigLogicalChannelAssignmentsHandler logicalChannelAssignments =
            new OpenConfigLogicalChannelAssignmentsHandler(parent);

        // call addAssignment
        OpenConfigAssignmentHandler assignment = new OpenConfigAssignmentHandler(2, logicalChannelAssignments);

        // expected ModelObject
        DefaultLogicalChannelAssignments modelObject = new DefaultLogicalChannelAssignments();
        DefaultAssignment assign = new DefaultAssignment();
        assign.index(2);
        modelObject.addToAssignment(assign);

        assertEquals("[NG]addAssignment:ModelObject(Assignment added) is not an expected one.\n",
                     modelObject, logicalChannelAssignments.getModelObject());
    }
}
