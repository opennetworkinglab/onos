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
import org.onosproject.yang.gen.v1.ydt.binarytest.rev20160524.BinarytestOpParam;
import org.onosproject.yang.gen.v1.ydt.binarytest.rev20160524.binarytest.cont1.AugmentedCont1;
import org.onosproject.yang.gen.v1.ydt.binarytest.rev20160524.binarytest.food.snack.Sportsarena;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;

/**
 * Test the YANG object building for the YANG data tree based on the binary.
 */
public class YobBinaryTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void testBinaryInLeaf() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addChild("binaryList", null);
        ydtBuilder.addLeaf("binary", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);

        byte[] binaryValue = binarytestOpParam.binaryList().get(0).binary();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testBinaryInTypedef() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addLeaf("name", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);

        byte[] binaryValue = binarytestOpParam.name().binary();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testBinaryInGrouping() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addChild("cont1", "ydt.binarytest");
        ydtBuilder.addLeaf("surname", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);

        byte[] binaryValue = binarytestOpParam.cont1().surname();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testBinaryInAugment() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addChild("cont1", "ydt.binarytest");
        ydtBuilder.addLeaf("lastname", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);

        AugmentedCont1 augmentedCont1 = (AugmentedCont1) binarytestOpParam.cont1()
                .yangAugmentedInfo(AugmentedCont1.class);
        byte[] binaryValue = augmentedCont1.lastname();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testBinaryInCase() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addChild("food", "ydt.binarytest");
        ydtBuilder.addLeaf("pretzel", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);
        Sportsarena sportsArena = ((Sportsarena) binarytestOpParam.food()
                .snack());
        byte[] binaryValue = sportsArena.pretzel();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }

    @Test
    public void testBinaryInUnion() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("binarytest", "ydt.binarytest");
        ydtBuilder.addLeaf("middlename", null, "YmluYXJ5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BinarytestOpParam binarytestOpParam = ((BinarytestOpParam) yangObject);

        byte[] binaryValue = binarytestOpParam.middlename().binary();
        String value = new String(binaryValue);
        assertThat(value, is("binary"));
    }
}
