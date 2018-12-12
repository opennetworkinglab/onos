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

import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.openconfigplatformtransceiver.porttransceivertop.DefaultTransceiver;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.openconfigplatformtransceiver.porttransceivertop.transceiver.DefaultConfig;

import static org.junit.Assert.assertEquals;

/**
 * UnitTest class for OpenConfigTransceiverHandler.
 */
public class OpenConfigTransceiverHandlerTest {
    // parent Handler
    OpenConfigComponentHandler parent;

    // expected ResourceId
    ResourceId rid;

    /**
     * Set up method for UnitTest.
     */
    @Before
    public void setUp() {
        // parent Handler set up
        // create <components xmlns="http://openconfig.net/yang/platform"></components>
        OpenConfigComponentsHandler components = new OpenConfigComponentsHandler();

        // add <component><name>name</name></component>
        parent = new OpenConfigComponentHandler("name", components);

        // expected ResourceId set up
        rid = ResourceId.builder()
        .addBranchPointSchema("components", "http://openconfig.net/yang/platform")
        .addBranchPointSchema("component", "http://openconfig.net/yang/platform")
        .addKeyLeaf("name", "http://openconfig.net/yang/platform", "name")
        .addBranchPointSchema("transceiver", "http://openconfig.net/yang/platform/transceiver")
        .build();
    }

    /**
     * UnitTest method for getModelObject.
     */
    @Test
    public void testGetModelObject() {
        // test Handler
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        // expected ModelObject
        DefaultTransceiver modelObject = new DefaultTransceiver();

        assertEquals("[NG]getModelObject:Return is not an expected ModelObject.\n",
                     modelObject, transceiver.getModelObject());
    }

    /**
     * UnitTest method for setResourceId.
     */
    @Test
    public void testSetResourceId() {
        // call setResourceId
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        // get resourceId
        ResourceId resourceId = null;
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("resourceId");
            field.setAccessible(true);
            resourceId = (ResourceId) field.get(transceiver);
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
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        assertEquals("[NG]getResourceId:Return is not an expected ResourceId.\n",
                     rid, transceiver.getResourceId());
    }

    /**
     * UnitTest method for getResourceIdBuilder.
     */
    @Test
    public void testGetResourceIdBuilder() {
        // test Handler
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        assertEquals("[NG]getResourceIdBuilder:Return is not an expected ResourceIdBuilder.\n",
                     rid, transceiver.getResourceIdBuilder().build());
    }

    /**
     * UnitTest method for addAnnotation.
     */
    @Test
    public void testAddAnnotation() {
        // test Handler
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        // call addAnnotation
        transceiver.addAnnotation("name", "value");

        // get annotatedNodeInfos
        List<AnnotatedNodeInfo> annotatedNodeInfos = new ArrayList<AnnotatedNodeInfo>();
        try {
            Field field = OpenConfigObjectHandler.class.getDeclaredField("annotatedNodeInfos");
            field.setAccessible(true);
            annotatedNodeInfos = (List<AnnotatedNodeInfo>) field.get(transceiver);
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
    public void testAnnotatedNodeInfoList() {
        // test Handler
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        // set list of AnnotatedNodeInfo
        transceiver.addAnnotation("name", "value");

        assertEquals("[NG]getAnnotatedNodeInfoList:List size of AnnotatedNodeInfo is invalid.\n",
                     1, transceiver.getAnnotatedNodeInfoList().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:ResourceId of AnnotatedNodeInfo is not an expected one.\n",
                     rid, transceiver.getAnnotatedNodeInfoList().get(0).resourceId());
        assertEquals("[NG]getAnnotatedNodeInfoList:List size of Annotation is invalid.\n",
                     1, transceiver.getAnnotatedNodeInfoList().get(0).annotations().size());
        assertEquals("[NG]getAnnotatedNodeInfoList:Name of Annotation is not an expected one.\n",
                     "name", transceiver.getAnnotatedNodeInfoList().get(0).annotations().get(0).name());
        assertEquals("[NG]getAnnotatedNodeInfoList:Value of Annotation is not an expected one.\n",
                     "value", transceiver.getAnnotatedNodeInfoList().get(0).annotations().get(0).value());
    }

    /**
     * UnitTest method for addConfig.
     */
    @Test
    public void testAddConfig() {
        // test Handler
        OpenConfigTransceiverHandler transceiver = new OpenConfigTransceiverHandler(parent);

        // call addConfig
        OpenConfigConfigOfTransceiverHandler config = new OpenConfigConfigOfTransceiverHandler(transceiver);

        // expected ModelObject
        DefaultTransceiver modelObject = new DefaultTransceiver();
        DefaultConfig con = new DefaultConfig();
        modelObject.config(con);

        assertEquals("[NG]addConfig:ModelObject(Config added) is not an expected one.\n",
                     modelObject, transceiver.getModelObject());
    }
}
