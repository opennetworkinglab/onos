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
import org.onosproject.yang.gen.v1.ydt.bit.rev20160524.BitOpParam;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YmsOperationType;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;

/**
 * Test the YANG object building for the YDT based on the bits.
 */
public class YobBitTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void testBitsInLeaf() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("bit", "ydt.bit");
        ydtBuilder.addChild("bitList", null);
        ydtBuilder.addLeaf("bit", null, "disable-nagle");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("bitList", null);
        ydtBuilder.addLeaf("bit", null, "auto-sense-speed");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("bitList", null);
        ydtBuilder.addLeaf("bit", null, "ten-Mb-only");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BitOpParam bitOpParam = ((BitOpParam) yangObject);

        assertThat(bitOpParam.bitList().get(0).bit().get(0), is(true));
        assertThat(bitOpParam.bitList().get(0).bit().get(1), is(false));
        assertThat(bitOpParam.bitList().get(1).bit().get(0), is(false));
        assertThat(bitOpParam.bitList().get(1).bit().get(1), is(true));
        assertThat(bitOpParam.bitList().get(2).bit().get(0), is(false));
        assertThat(bitOpParam.bitList().get(2).bit().get(1), is(false));
        assertThat(bitOpParam.bitList().get(2).bit().get(2), is(true));
        assertThat(bitOpParam.bitList().get(2).bit().get(2), is(true));
    }

    @Test
    public void testBitsInTypedef() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild("bit", "ydt.bit");
        ydtBuilder.addLeaf("name", null, "bit3");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BitOpParam bitOpParam = ((BitOpParam) yangObject);

        assertThat(bitOpParam.name().bits().get(0), is(false));
        assertThat(bitOpParam.name().bits().get(3), is(true));
    }

    @Test
    public void testBitsQuery() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, YmsOperationType.QUERY_REQUEST,
                utils.schemaRegistry(), true);
        ydtBuilder.addChild("bit", "ydt.bit");
        ydtBuilder.addChild("name", "ydt.bit");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        YdtContext rootCtx = ydtBuilder.getRootNode();
        YdtContext childCtx = rootCtx.getFirstChild();
        DefaultYobBuilder builder = new DefaultYobBuilder();
        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, utils.schemaRegistry());
        assertThat(yangObject, notNullValue());
        BitOpParam bitOpParam = ((BitOpParam) yangObject);
        assertThat(bitOpParam.selectLeafFlags().get(1), is(true));
    }
}
