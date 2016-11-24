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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.YtbDerivedTypeWithBitsAndBinary;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.YtbDerivedTypeWithBitsAndBinaryOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.ytbderivedtypewithbitsandbinary.Derivedbinarya;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.ytbderivedtypewithbitsandbinary.Derivedbinaryb;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.ytbderivedtypewithbitsandbinary.Derivedbitsa;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.ytbderivedtypewithbitsandbinary.Derivedbitsb;
import org.onosproject.yang.gen.v1.yms.test.ytb.derived.type.with.bits.and.binary.rev20160826.ytbderivedtypewithbitsandbinary.ForunionUnion;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.container.rev20160826.YtbModuleWithContainer;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.container.rev20160826.YtbModuleWithContainerOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.container.rev20160826.ytbmodulewithcontainer.DefaultSched;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.container.rev20160826.ytbmodulewithcontainer.Sched;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaf.ietfschedule.rev20160826.YtbIetfSchedule;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaf.ietfschedule.rev20160826.YtbIetfSchedule.OnosYangOpType;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaf.ietfschedule.rev20160826.YtbIetfScheduleOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaf.ietfschedule.rev20160826.ytbietfschedule.Enum1Enum;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaf.ietfschedule.rev20160826.ytbietfschedule.Enum2Enum;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaflist.rev20160826.YtbModuleWithLeafList;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.leaflist.rev20160826.YtbModuleWithLeafListOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.yangautoprefixlist.rev20160826.YtbModuleWithList;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.yangautoprefixlist.rev20160826.YtbModuleWithListOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.yangautoprefixlist.rev20160826.ytbmodulewithlist.DefaultYtblistlist;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.yangautoprefixlist.rev20160826.ytbmodulewithlist.Find;
import org.onosproject.yang.gen.v1.yms.test.ytb.module.with.yangautoprefixlist.rev20160826.ytbmodulewithlist.Ytblistlist;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.a.rev20160826.YtbMultiModulea;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.a.rev20160826.YtbMultiModuleaOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.a.rev20160826.ytbmultimodulea.DefaultYtbmultilist;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.a.rev20160826.ytbmultimodulea.Ytbmultilist;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.b.rev20160826.YtbMultiModuleb;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.b.rev20160826.YtbMultiModulebOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.b.rev20160826.ytbmultimoduleb.DefaultYtbmultilistb;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.module.b.rev20160826.ytbmultimoduleb.Ytbmultilistb;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.DefaultFortesta;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.Fortesta;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.YtbMultiNotificationWithContainerEvent;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.YtbMultiNotificationWithContainerEventSubject;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.fortesta.DefaultYtbnot;
import org.onosproject.yang.gen.v1.yms.test.ytb.multi.notification.with.container.rev20160826.ytbmultinotificationwithcontainer.fortesta.Ytbnot;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.YtbTreeBuilderForListHavingList;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.YtbTreeBuilderForListHavingListOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.Carrier;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.DefaultCarrier;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.carrier.DefaultMultiplexes;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.carrier.Multiplexes;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.carrier.multiplexes.ApplicationAreas;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.carrier.multiplexes.DefaultApplicationAreas;
import org.onosproject.yang.gen.v1.yms.test.ytb.tree.builder.yangautoprefixfor.yangautoprefixlist.having.yangautoprefixlist.rev20160826.ytbtreebuilderforlisthavinglist.carrier.multiplexes.TypesEnum;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ydt.YdtNode;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.onosproject.yms.ydt.YdtContextOperationType.CREATE;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;

/**
 * Unit test cases for YANG tree builder with different YANG object
 * configuration.
 */
public class DefaultYangTreeBuilderTest extends YtbErrMsgAndConstants {

    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final String THREE = "3";
    private static final String FOUR = "4";
    private static final String FIVE = "5";
    private static final String SIX = "6";
    private static final String NINE = "9";
    private static final String IETF_SCHEDULE = "YtbIetfSchedule";
    private static final String TIME = "time";
    private static final String MOD_LEAF_LIST = "YtbModuleWithLeafList";
    private static final String ENUM_1 = "enum1";
    private static final String ENUM_2 = "enum2";
    private static final String HUNDRED_100 = "hundred-100";
    private static final String TEN_10 = "ten-10";
    private static final String THOUSAND_1000 = "thousand-1000";
    private static final String MOD_CONT = "YtbModuleWithContainer";
    private static final String SCHED = "sched";
    private static final String PREDICT_VAL = "98989";
    private static final String MOD_LIST = "YtbModuleWithList";
    private static final String LIST_LIST = "ytblistlist";
    private static final String PREDICTION = "prediction";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String MUL_NOTIFY =
            "YtbMultiNotificationWithContainer";
    private static final String NOTIFICATION = "notification";
    private static final String NOTIFY = "fortesta";
    private static final String YTB_NOTIFY_CONT = "ytbnot";
    private static final String NOTIFY_LEAF = "notileaf";
    private static final String ANT = "ant";
    private static final String ANIMAL = "animal";
    private static final String BIRD = "bird";
    private static final String BALL = "ball";
    private static final String BAT = "bat";
    private static final String MUL_MOD_A = "YtbMultiModulea";
    private static final String MUL_LIST_A = "ytbmultilist";
    private static final String CHECK = "check";
    private static final String MUL_MOD_B = "YtbMultiModuleb";
    private static final String MUL_LIST_B = "ytbmultilistb";
    private static final String CHECKIN = "checkin";
    private static final String LIST_WITH_LIST =
            "YtbTreeBuilderForListHavingList";
    private static final String CONT_CARRIER = "carrier";
    private static final String LIST_MULTIPLEXES = "multiplexes";
    private static final String TYPES = "types";
    private static final String TIME_DIVISION = "time-division";
    private static final String APP_AREA_LIST = "application-areas";
    private static final String DEST_AREA = "destination-areas";
    private static final String FREQUENCY_DIV = "frequency-division";
    private static final String MOD_BIT_BIN = "YtbDerivedTypeWithBitsAndBinary";
    private static final String FOR_BINARY = "forbinary";
    private static final String BIN_VAL_1 = "BQUF";
    private static final String FOR_BITS = "forbits";
    private static final String FOR_BINARY_LIST = "forbinarylist";
    private static final String BIN_VAL_2 = "CQkA";
    private static final String BIN_VAL_3 = "DAYA";
    private static final String BIN_VAL_4 = "EB0Z";
    private static final String FOR_BITS_LIST = "forbitslist";
    private static final String FOR_UNION = "forunion";
    private static final String BIN_VAL_5 = "AAAA";
    private static final String BIT_VAL_1 = "leaf1 leaf2";
    private static final String BIN_VAL_6 = "AQYD";
    private static final String BIN_VAL_7 = "AgcE";
    private static final String BIN_VAL_8 = "BQYB";
    private static final String BIN_VAL_9 = "AwgE";
    private static final String BIN_VAL_10 = "AQAA";
    private static final String BIN_VAL_11 = "AAAB";
    private static final String BIN_VAL_12 = "BwcH";
    private static final String BIN_VAL_13 = "AAE=";
    private static final String BIT_VAL_2 = "index signature";
    private static final String BIT_VAL_3 = "index";
    private static final String BIT_VAL_4 = "index name";
    private static final String BIT_VAL_5 = "index name signature";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String emptyObjErrMsg(String objName) {
        return "The " + objName + " given for tree creation cannot be null";
    }

    private static BigDecimal getBigDeci(int bigDecimal) {
        return BigDecimal.valueOf(bigDecimal);
    }

    /**
     * Processes an empty object list to the YTB and checks that the
     * exception is thrown.
     */
    @Test
    public void processInvalidListInput() {
        thrown.expect(YtbException.class);
        thrown.expectMessage(emptyObjErrMsg("object list"));
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        treeBuilder.getYdtBuilderForYo(null, ROOT_NAME, ROOT_NAME_SPACE,
                                       EDIT_CONFIG_REQUEST, null);
    }

    /**
     * Processes an empty notification object to the YTB and checks that the
     * exception is thrown.
     */
    @Test
    public void processInvalidInputForNotification() {
        thrown.expect(YtbException.class);
        thrown.expectMessage(emptyObjErrMsg("event object"));
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        treeBuilder.getYdtForNotification(null, ROOT_NAME, null);
    }

    /**
     * Processes an empty rpc output object to the YTB and checks that the
     * exception is thrown.
     */
    @Test
    public void processInvalidInputForRpc() {
        thrown.expect(YtbException.class);
        thrown.expectMessage(emptyObjErrMsg("output object"));
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        treeBuilder.getYdtForRpcResponse(null, null);
    }

    /**
     * Processes a YAB/YSB request to YTB with a leaf value being filled in
     * the app object. Checks the constructed YDT tree for module and leaf
     * and its value.
     */
    @Test
    public void processModuleAndLeaf() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.
        YtbIetfSchedule schedule = new YtbIetfScheduleOpParam
                .YtbIetfScheduleBuilder()
                .time((byte) 9)
                .yangYtbIetfScheduleOpType(OnosYangOpType.MERGE)
                .build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(schedule);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and checks the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        YdtContextOperationType opType = ((YdtNode) module)
                .getYdtContextOperationType();
        assertThat(getInCrtName(MODULE, IETF_SCHEDULE),
                   module.getName(), is(IETF_SCHEDULE));
        assertThat(getInCrtOpType(MODULE, IETF_SCHEDULE),
                   opType, is(MERGE));

        // Gets the first leaf from module IetfSchedule.
        YdtContext leafContext = module.getFirstChild();
        assertThat(getInCrtName(LEAF, TIME),
                   leafContext.getName(), is(TIME));
        assertThat(getInCrtLeafValue(TIME, NINE),
                   leafContext.getValue(), is(NINE));
    }

    /**
     * Processes a YAB/YSB request to YTB with a leaf-list value being filled
     * in the app object. Checks the constructed YDT tree for module and
     * leaf-list and its value.
     */
    @Test
    public void processModuleAndLeafList() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates list of type long for setting the leaf-list.
        List<Long> longList = new ArrayList<>();
        longList.add((long) 1);
        longList.add((long) 2);
        longList.add((long) 3);

        YtbModuleWithLeafList leafListModule = new YtbModuleWithLeafListOpParam
                .YtbModuleWithLeafListBuilder().time(longList).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(leafListModule);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE, QUERY_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        YdtContextOperationType opType = ((YdtNode) module)
                .getYdtContextOperationType();
        assertThat(getInCrtName(MODULE, MOD_LEAF_LIST),
                   module.getName(), is(MOD_LEAF_LIST));
        assertThat(getInCrtOpType(MODULE, MOD_LEAF_LIST), opType, nullValue());

        // Gets the first leaf-list from module.
        YdtContext leafList = module.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, TIME), leafList.getName(),
                   is(TIME));
        Set<String> value = leafList.getValueSet();
        assertThat(getInCrtLeafListValue(TIME, ONE),
                   value.contains(ONE), is(true));
        assertThat(getInCrtLeafListValue(TIME, TWO),
                   value.contains(TWO), is(true));
        assertThat(getInCrtLeafListValue(TIME, THREE),
                   value.contains(THREE), is(true));
    }

    /**
     * Processes leaf and leaf-list with type enum under module. Checks the
     * constructed YDT tree has YANG enum value.
     */
    @Test
    public void processWithTypeEnum() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates enum list for setting the leaf-list.
        List<Enum2Enum> enumList = new ArrayList<>();
        enumList.add(Enum2Enum.HUNDRED_100);
        enumList.add(Enum2Enum.TEN_10);
        enumList.add(Enum2Enum.THOUSAND_1000);

        YtbIetfSchedule schedule = new YtbIetfScheduleOpParam
                .YtbIetfScheduleBuilder()
                .time((byte) 9)
                .yangYtbIetfScheduleOpType(OnosYangOpType.MERGE)
                .enum1(Enum1Enum.HUNDRED)
                .enum2(enumList)
                .build();


        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(schedule);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        YdtContextOperationType opType =
                ((YdtNode) module).getYdtContextOperationType();
        assertThat(getInCrtName(MODULE, IETF_SCHEDULE),
                   module.getName(), is(IETF_SCHEDULE));
        assertThat(getInCrtOpType(MODULE, IETF_SCHEDULE), opType, is(MERGE));

        // Checks the leaf and leaf-list values.
        YdtContext timeLeaf = module.getFirstChild();
        assertThat(getInCrtName(LEAF, TIME), timeLeaf.getName(), is(TIME));
        assertThat(getInCrtLeafValue(TIME, NINE),
                   timeLeaf.getValue(), is(NINE));

        YdtContext enum1Leaf = timeLeaf.getNextSibling();
        assertThat(getInCrtName(LEAF, ENUM_1), enum1Leaf.getName(), is(ENUM_1));
        assertThat(getInCrtLeafValue(ENUM_1, HUNDRED),
                   enum1Leaf.getValue(), is(HUNDRED));

        YdtContext enum2LeafList = enum1Leaf.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, ENUM_2),
                   enum2LeafList.getName(), is(ENUM_2));
        Set<String> valueSet = enum2LeafList.getValueSet();
        assertThat(getInCrtLeafListValue(ENUM_2, HUNDRED_100),
                   valueSet.contains(HUNDRED_100), is(true));
        assertThat(getInCrtLeafListValue(ENUM_2, TEN_10),
                   valueSet.contains(TEN_10), is(true));
        assertThat(getInCrtLeafListValue(ENUM_2, THOUSAND_1000),
                   valueSet.contains(THOUSAND_1000), is(true));
    }

    /**
     * Processes a YAB/YSB request to YTB with a container having leaf value
     * being filled in the app object. Checks the constructed YDT tree for
     * module and container and leaf.
     */
    @Test
    public void processModuleWithContainer() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates container object with leaf of decimal type.
        Sched sched = new DefaultSched.SchedBuilder()
                .predict(getBigDeci(98989))
                .yangSchedOpType(
                        YtbModuleWithContainerOpParam.OnosYangOpType.DELETE)
                .build();
        // Creates module object with the container.
        YtbModuleWithContainer contModule = new YtbModuleWithContainerOpParam
                .YtbModuleWithContainerBuilder()
                .sched(sched)
                .yangYtbModuleWithContainerOpType(
                        YtbModuleWithContainerOpParam.OnosYangOpType.CREATE)
                .build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(contModule);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                QUERY_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        YdtContextOperationType opType = ((YdtNode) module)
                .getYdtContextOperationType();

        assertThat(getInCrtName(MODULE, MOD_CONT),
                   module.getName(), is(MOD_CONT));
        assertThat(getInCrtOpType(MODULE, MOD_CONT), opType, is(CREATE));

        // Get the container from module.
        YdtContext container = module.getFirstChild();
        YdtContextOperationType opTypeOfCont = ((YdtNode) container)
                .getYdtContextOperationType();

        assertThat(getInCrtName(CONTAINER, SCHED),
                   container.getName(), is("sched"));
        assertThat(getInCrtOpType(CONTAINER, SCHED), opTypeOfCont, is(DELETE));

        // Gets leaf from container.
        YdtContext leafContext = container.getFirstChild();
        assertThat(getInCrtName(LEAF, PREDICT),
                   leafContext.getName(), is(PREDICT));
        assertThat(getInCrtLeafValue(PREDICT, PREDICT_VAL),
                   leafContext.getValue(), is(PREDICT_VAL));
    }

    /**
     * Processes a YAB/YSB request to YTB with a list having leaf-list value
     * being filled in the app object. Checks the constructed YDT tree for
     * module and list and leaf-list.
     */
    @Test
    public void processModuleWithList() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates multi typedef values.
        Find find1 = new Find(true);
        Find find2 = new Find(false);
        Find find3 = new Find(true);
        Find find4 = new Find(false);

        // Creates two lists, with the typedef values added.
        List<Find> findList1 = new ArrayList<>();
        List<Find> findList2 = new ArrayList<>();
        findList1.add(find1);
        findList1.add(find2);
        findList2.add(find3);
        findList2.add(find4);

        // Creates two list contents.
        Ytblistlist list1 = new DefaultYtblistlist
                .YtblistlistBuilder().prediction(findList1).build();
        Ytblistlist list2 = new DefaultYtblistlist
                .YtblistlistBuilder().prediction(findList2).build();

        List<Ytblistlist> ytbList = new ArrayList<>();
        ytbList.add(list1);
        ytbList.add(list2);

        // Creates module having list.
        YtbModuleWithList listModule = new YtbModuleWithListOpParam
                .YtbModuleWithListBuilder().ytblistlist(ytbList).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(listModule);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        YdtContextOperationType opType = ((YdtNode) module)
                .getYdtContextOperationType();

        assertThat(getInCrtName(MODULE, MOD_LIST),
                   module.getName(), is(MOD_LIST));
        assertThat(getInCrtOpType(MODULE, MOD_LIST), opType, nullValue());

        // Gets the first list from module YtbModuleWithList.
        YdtContext firstList = module.getFirstChild();
        YdtContextOperationType listOpType = ((YdtNode) firstList)
                .getYdtContextOperationType();
        // Checks the contents in the list.
        assertThat(getInCrtName(LIST, LIST_LIST),
                   firstList.getName(), is(LIST_LIST));
        assertThat(getInCrtOpType(LIST, LIST_LIST), listOpType, nullValue());

        // Gets the contents of the leaf-list in the first list content.
        YdtContext leafListInList1 = firstList.getFirstChild();

        // Evaluates the leaf-list values.
        Set leafListValue1 = leafListInList1.getValueSet();
        assertThat(getInCrtName(LEAF_LIST, PREDICTION),
                   leafListInList1.getName(), is(PREDICTION));
        assertThat(getInCrtLeafListValue(PREDICTION, TRUE),
                   leafListValue1.contains(TRUE), is(true));
        assertThat(getInCrtLeafListValue(PREDICTION, FALSE),
                   leafListValue1.contains(FALSE), is(true));

        // Gets the second list from module YtbModuleWithList.
        YdtContext secondList = firstList.getNextSibling();

        // Gets the contents of the leaf-list in the second list content.
        YdtContext leafListInList2 = secondList.getFirstChild();
        // Evaluates the leaf-list values.
        Set leafListValue2 = leafListInList2.getValueSet();
        assertThat(getInCrtName(LEAF_LIST, PREDICTION),
                   leafListInList2.getName(), is(PREDICTION));
        assertThat(getInCrtLeafListValue(PREDICTION, TRUE),
                   leafListValue2.contains(TRUE), is(true));
        assertThat(getInCrtLeafListValue(PREDICTION, FALSE),
                   leafListValue2.contains(FALSE), is(true));
    }

    /**
     * Processes multi notification under module when request comes for one
     * notification event in module.
     */
    @Test
    public void processMultiNotificationWithContainer() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Sets the bit value.
        BitSet bitleaf = new BitSet();
        bitleaf.set(0);
        bitleaf.set(1);

        // Creates container with the leaf.
        Ytbnot ytbnot = new DefaultYtbnot.YtbnotBuilder().notileaf(bitleaf)
                .build();
        // Creates notification with container.
        Fortesta testa = new DefaultFortesta.FortestaBuilder()
                .ytbnot(ytbnot).build();
        // Invokes event subject class with notification.
        YtbMultiNotificationWithContainerEventSubject eventSubject = new
                YtbMultiNotificationWithContainerEventSubject();
        eventSubject.fortesta(testa);
        // Invokes event class with the event type and the event subject obj.
        YtbMultiNotificationWithContainerEvent event =
                new YtbMultiNotificationWithContainerEvent(
                        YtbMultiNotificationWithContainerEvent.Type.FORTESTA,
                        eventSubject);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtContext ydtContext = treeBuilder.getYdtForNotification(
                event, ROOT_NAME, registry);

        // Gets the first module from logical root node.
        YdtContext context = ydtContext.getFirstChild();
        YdtContextOperationType opType = ((YdtNode) context)
                .getYdtContextOperationType();

        assertThat(getInCrtName(MODULE, MUL_NOTIFY), context.getName(),
                   is(MUL_NOTIFY));
        assertThat(getInCrtOpType(MODULE, MUL_NOTIFY), opType, is(NONE));

        // Gets the notification under module.
        YdtContext notify = context.getFirstChild();
        YdtContextOperationType notifyOpType = ((YdtNode) notify)
                .getYdtContextOperationType();

        // Checks the contents in the first notification.
        assertThat(getInCrtName(NOTIFICATION, NOTIFY), notify.getName(),
                   is(NOTIFY));
        assertThat(getInCrtOpType(NOTIFICATION, NOTIFY), notifyOpType,
                   is(NONE));

        // Gets the container in notification
        YdtContext container = notify.getFirstChild();
        assertThat(getInCrtName(CONTAINER, YTB_NOTIFY_CONT),
                   container.getName(), is(YTB_NOTIFY_CONT));

        // Evaluates the leaf values.
        YdtContext leafInCont = container.getFirstChild();
        assertThat(getInCrtName(LEAF, NOTIFY_LEAF), leafInCont.getName(),
                   is(NOTIFY_LEAF));
        assertThat(getInCrtLeafValue(NOTIFY_LEAF, BIT_VAL_1),
                   leafInCont.getValue(), is(BIT_VAL_1));
    }

    /**
     * Processes multi module with list in both the modules and checks the
     * YANG data tree building.
     */
    @Test
    public void processMultiModule() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates list of big integer for leaf-list under list1.
        List<BigInteger> bigIntegerList = new ArrayList<>();
        bigIntegerList.add(BigInteger.valueOf(1));
        bigIntegerList.add(BigInteger.valueOf(2));
        bigIntegerList.add(BigInteger.valueOf(3));
        // Creates list of big integer for leaf-list under list2.
        List<BigInteger> bigIntegerList1 = new ArrayList<>();
        bigIntegerList1.add(BigInteger.valueOf(4));
        bigIntegerList1.add(BigInteger.valueOf(5));
        bigIntegerList1.add(BigInteger.valueOf(6));

        // Creates two list contents.
        Ytbmultilist listContent1 = new DefaultYtbmultilist
                .YtbmultilistBuilder().check(bigIntegerList).build();
        Ytbmultilist listContent2 = new DefaultYtbmultilist
                .YtbmultilistBuilder().check(bigIntegerList1).build();

        List<Ytbmultilist> ytbmultilists = new ArrayList<>();
        ytbmultilists.add(listContent1);
        ytbmultilists.add(listContent2);

        // Creates module-a with two list contents created.
        YtbMultiModulea modulea = new YtbMultiModuleaOpParam
                .YtbMultiModuleaBuilder().ytbmultilist(ytbmultilists).build();

        // Creates list of string for leaf-list under list1.
        List<String> stringList = new ArrayList<>();
        stringList.add(ANT);
        stringList.add(ANIMAL);
        stringList.add(BIRD);

        // Creates list of string for leaf-list under list2.
        List<String> stringList1 = new ArrayList<>();
        stringList1.add(CATCH);
        stringList1.add(BALL);
        stringList1.add(BAT);

        // Creates two list contents.
        Ytbmultilistb listContent3 = new DefaultYtbmultilistb
                .YtbmultilistbBuilder().checkin(stringList).build();
        Ytbmultilistb listContent4 = new DefaultYtbmultilistb
                .YtbmultilistbBuilder().checkin(stringList1).build();

        List<Ytbmultilistb> ytbMultiListb = new ArrayList<>();
        ytbMultiListb.add(listContent3);
        ytbMultiListb.add(listContent4);

        // Creates module-b with two list contents created.
        YtbMultiModuleb moduleb = new YtbMultiModulebOpParam
                .YtbMultiModulebBuilder().ytbmultilistb(ytbMultiListb).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> listOfModules = new ArrayList<>();
        listOfModules.add(modulea);
        listOfModules.add(moduleb);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                listOfModules, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Checks module-a under root node.
        YdtContext moduleA = context.getFirstChild();
        assertThat(getInCrtName(MODULE, MUL_MOD_A), moduleA.getName(),
                   is(MUL_MOD_A));

        // Checks list-a in module-a and its respective leaf-list.
        YdtContext list1InModuleA = moduleA.getFirstChild();
        assertThat(getInCrtName(LIST, MUL_LIST_A), list1InModuleA.getName(),
                   is(MUL_LIST_A));

        YdtContext leafListA = list1InModuleA.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, CHECK), leafListA.getName(),
                   is(CHECK));

        Set<String> valueA = leafListA.getValueSet();
        assertThat(getInCrtLeafListValue(CHECK, ONE), valueA.contains(ONE),
                   is(true));
        assertThat(getInCrtLeafListValue(CHECK, TWO), valueA.contains(TWO),
                   is(true));
        assertThat(getInCrtLeafListValue(CHECK, THREE), valueA.contains(THREE),
                   is(true));

        // Checks list-b in module-a and its respective leaf-list.
        YdtContext list2InModuleA = list1InModuleA.getNextSibling();
        assertThat(getInCrtName(LIST, MUL_LIST_A), list2InModuleA.getName(),
                   is(MUL_LIST_A));

        YdtContext leafListB = list2InModuleA.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, CHECK), leafListB.getName(),
                   is(CHECK));

        Set<String> valueB = leafListB.getValueSet();
        assertThat(getInCrtLeafListValue(CHECK, FOUR), valueB.contains(FOUR),
                   is(true));
        assertThat(getInCrtLeafListValue(CHECK, FIVE), valueB.contains(FIVE),
                   is(true));
        assertThat(getInCrtLeafListValue(CHECK, SIX), valueB.contains(SIX),
                   is(true));

        // Checks module-b under root node.
        YdtContext moduleB = moduleA.getNextSibling();
        assertThat(getInCrtName(MODULE, MUL_MOD_B), moduleB.getName(),
                   is(MUL_MOD_B));

        // Checks list-a in module-b and its respective leaf-list.
        YdtContext list1InModuleB = moduleB.getFirstChild();
        assertThat(getInCrtName(LIST, MUL_LIST_B), list1InModuleB.getName(),
                   is(MUL_LIST_B));

        YdtContext leafListC = list1InModuleB.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, CHECKIN), leafListC.getName(),
                   is(CHECKIN));

        Set<String> valueC = leafListC.getValueSet();
        assertThat(getInCrtLeafListValue(CHECKIN, ANT), valueC.contains(ANT),
                   is(true));
        assertThat(getInCrtLeafListValue(CHECKIN, ANIMAL),
                   valueC.contains(ANIMAL), is(true));
        assertThat(getInCrtLeafListValue(CHECKIN, BIRD),
                   valueC.contains(BIRD), is(true));

        // Checks list-b in module-b and its respective leaf-list.
        YdtContext list2InModuleB = list1InModuleB.getNextSibling();
        assertThat(getInCrtName(LIST, MUL_LIST_B), list2InModuleB.getName(),
                   is(MUL_LIST_B));

        YdtContext leafListD = list2InModuleB.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, CHECKIN), leafListD.getName(),
                   is(CHECKIN));

        Set<String> valueD = leafListD.getValueSet();
        assertThat(getInCrtLeafListValue(CHECKIN, CATCH),
                   valueD.contains(CATCH), is(true));
        assertThat(getInCrtLeafListValue(CHECKIN, BALL),
                   valueD.contains(BALL), is(true));
        assertThat(getInCrtLeafListValue(CHECKIN, BAT),
                   valueD.contains(BAT), is(true));
    }

    /**
     * Processes tree building when a list node is having list inside it.
     */
    @Test
    public void processTreeBuilderForListHavingList() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates two binary leaf-lists for two list app areas.
        List<byte[]> destArea1 = new ArrayList<>();
        byte[] arr = new byte[]{1, 6, 3};
        byte[] arr1 = new byte[]{2, 7, 4};
        destArea1.add(arr);
        destArea1.add(arr1);

        List<byte[]> destArea2 = new ArrayList<>();
        byte[] arr2 = new byte[]{3, 8, 4};
        byte[] arr3 = new byte[]{5, 6, 1};
        destArea2.add(arr2);
        destArea2.add(arr3);

        // Creates two app areas list.
        ApplicationAreas appArea1 = new DefaultApplicationAreas
                .ApplicationAreasBuilder().destinationAreas(destArea1).build();
        ApplicationAreas appArea2 = new DefaultApplicationAreas
                .ApplicationAreasBuilder().destinationAreas(destArea2).build();

        List<ApplicationAreas> applicationAreasList = new ArrayList<>();
        applicationAreasList.add(appArea1);
        applicationAreasList.add(appArea2);

        // Adds two lists under the multiplex list for content 1.
        Multiplexes mpx1 = new DefaultMultiplexes.MultiplexesBuilder()
                .types(TypesEnum.TIME_DIVISION)
                .applicationAreas(applicationAreasList).build();

        // Creates two binary leaf-lists for two list app areas.
        List<byte[]> destArea3 = new ArrayList<>();
        byte[] arrB = new byte[]{0, 0, 1};
        byte[] arr1B = new byte[]{1, 0, 0};
        destArea3.add(arrB);
        destArea3.add(arr1B);

        List<byte[]> destArea4 = new ArrayList<>();
        byte[] arr2B = new byte[]{7, 7, 7};
        byte[] arr3B = new byte[]{0, 1};
        destArea4.add(arr2B);
        destArea4.add(arr3B);

        // Creates two app areas list.
        ApplicationAreas appArea3 = new DefaultApplicationAreas
                .ApplicationAreasBuilder().destinationAreas(destArea3).build();
        ApplicationAreas appArea4 = new DefaultApplicationAreas
                .ApplicationAreasBuilder().destinationAreas(destArea4).build();

        List<ApplicationAreas> applicationAreasListB = new ArrayList<>();
        applicationAreasListB.add(appArea3);
        applicationAreasListB.add(appArea4);

        // Adds two lists under the multiplex list for content 2.
        Multiplexes mpx2 = new DefaultMultiplexes.MultiplexesBuilder()
                .types(TypesEnum.FREQUENCY_DIVISION)
                .applicationAreas(applicationAreasListB).build();

        List<Multiplexes> multiplexList = new ArrayList<>();
        multiplexList.add(mpx1);
        multiplexList.add(mpx2);

        // Sets it in the container carrier.
        Carrier carrier = new DefaultCarrier.CarrierBuilder()
                .multiplexes(multiplexList).build();

        YtbTreeBuilderForListHavingList listWithList = new
                YtbTreeBuilderForListHavingListOpParam
                        .YtbTreeBuilderForListHavingListBuilder()
                .carrier(carrier).build();

        // As YSB or YAB protocol sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(listWithList);

        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                QUERY_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(LIST, LIST_WITH_LIST), module.getName(),
                   is(LIST_WITH_LIST));

        // Checks the container node under module node.
        YdtContext container = module.getFirstChild();
        assertThat(getInCrtName(CONTAINER, CONT_CARRIER), container.getName(),
                   is(CONT_CARRIER));

        // Checks the list node with content 1 of multiplex.
        YdtContext mtx1 = container.getFirstChild();
        assertThat(getInCrtName(LIST, LIST_MULTIPLEXES), mtx1.getName(),
                   is(LIST_MULTIPLEXES));

        // Checks enum leaf under multiplex of content1.
        YdtContext enumLeaf1 = mtx1.getFirstChild();
        assertThat(getInCrtName(LEAF, TYPES), enumLeaf1.getName(), is(TYPES));
        assertThat(getInCrtLeafValue(TYPES, TIME_DIVISION),
                   enumLeaf1.getValue(), is(TIME_DIVISION));

        // Checks list app area content 1 under multiplex content 1.
        YdtContext appAreaList1 = enumLeaf1.getNextSibling();
        assertThat(getInCrtName(LIST, APP_AREA_LIST), appAreaList1.getName(),
                   is(APP_AREA_LIST));

        YdtContext leafList1 = appAreaList1.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, DEST_AREA), leafList1.getName(),
                   is(DEST_AREA));
        Set value1 = leafList1.getValueSet();
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_6),
                   value1.contains(BIN_VAL_6), is(true));
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_7),
                   value1.contains(BIN_VAL_7), is(true));

        // Checks list app area content 2 under multiplex content 1.
        YdtContext appAreaList2 = appAreaList1.getNextSibling();
        assertThat(getInCrtName(LIST, APP_AREA_LIST), appAreaList2.getName(),
                   is(APP_AREA_LIST));

        YdtContext leafList2 = appAreaList2.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, DEST_AREA), leafList2.getName(),
                   is(DEST_AREA));
        Set value2 = leafList2.getValueSet();
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_8),
                   value2.contains(BIN_VAL_8), is(true));
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_9),
                   value2.contains(BIN_VAL_9), is(true));

        // Checks the list node with content 2 of multiplex.
        YdtContext mtx2 = mtx1.getNextSibling();
        assertThat(getInCrtName(LIST, LIST_MULTIPLEXES), mtx2.getName(),
                   is(LIST_MULTIPLEXES));

        // Checks enum leaf under multiplex of content2.
        YdtContext enumLeaf2 = mtx2.getFirstChild();
        assertThat(getInCrtName(LEAF, TYPES), enumLeaf2.getName(), is(TYPES));
        assertThat(getInCrtLeafValue(TYPES, FREQUENCY_DIV),
                   enumLeaf2.getValue(), is(FREQUENCY_DIV));

        // Checks list app area content 1 under multiplex content 2.
        YdtContext appAreaList3 = enumLeaf2.getNextSibling();
        assertThat(getInCrtName(LIST, APP_AREA_LIST), appAreaList3.getName(),
                   is(APP_AREA_LIST));

        YdtContext leafList3 = appAreaList3.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, DEST_AREA), leafList3.getName(),
                   is(DEST_AREA));
        Set value3 = leafList3.getValueSet();
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_10),
                   value3.contains(BIN_VAL_10), is(true));
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_11),
                   value3.contains(BIN_VAL_11), is(true));

        // Checks list app area content 2 under multiplex content 2.
        YdtContext appAreaList4 = appAreaList3.getNextSibling();
        assertThat(getInCrtName(LIST, APP_AREA_LIST), appAreaList4.getName(),
                   is(APP_AREA_LIST));

        YdtContext leafList4 = appAreaList4.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, DEST_AREA), leafList4.getName(),
                   is(DEST_AREA));
        Set value4 = leafList4.getValueSet();
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_12),
                   value4.contains(BIN_VAL_12), is(true));
        assertThat(getInCrtLeafListValue(DEST_AREA, BIN_VAL_13),
                   value4.contains(BIN_VAL_13), is(true));
    }

    /**
     * Processes tree building from the derived type of leaf and leaf-list
     * having binary and bits .
     */
    @Test
    public void processTreeBuilderForBinaryAndBits() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates a byte array for binary leaf.
        byte[] binLeaf = new byte[]{5, 5, 5};

        // Assigns the value in the chained loop of typedef.
        Derivedbinaryb derBinb = new Derivedbinaryb(binLeaf);
        Derivedbinarya derBina = new Derivedbinarya(derBinb);

        // Creates bit set for bit leaf.
        BitSet bitLeaf = new BitSet();
        bitLeaf.set(1);
        bitLeaf.set(100);

        // Assigns the value in the chained loop of typedef.
        Derivedbitsb derBitb = new Derivedbitsb(bitLeaf);
        Derivedbitsa derBita = new Derivedbitsa(derBitb);

        // Creates a byte array list for binary leaf-list.
        byte[] binList1 = new byte[]{9, 9, 0};
        byte[] binList2 = new byte[]{12, 6, 0};
        byte[] binList3 = new byte[]{16, 29, 25};

        // Assigns the value in the chained loop of typedef.
        Derivedbinaryb derBinBList1 = new Derivedbinaryb(binList1);
        Derivedbinaryb derBinBList2 = new Derivedbinaryb(binList2);
        Derivedbinaryb derBinBList3 = new Derivedbinaryb(binList3);

        Derivedbinarya derBinAList1 = new Derivedbinarya(derBinBList1);
        Derivedbinarya derBinAList2 = new Derivedbinarya(derBinBList2);
        Derivedbinarya derBinAList3 = new Derivedbinarya(derBinBList3);

        List<Derivedbinarya> binAlist = new ArrayList<>();
        binAlist.add(derBinAList1);
        binAlist.add(derBinAList2);
        binAlist.add(derBinAList3);

        // Creates a bit set list for bit leaf-list.
        BitSet bitList1 = new BitSet();
        bitList1.set(1);
        BitSet bitList2 = new BitSet();
        bitList2.set(1);
        bitList2.set(10);
        BitSet bitList3 = new BitSet();
        bitList3.set(1);
        bitList3.set(10);
        bitList3.set(100);

        // Assigns the value in the chained loop of typedef.
        Derivedbitsb bitBlist1 = new Derivedbitsb(bitList1);
        Derivedbitsb bitBlist2 = new Derivedbitsb(bitList2);
        Derivedbitsb bitBlist3 = new Derivedbitsb(bitList3);

        Derivedbitsa bitAlist1 = new Derivedbitsa(bitBlist1);
        Derivedbitsa bitAlist2 = new Derivedbitsa(bitBlist2);
        Derivedbitsa bitAlist3 = new Derivedbitsa(bitBlist3);

        List<Derivedbitsa> bitAlist = new ArrayList<>();
        bitAlist.add(bitAlist1);
        bitAlist.add(bitAlist2);
        bitAlist.add(bitAlist3);

        // Creates a module by assigning all the leaf and leaf-list.
        YtbDerivedTypeWithBitsAndBinary bitsAndBinary = new
                YtbDerivedTypeWithBitsAndBinaryOpParam
                        .YtbDerivedTypeWithBitsAndBinaryBuilder()
                .forbinary(derBina).forbits(derBita)
                .forbinarylist(binAlist)
                .forbitslist(bitAlist).build();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(bitsAndBinary);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, MOD_BIT_BIN), module.getName(),
                   is(MOD_BIT_BIN));

        // Checks the leaf for binary.
        YdtContext binaryLeaf = module.getFirstChild();
        assertThat(getInCrtName(LEAF, FOR_BINARY), binaryLeaf.getName(),
                   is(FOR_BINARY));
        assertThat(getInCrtLeafValue(FOR_BINARY, BIN_VAL_1),
                   binaryLeaf.getValue(), is(BIN_VAL_1));

        // Checks the leaf for bits.
        YdtContext bitsLeaf = binaryLeaf.getNextSibling();
        assertThat(getInCrtName(LEAF, FOR_BITS), bitsLeaf.getName(),
                   is(FOR_BITS));
        assertThat(getInCrtLeafValue(FOR_BITS, BIT_VAL_2),
                   bitsLeaf.getValue(), is(BIT_VAL_2));

        // Checks the leaf-list for binary.
        YdtContext binaryLeafList = bitsLeaf.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, FOR_BINARY_LIST),
                   binaryLeafList.getName(), is(FOR_BINARY_LIST));

        Set value2 = binaryLeafList.getValueSet();
        assertThat(getInCrtLeafListValue(FOR_BINARY_LIST, BIN_VAL_2),
                   value2.contains(BIN_VAL_2), is(true));
        assertThat(getInCrtLeafListValue(FOR_BINARY_LIST, BIN_VAL_3),
                   value2.contains(BIN_VAL_3), is(true));
        assertThat(getInCrtLeafListValue(FOR_BINARY_LIST, BIN_VAL_4),
                   value2.contains(BIN_VAL_4), is(true));

        // Checks the leaf-list for bits.
        YdtContext bitsLeafList = binaryLeafList.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, FOR_BITS_LIST),
                   bitsLeafList.getName(), is(FOR_BITS_LIST));
        Set value3 = bitsLeafList.getValueSet();
        assertThat(getInCrtLeafListValue(FOR_BITS_LIST, BIT_VAL_3),
                   value3.contains(BIT_VAL_3), is(true));
        assertThat(getInCrtLeafListValue(FOR_BITS_LIST, BIT_VAL_4),
                   value3.contains(BIT_VAL_4), is(true));
        assertThat(getInCrtLeafListValue(FOR_BITS_LIST, BIT_VAL_5),
                   value3.contains(BIT_VAL_5), is(true));
    }

    /**
     * Processes tree building for the union type.
     */
    @Test
    public void processYtbUnionType() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates union with binary type.
        byte[] binary = new byte[]{0, 0, 0};
        ForunionUnion union = new ForunionUnion(binary);

        // Creates module with union.
        YtbDerivedTypeWithBitsAndBinary unionType = new
                YtbDerivedTypeWithBitsAndBinaryOpParam
                        .YtbDerivedTypeWithBitsAndBinaryBuilder()
                .forunion(union)
                .build();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(unionType);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext rootNode = ydtBuilder.getRootNode();
        YdtContext module = rootNode.getFirstChild();
        YdtContext unionChild = module.getFirstChild();
        assertThat(getInCrtName(LEAF, FOR_UNION), unionChild.getName(),
                   is(FOR_UNION));
        assertThat(getInCrtLeafValue(FOR_UNION, BIN_VAL_5),
                   unionChild.getValue(), is(BIN_VAL_5));
    }
}
