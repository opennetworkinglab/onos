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

package org.onosproject.yms.app.ytb;

import org.junit.Test;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.YtbSimpleRpcResponse;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.YtbSimpleRpcResponseOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.Cumulative;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.DefaultCumulative;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;

/**
 * Unit test cases for invalid node skip in YANG tree builder.
 */
public class YtbInvalidNodeSkipTest extends YtbErrMsgAndConstants {

    private static final String CUMULATIVE = "cumulative";
    private static final String SUM = "sum";
    private static final String FIVE = "5";
    private static final String TEN = "10";

    /**
     * Processes RPC node which is the sibling to the empty current node.
     */
    @Test
    public void processRpcSiblingWhenNodeIsEmpty() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.
        Cumulative cumulative1 = new DefaultCumulative.CumulativeBuilder()
                .sum((byte) 5).build();
        Cumulative cumulative2 = new DefaultCumulative.CumulativeBuilder()
                .sum((byte) 10).build();
        List<Cumulative> list = new ArrayList<>();
        list.add(cumulative1);
        list.add(cumulative2);
        YtbSimpleRpcResponse rpc = new YtbSimpleRpcResponseOpParam
                .YtbSimpleRpcResponseBuilder().cumulative(list).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(rpc);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and checks the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, RPC_NAME), module.getName(),
                   is(RPC_NAME));

        // Gets the first list content of cumulative.
        YdtContext list1 = module.getFirstChild();
        assertThat(getInCrtName(LIST, CUMULATIVE), list1.getName(),
                   is(CUMULATIVE));

        YdtContext sum1 = list1.getFirstChild();
        assertThat(getInCrtName(LEAF, SUM), sum1.getName(), is(SUM));
        assertThat(getInCrtLeafValue(SUM, FIVE), sum1.getValue(), is(FIVE));

        // Gets the second list content of cumulative.
        YdtContext list2 = list1.getNextSibling();
        assertThat(getInCrtName(LIST, CUMULATIVE), list2.getName(),
                   is(CUMULATIVE));

        YdtContext sum2 = list2.getFirstChild();
        assertThat(getInCrtName(LEAF, SUM), sum2.getName(), is(SUM));
        assertThat(getInCrtLeafValue(SUM, TEN), sum2.getValue(), is(TEN));
    }


}
