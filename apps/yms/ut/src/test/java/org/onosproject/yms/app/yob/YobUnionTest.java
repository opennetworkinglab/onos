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
import org.onosproject.yang.gen.v1.ydt.uniontest.rev20160524.UniontestOpParam;
import org.onosproject.yang.gen.v1.ydt.uniontest.rev20160524.uniontest.cont1.AugmentedCont1;
import org.onosproject.yang.gen.v1.ydt.uniontest.rev20160524.uniontest.food.snack.Sportsarena;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;

/**
 * Test the YANG object building for the YANG data tree based on the union.
 */
public class YobUnionTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void testUnionInLeaf() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("uniontest", "ydt.uniontest");
        ydtBuilder.addChild("unionList", null);
        ydtBuilder.addLeaf("id", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        UniontestOpParam unionTestOpParam = ((UniontestOpParam) yangObject);

        byte[] binaryValue = unionTestOpParam.unionList().get(0).id().binary();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testUnionInTypedef() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("uniontest", "ydt.uniontest");
        ydtBuilder.addLeaf("name", null, "bit1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        UniontestOpParam unionTestOpParam = ((UniontestOpParam) yangObject);
        assertThat(unionTestOpParam.name().union().bits().get(1), is(true));
    }

    @Test
    public void testUnionInGrouping() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("uniontest", "ydt.uniontest");
        ydtBuilder.addChild("cont1", "ydt.uniontest");
        ydtBuilder.addLeaf("surname", null, "yang");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        UniontestOpParam uniontestOpParam = ((UniontestOpParam) yangObject);
        assertThat(uniontestOpParam.cont1().surname().string(), is("yang"));
    }

    @Test
    public void testUnionInAugment() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("uniontest", "ydt.uniontest");
        ydtBuilder.addChild("cont1", "ydt.uniontest");
        ydtBuilder.addLeaf("lastname", null, "bit0");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        UniontestOpParam uniontestOpParam = ((UniontestOpParam) yangObject);

        AugmentedCont1 augmentedCont1 = (AugmentedCont1) uniontestOpParam
                .cont1().yangAugmentedInfo(AugmentedCont1.class);
        assertThat(augmentedCont1.lastname().bits().get(0), is(true));
    }

    @Test
    public void testUnionInCase() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("uniontest", "ydt.uniontest");
        ydtBuilder.addChild("food", "ydt.uniontest");
        ydtBuilder.addLeaf("pretzel", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        UniontestOpParam uniontestOpParam = ((UniontestOpParam) yangObject);
        Sportsarena sportsArena = ((Sportsarena) uniontestOpParam.food()
                .snack());
        byte[] binaryValue = sportsArena.pretzel().binary();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }
}
