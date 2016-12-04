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
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.YtbDataType;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.YtbDataTypeOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.EnumDer1;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.EnumDer2;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.EnumLeafListUnion;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.UnionEnumUnion;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.enumder2.EnumDer2Enum;
import org.onosproject.yang.gen.v1.yms.test.ytb.data.type.rev20160826.ytbdatatype.enumleaflistunion.EnumLeafListUnionEnum1;
import org.onosproject.yang.gen.v1.yms.test.ytb.empty.type.rev20160826.YtbEmptyType;
import org.onosproject.yang.gen.v1.yms.test.ytb.empty.type.rev20160826.YtbEmptyTypeOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.empty.type.rev20160826.ytbemptytype.EmpType;
import org.onosproject.yang.gen.v1.yms.test.ytb.empty.type.rev20160826.ytbemptytype.EmpType2;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.YtbSimpleRpcResponse;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.YtbSimpleRpcResponseOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.Cumulative;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.DefaultCumulative;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;

/**
 * Unit test cases for invalid node skip in YANG tree builder.
 */
public class YtbInvalidNodeSkipTest extends YtbErrMsgAndConstants {

    private static final String CUMULATIVE = "cumulative";
    private static final String SUM = "sum";
    private static final String FIVE = "5";
    private static final String TEN_NUM = "10";
    private static final String DATA_TYPE = "YtbDataType";
    private static final String ENUM = "enum";
    private static final String EMPTY = "empty";
    private static final String THOUSAND = "thousand";
    private static final String TEN = "ten";
    private static final String ENUM_LEAF_LIST = "enum-leaf-list";
    private static final String UNION_ENUM = "union-enum";
    private static final String ENUM_LEAF_REF = "leaf-ref-enum";
    private static final String EMPTY_MOD = "YtbEmptyType";
    private static final String EMPTY_REF_LIST = "empty-list-ref";
    private static final String EMPTY_TYPE = "empty-type";
    private static final String EMP_LIST_REF_TYPE = "empty-list-ref-type";

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
        assertThat(getInCrtLeafValue(SUM, TEN_NUM), sum2.getValue(), is(TEN_NUM));
    }

    @Test
    public void processEnumDataType() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates the enum hundred for leaf enum.
        EnumDer2 der2 = new EnumDer2(EnumDer2Enum.HUNDRED);
        EnumDer1 der1 = new EnumDer1(der2);

        // Creates the enum hundred and ten for leaf-list having union.
        EnumLeafListUnion union1 = new EnumLeafListUnion(EnumLeafListUnionEnum1
                                                                 .HUNDRED);
        EnumLeafListUnion union2 = new EnumLeafListUnion(EnumLeafListUnionEnum1
                                                                 .TEN);

        List<EnumLeafListUnion> leafList = new ArrayList<>();
        leafList.add(union1);
        leafList.add(union2);

        // Creates a leaf having typedef in union, where as the typedef is enum.
        UnionEnumUnion enumUnion = new UnionEnumUnion(der1);

        // Creates a leaf-list with leaf-ref pointing to leaf with enum.
        EnumDer2 enum2 = new EnumDer2(EnumDer2Enum.THOUSAND);
        EnumDer1 enum1 = new EnumDer1(enum2);
        EnumDer2 enum4 = new EnumDer2(EnumDer2Enum.HUNDRED);
        EnumDer1 enum3 = new EnumDer1(enum4);

        List<EnumDer1> enumDer1 = new ArrayList<>();
        enumDer1.add(enum1);
        enumDer1.add(enum3);

        YtbDataType dataType = new YtbDataTypeOpParam.YtbDataTypeBuilder()
                .yangAutoPrefixEnum(der1).enumLeafList(leafList)
                .unionEnum(enumUnion).leafRefEnum(enumDer1).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(dataType);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and checks the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, DATA_TYPE), module.getName(),
                   is(DATA_TYPE));

        // Gets the first list content of cumulative.
        YdtContext leafEnum = module.getFirstChild();
        assertThat(getInCrtName(LEAF, ENUM), leafEnum.getName(), is(ENUM));
        assertThat(getInCrtLeafValue(ENUM, HUNDRED), leafEnum.getValue(),
                   is(HUNDRED));

        YdtContext unionEnum = leafEnum.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, UNION_ENUM), unionEnum.getName(),
                   is(UNION_ENUM));
        assertThat(getInCrtLeafValue(UNION_ENUM, HUNDRED), unionEnum.getValue(),
                   is(HUNDRED));

        YdtContext leafListEnum = unionEnum.getNextSibling();
        Set leafListVal = leafListEnum.getValueSet();
        assertThat(getInCrtName(LEAF_LIST, ENUM_LEAF_LIST),
                   leafListEnum.getName(), is(ENUM_LEAF_LIST));
        assertThat(getInCrtLeafListValue(ENUM_LEAF_LIST, HUNDRED),
                   leafListVal.contains(HUNDRED), is(true));
        assertThat(getInCrtLeafListValue(ENUM_LEAF_LIST, TEN_NUM),
                   leafListVal.contains(TEN), is(true));

        YdtContext leafRef = leafListEnum.getNextSibling();
        Set leafRefVal = leafRef.getValueSet();
        assertThat(getInCrtName(LEAF_LIST, ENUM_LEAF_REF), leafRef.getName(),
                   is(ENUM_LEAF_REF));
        assertThat(getInCrtLeafListValue(ENUM_LEAF_REF, HUNDRED),
                   leafRefVal.contains(HUNDRED), is(true));
        assertThat(getInCrtLeafListValue(ENUM_LEAF_REF, TEN_NUM),
                   leafRefVal.contains(THOUSAND), is(true));
    }

    @Test
    public void processEmptyDataType() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // For leaf-list empty-list.
        List<Boolean> empList = new ArrayList<>();
        empList.add(false);
        empList.add(true);

        // For leaf-list empty-list-ref-type and emp-ref-list.
        List<Boolean> empRefList = new ArrayList<>();
        empRefList.add(true);
        empRefList.add(false);

        // For leaf empty-type with typedef emp-type
        EmpType2 type2 = new EmpType2(true);
        EmpType type1 = new EmpType(type2);

        // For leaf-list empty-list-type with typedef emp-type
        EmpType2 type4 = new EmpType2(false);
        EmpType type3 = new EmpType(type4);
        EmpType2 type6 = new EmpType2(true);
        EmpType type5 = new EmpType(type6);

        List<EmpType> typeList = new ArrayList<>();
        typeList.add(type3);
        typeList.add(type5);

        YtbEmptyType emType = new YtbEmptyTypeOpParam.YtbEmptyTypeBuilder()
                .empty(true).emptyList(empList).emptyRef(false)
                .emptyListRef(empRefList).emptyType(type1)
                .emptyListType(typeList).emptyRefType(false)
                .emptyListRefType(empRefList).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(emType);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and checks the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, EMPTY_MOD), module.getName(),
                   is(EMPTY_MOD));

        // Gets the first list content of cumulative.
        YdtContext empty = module.getFirstChild();
        assertThat(getInCrtName(LEAF, EMPTY), empty.getName(), is(EMPTY));
        assertThat(empty.getValue(), nullValue());

        YdtContext emptyType = empty.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, EMPTY_TYPE), emptyType.getName(),
                   is(EMPTY_TYPE));
        assertThat(emptyType.getValue(), nullValue());

        YdtContext emptyRefList = emptyType.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, EMPTY_REF_LIST),
                   emptyRefList.getName(), is(EMPTY_REF_LIST));
        Set valueSet = emptyRefList.getValueSet();
        assertThat(valueSet.isEmpty(), is(true));

        YdtContext emptyListRefType = emptyRefList.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, EMP_LIST_REF_TYPE),
                   emptyListRefType.getName(), is(EMP_LIST_REF_TYPE));
        Set valueSet1 = emptyListRefType.getValueSet();
        assertThat(valueSet1.isEmpty(), is(true));
    }
}
