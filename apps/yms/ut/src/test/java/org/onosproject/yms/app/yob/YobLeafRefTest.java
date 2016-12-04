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

package org.onosproject.yms.app.yob;

import org.junit.Test;
import org.onosproject.yang.gen.v1.ydt.leafreftest.rev20160524.LeafreftestOpParam;
import org.onosproject.yang.gen.v1.ydt.leafreftest.rev20160524.leafreftest.cont1.AugmentedCont1;
import org.onosproject.yang.gen.v1.ydt.leafreftest.rev20160524.leafreftest.food.snack.Sportsarena;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;

/**
 * Test the YANG object building for the YANG data tree based on the leaf ref.
 */
public class YobLeafRefTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void testLeafrefInLeaf() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("leafreftest", "ydt.leafreftest");
        ydtBuilder.addChild("leafrefList", null);
        ydtBuilder.addLeaf("id", null, "leafref");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        LeafreftestOpParam leafrefTestOpParam = ((LeafreftestOpParam)
                yangObject);
        assertThat(leafrefTestOpParam.leafrefList().get(0).id().toString(),
                   is("leafref"));
    }

    @Test
    public void testLeafrefInTypedef() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("leafreftest", "ydt.leafreftest");
        ydtBuilder.addLeaf("name", null, "leafref");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        LeafreftestOpParam leafrefTestOpParam = ((LeafreftestOpParam) yangObject);
        assertThat(leafrefTestOpParam.name().toString(), is("leafref"));
    }

    @Test
    public void testLeafrefInGrouping() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("leafreftest", "ydt.leafreftest");
        ydtBuilder.addChild("cont1", "ydt.leafreftest");
        ydtBuilder.addLeaf("surname", null, "leafref");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        LeafreftestOpParam leafreftestOpParam = ((LeafreftestOpParam) yangObject);
        assertThat(leafreftestOpParam.cont1().surname().toString(),
                   is("leafref"));
    }

    @Test
    public void testLeafrefInAugment() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("leafreftest", "ydt.leafreftest");
        ydtBuilder.addChild("cont1", "ydt.leafreftest");
        ydtBuilder.addLeaf("lastname", null, "yang");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        LeafreftestOpParam leafreftestOpParam = ((LeafreftestOpParam) yangObject);
        AugmentedCont1 augmentedCont1 = (AugmentedCont1) leafreftestOpParam
                .cont1().yangAugmentedInfo(AugmentedCont1.class);
        assertThat(augmentedCont1.lastname().toString(), is("yang"));
    }

    @Test
    public void testLeafrefInCase() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("leafreftest", "ydt.leafreftest");
        ydtBuilder.addChild("food", "ydt.leafreftest");
        ydtBuilder.addLeaf("pretzel", null, "yang");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        LeafreftestOpParam leafreftestOpParam = ((LeafreftestOpParam)
                yangObject);
        Sportsarena sportsArena = ((Sportsarena) leafreftestOpParam.food()
                .snack());
        assertThat(sportsArena.pretzel().toString(), is("yang"));
    }
}
