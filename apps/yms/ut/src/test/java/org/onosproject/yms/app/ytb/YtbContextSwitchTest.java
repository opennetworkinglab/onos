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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ymsietfinettypes.Uri;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.YmsIetfNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.YmsIetfNetworkOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.DefaultNetworks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.DefaultNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.network.DefaultNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.network.Node;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.network.node.DefaultSupportingNode;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networks.network.node.SupportingNode;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.AugmentedNdNode;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.DefaultAugmentedNdNode;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.augmentedndnode.DefaultTerminationPoint;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.augmentedndnode.TerminationPoint;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.augmentedndnode.terminationpoint.DefaultSupportingTerminationPoint;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.from.another.file.rev20160826.ytbaugmentfromanotherfile.networks.network.node.augmentedndnode.terminationpoint.SupportingTerminationPoint;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput.activatesoftwareimage.output.AugmentedRpcOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput.activatesoftwareimage.output.DefaultAugmentedRpcOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput.activatesoftwareimage.output.augmentedrpcoutput.Selection;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput.activatesoftwareimage.output.augmentedrpcoutput.selection.DefaultValueIn;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput.activatesoftwareimage.output.augmentedrpcoutput.selection.valuein.ValueIn;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput2.activatesoftwareimage.output.AugmentedInputOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput2.activatesoftwareimage.output.DefaultAugmentedInputOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput2.activatesoftwareimage.output.augmentedinputoutput.DefaultFriction;
import org.onosproject.yang.gen.v1.yms.test.ytb.augment.yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput2.activatesoftwareimage.output.augmentedinputoutput.Friction;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.YtbChoiceWithContainerAndLeafList;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.YtbChoiceWithContainerAndLeafListOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.ContentTest;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.CurrentValue;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.DefaultChoiceContainer;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.choicecontainer.ChoiceContainer;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.choicecontainer.choicecontainer.DefaultPredict;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.choicecontainer.choicecontainer.Predict;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.choicecontainer.choicecontainer.predict.DefaultReproduce;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.contenttest.choicecontainer.choicecontainer.predict.Reproduce;
import org.onosproject.yang.gen.v1.yms.test.ytb.choice.with.container.and.leaf.yangautoprefixlist.rev20160826.ytbchoicewithcontainerandleaflist.currentvalue.DefaultYtbAbsent;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.ActivateSoftwareImageOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.DefaultActivateSoftwareImageOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.activatesoftwareimageoutput.DefaultOutputList;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.activatesoftwareimageoutput.OutputList;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.activatesoftwareimageoutput.outputlist.ContentInside;
import org.onosproject.yang.gen.v1.yms.test.ytb.rpc.response.with.advanced.input.and.output.rev20160826.ytbrpcresponsewithadvancedinputandoutput.activatesoftwareimage.activatesoftwareimageoutput.outputlist.DefaultContentInside;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.YtbSimpleAugment;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.YtbSimpleAugmentOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.Cont1;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.DefaultCont1;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.DefaultCont2;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.cont2.AugmentedCont2;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.cont2.DefaultAugmentedCont2;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.cont2.augmentedcont2.Cont1s;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.cont2.augmentedcont2.DefaultCont1s;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.YtbSimpleChoiceCase;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.YtbSimpleChoiceCaseOpParam;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.ytbsimplechoicecase.DefaultYtbFood;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.ytbsimplechoicecase.YtbFood;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.ytbsimplechoicecase.ytbfood.YtbSnack;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.choice.yangautoprefixcase.rev20160826.ytbsimplechoicecase.ytbfood.ytbsnack.DefaultYtbLateNight;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.rpc.DefaultRpcOutput;
import org.onosproject.yang.gen.v1.yms.test.ytb.simple.rpc.response.rev20160826.ytbsimplerpcresponse.rpc.RpcOutput;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REPLY;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REPLY;
import static org.onosproject.yms.ydt.YmsOperationType.RPC_REQUEST;

/**
 * Unit test cases for YANG tree builder for context switch for augment, RPC
 * and case.
 */
public class YtbContextSwitchTest extends YtbErrMsgAndConstants {

    private static final String RPC_ADV_NAME = "RPCAdvanced";
    private static final String RPC_ADV_NAMESPACE = "RPCAdvancedSpace";
    private static final String RPC_ADV_IO =
            "YtbRpcResponseWithAdvancedInputAndOutput";
    private static final String ACT_IMG = "activate-software-image";
    private static final String INPUT = "input";
    private static final String FINAL = "final";
    private static final String AUG_NW_REF_1 = "network-ref-aug1";
    private static final String AUG_NODE_REF_1 = "node-ref-aug1";
    private static final String AUG_TP_REF_1 = "tp-ref-aug-1";
    private static final String AUG_TP_ID_1 = "tp-id-aug-1";
    private static final String AUG_NW_REF_B1 = "network-ref-augb1";
    private static final String AUG_NODE_REF_B1 = "node-ref-augb1";
    private static final String AUG_TP_REF_B1 = "tp-ref-aug-b1";
    private static final String AUG_TP_ID_B1 = "tp-id-aug-1b";
    private static final String NW_REF = "network-ref";
    private static final String NODE_REF = "node-ref";
    private static final String NW_REF_2 = "network-ref2";
    private static final String NODE_REF_3 = "node-ref3";
    private static final String NW_REF_B = "network-ref-b";
    private static final String NODE_REF_B = "node-ref-b";
    private static final String NW_REF_2B = "network-ref2-b";
    private static final String NODE_REF_2B = "node-ref2-b";
    private static final String NODE_REF_3B = "node-ref3-b";
    private static final String CHOC = "choc";
    private static final String CHOICE_CASE = "YtbSimpleChoiceCase";
    private static final String FOOD = "YtbFood";
    private static final String CHOCOLATE = "chocolate";
    private static final String VAL = "val";
    private static final String IND = "ind";
    private static final String CHOICE_ROOT_NAME = "choiceContainerRootName";
    private static final String CHOICE_ROOT_NAMESPACE =
            "choiceContainerRootNamespace";
    private static final String CHOICE_CONT =
            "YtbChoiceWithContainerAndLeafList";
    private static final String CONT_CHOICE = "choice-container";
    private static final String REPRODUCE = "reproduce";
    private static final String NINETY = "90";
    private static final String HUNDRED = "100";
    private static final String RPC_RT_NAME = "rpcRootName";
    private static final String RPC_RT_NAMESPACE = "rpcRootNameSpace";
    private static final String OUTPUT_LEAF = "output-leaf";
    private static final String FIVE_HUNDRED = "500";
    private static final String OUTPUT_LIST = "output-list";
    private static final String LIST_KEY = "list-key";
    private static final String BIN_VAL_1 = "AAE=";
    private static final String CONT_INSIDE = "content_inside";
    private static final String BIN_VAL_2 = "CAk=";
    private static final String AVAILABLE = "available";
    private static final String EIGHTY_NINE = "89";
    private static final String NINETY_EIGHT = "98";
    private static final String BIN_VAL_3 = "AAA=";
    private static final String SIM_AUG = "simpleAugment";
    private static final String SIM_AUG_NAMESPACE = "simpleAugmentSpace";
    private static final String SIMPLE_AUG = "YtbSimpleAugment";
    private static final String CONT1 = "cont1";
    private static final String CONT2 = "cont2";
    private static final String LEAF4 = "leaf4";
    private static final String CONT1S = "cont1s";
    private static final String INTER_AUG = "inter-file-augment";
    private static final String INTER_AUG_NAMESPACE =
            "inter-file-augment-space";
    private static final String IETF_NW = "yms-ietf-network";
    private static final String NWS = "networks";
    private static final String NW = "network";
    private static final String NODE = "node";
    private static final String NODE_ID = "node-id";
    private static final String TERM_POINT = "termination-point";
    private static final String TP_ID = "tp-id";
    private static final String SUP_TERM_POINT = "supporting-termination-point";
    private static final String TP_REF = "tp-ref";
    private static final String SUP_NODE = "supporting-node";
    private static final String KIN1 = "kin1";
    private static final String KIN2 = "kin2";

    /**
     * Creates object as like an application for RPC with list.
     *
     * @return object of RPC
     */
    private List<OutputList> createApplicationBuiltObjectForRpc() {

        // Creates a empty container inside without leaf for list1.
        ContentInside inside1 = new DefaultContentInside.ContentInsideBuilder()
                .build();

        // Creates a leaf list-key which is a leaf ref.
        byte[] listKey1 = new byte[]{0, 1};

        // Creates the list content 1.
        OutputList output1 = new DefaultOutputList.OutputListBuilder()
                .listKey(listKey1).contentInside(inside1).build();

        // Creates a list of leaf for available.
        List<Short> avail = new ArrayList<>();
        avail.add((short) 89);
        avail.add((short) 98);

        // Adds the leaf list in the inside container.
        ContentInside inside2 = new DefaultContentInside.ContentInsideBuilder()
                .available(avail).build();

        // Creates a leaf, list-key which is a leaf ref.
        byte[] listKey2 = new byte[]{8, 9};

        // Creates the list content 2.
        OutputList outputList2 = new DefaultOutputList.OutputListBuilder()
                .listKey(listKey2).contentInside(inside2).build();

        // Creates only leaf, list-key which is a leaf ref.
        byte[] arr3 = new byte[]{0, 0};

        // Creates the list content 3.
        OutputList outputList3 = new DefaultOutputList.OutputListBuilder()
                .listKey(arr3).build();

        // Adds all the list contents in array list and gives returns it.
        List<OutputList> outputLists = new ArrayList<>();
        outputLists.add(output1);
        outputLists.add(outputList2);
        outputLists.add(outputList3);
        return outputLists;
    }

    /**
     * Builds YANG request work bench for RPC with container input.
     *
     * @param registry schema registry
     * @return YANG request work bench
     */
    private YangRequestWorkBench buildYangRequestWorkBenchForRpc(
            DefaultYangSchemaRegistry registry) {

        // Creates a request work bench and adds the input child into it.
        YangRequestWorkBench workBench = new YangRequestWorkBench(
                RPC_ADV_NAME, RPC_ADV_NAMESPACE, RPC_REQUEST,
                registry, true);
        Set<String> valueList = new HashSet<>();
        valueList.add("800");
        valueList.add("900");
        workBench.addChild(RPC_ADV_IO, null, NONE);
        workBench.addChild(ACT_IMG, null, NONE);
        workBench.addChild(INPUT, null, NONE);
        workBench.addChild(FINAL, null, NONE);
        workBench.addLeaf("value", null, valueList);
        return workBench;
    }

    /**
     * Creates an application object for inter file augment.
     *
     * @return application object
     */
    private Object createObjectForInterFileAugment() {

        // Creates leaf value for network-ref.
        Uri nwkRef = new Uri(AUG_NW_REF_1);
        NetworkId nwIdUri = new NetworkId(nwkRef);
        Uri nwkRef2 = new Uri("network-ref-aug2");
        NetworkId nwIdUri2 = new NetworkId(nwkRef2);

        // Creates leaf value for node-ref
        Uri nodeRef = new Uri(AUG_NODE_REF_1);
        NodeId nodeId = new NodeId(nodeRef);

        Uri nodeRef2 = new Uri("node-ref-aug2");
        NodeId nodeId2 = new NodeId(nodeRef2);

        // Creates support termination list with the above two contents.
        SupportingTerminationPoint point1 =
                new DefaultSupportingTerminationPoint
                        .SupportingTerminationPointBuilder()
                        .networkRef(nwIdUri).nodeRef(nodeId)
                        .tpRef(AUG_TP_REF_1).build();
        SupportingTerminationPoint point2 =
                new DefaultSupportingTerminationPoint
                        .SupportingTerminationPointBuilder()
                        .networkRef(nwIdUri2).nodeRef(nodeId2)
                        .tpRef("tp-ref-aug-2").build();

        List<SupportingTerminationPoint> pointList = new ArrayList<>();
        pointList.add(point1);
        pointList.add(point2);

        // Adds the list created to the termination point content1.
        TerminationPoint tPoint1 = new DefaultTerminationPoint
                .TerminationPointBuilder()
                .supportingTerminationPoint(pointList)
                .tpId(AUG_TP_ID_1).build();

        // Creates leaf value for network-ref.
        Uri nwkRef3 = new Uri(AUG_NW_REF_B1);
        NetworkId nwIdUri3 = new NetworkId(nwkRef3);
        Uri nwkRef4 = new Uri("network-ref-augb2");
        NetworkId nwIdUri4 = new NetworkId(nwkRef4);

        // Creates leaf value for node-ref
        Uri nodeRef3 = new Uri(AUG_NODE_REF_B1);
        NodeId nodeId3 = new NodeId(nodeRef3);

        Uri nodeRef4 = new Uri("node-ref-augb2");
        NodeId nodeId4 = new NodeId(nodeRef4);

        // Creates support termination list with the above two contents.
        SupportingTerminationPoint point3 =
                new DefaultSupportingTerminationPoint
                        .SupportingTerminationPointBuilder()
                        .networkRef(nwIdUri3).nodeRef(nodeId3)
                        .tpRef(AUG_TP_REF_B1).build();
        SupportingTerminationPoint point4 =
                new DefaultSupportingTerminationPoint
                        .SupportingTerminationPointBuilder()
                        .networkRef(nwIdUri4).nodeRef(nodeId4)
                        .tpRef("tp-ref-aug-b2").build();

        List<SupportingTerminationPoint> pointList2 = new ArrayList<>();
        pointList2.add(point3);
        pointList2.add(point4);

        // Adds the list created to the termination point content2.
        TerminationPoint tPoint2 = new DefaultTerminationPoint
                .TerminationPointBuilder()
                .supportingTerminationPoint(pointList2)
                .tpId(AUG_TP_ID_B1).build();

        List<TerminationPoint> terminationPointList = new ArrayList<>();
        terminationPointList.add(tPoint1);
        terminationPointList.add(tPoint2);

        // Adds all the above contents to the augment.
        AugmentedNdNode augment = new DefaultAugmentedNdNode
                .AugmentedNdNodeBuilder()
                .terminationPoint(terminationPointList)
                .build();

        // Creates leaf value for network-ref in augmented node(ietf-network).
        Uri nwRef5 = new Uri(NW_REF);
        NetworkId nwIdUri5 = new NetworkId(nwRef5);

        //Creates leaf value for node-ref in augmented node(ietf-network).
        Uri nodeRef5 = new Uri(NODE_REF);
        NodeId nodeId5 = new NodeId(nodeRef5);

        // Creates supporting node list content 1 with above contents.
        SupportingNode supNode1 = new DefaultSupportingNode
                .SupportingNodeBuilder().nodeRef(nodeId5)
                .networkRef(nwIdUri5).build();

        // Creates leaf value for network-ref in augmented node(ietf-network).
        Uri nwRef6 = new Uri(NW_REF_2);
        NetworkId nwIdUri6 = new NetworkId(nwRef6);

        //Creates leaf value for node-ref in augmented node(ietf-network).
        Uri nodeRef6 = new Uri("node-ref2");
        NodeId nodeId6 = new NodeId(nodeRef6);

        // Creates supporting node list content 2 with above contents.
        SupportingNode supNode2 = new DefaultSupportingNode
                .SupportingNodeBuilder()
                .nodeRef(nodeId6)
                .networkRef(nwIdUri6).build();

        List<SupportingNode> supNodeList = new ArrayList<>();
        supNodeList.add(supNode1);
        supNodeList.add(supNode2);

        // Creates leaf value for node-id in augmented node(ietf-network).
        Uri nodeId1 = new Uri(NODE_REF_3);
        NodeId nodeIdForId = new NodeId(nodeId1);

        // Creates node list with content 1 by adding augment also.
        DefaultNode.NodeBuilder nodeBuilder = new DefaultNode.NodeBuilder();
        nodeBuilder.addYangAugmentedInfo(
                augment, AugmentedNdNode.class);
        nodeBuilder.supportingNode(supNodeList);
        nodeBuilder.nodeId(nodeIdForId);
        Node node1 = nodeBuilder.build();

        // Creates an augment node without any values set to it.
        AugmentedNdNode augmentedNdNode2 = new DefaultAugmentedNdNode
                .AugmentedNdNodeBuilder().build();

        // Creates leaf value for network-ref in augmented node(ietf-network).
        Uri nwRef7 = new Uri(NW_REF_B);
        NetworkId nwIdUri7 = new NetworkId(nwRef7);
        //Creates leaf value for node-ref in augmented node(ietf-network).
        Uri nodeRef7 = new Uri(NODE_REF_B);
        NodeId nodeId7 = new NodeId(nodeRef7);

        // Creates supporting node list content 1 with above contents.
        SupportingNode supNode3 = new DefaultSupportingNode
                .SupportingNodeBuilder().nodeRef(nodeId7)
                .networkRef(nwIdUri7).build();

        // Creates leaf value for network-ref in augmented node(ietf-network).
        Uri nwRef8 = new Uri(NW_REF_2B);
        NetworkId nwIdUri8 = new NetworkId(nwRef8);

        //Creates leaf value for node-ref in augmented node(ietf-network).
        Uri nodeRef8 = new Uri(NODE_REF_2B);
        NodeId nodeId8 = new NodeId(nodeRef8);

        // Creates supporting node list content 1 with above contents.
        SupportingNode supNode4 = new DefaultSupportingNode
                .SupportingNodeBuilder()
                .nodeRef(nodeId8)
                .networkRef(nwIdUri8).build();

        List<SupportingNode> supNodeList2 = new ArrayList<>();
        supNodeList2.add(supNode3);
        supNodeList2.add(supNode4);

        // Creates leaf value for node-id in augmented node(ietf-network).
        Uri nodeIdLeaf = new Uri(NODE_REF_3B);
        NodeId nodeIdForId2 = new NodeId(nodeIdLeaf);

        // Creates node list with content 2 by adding empty augment also.
        DefaultNode.NodeBuilder nodeBuilder2 = new DefaultNode.NodeBuilder();
        nodeBuilder2.addYangAugmentedInfo(
                augmentedNdNode2, AugmentedNdNode.class);
        nodeBuilder2.supportingNode(supNodeList2);
        nodeBuilder2.nodeId(nodeIdForId2);
        Node node2 = nodeBuilder2.build();

        // Adds both nodes into the list.
        List<Node> nodeList = new LinkedList<>();
        nodeList.add(node1);
        nodeList.add(node2);

        // Adds the list into the network list.
        Network nwkList = new DefaultNetwork.NetworkBuilder()
                .node(nodeList).build();

        List<Network> networkList = new ArrayList<>();
        networkList.add(nwkList);

        // Adds the network list into networks container.
        Networks contNetworks = new DefaultNetworks.NetworksBuilder()
                .network(networkList).build();

        // Adds the container into the module.
        YmsIetfNetwork opParam = new YmsIetfNetworkOpParam
                .YmsIetfNetworkBuilder()
                .networks(contNetworks).build();
        return opParam;
    }

    /**
     * Processes a simple choice case and builds the YDT.
     */
    @Test
    public void processSimpleChoiceCase() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates a choice snack with the case late night.
        YtbSnack lateNight = new DefaultYtbLateNight.YtbLateNightBuilder()
                .chocolate(CHOC).build();

        // Creates container food with the created case.
        YtbFood food = new DefaultYtbFood.YtbFoodBuilder()
                .ytbSnack(lateNight).build();

        // Creates module with the container food.
        YtbSimpleChoiceCase choiceCase = new YtbSimpleChoiceCaseOpParam
                .YtbSimpleChoiceCaseBuilder().ytbFood(food).build();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(choiceCase);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, ROOT_NAME, ROOT_NAME_SPACE,
                EDIT_CONFIG_REPLY, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext rootNode = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = rootNode.getFirstChild();
        assertThat(getInCrtName(MODULE, CHOICE_CASE), module.getName(),
                   is(CHOICE_CASE));

        // Gets the container food from module.
        YdtContext container = module.getFirstChild();
        assertThat(getInCrtName(CONTAINER, FOOD), container.getName(),
                   is(FOOD));

        // Gets the case-leaf from container
        YdtContext caseNode = container.getFirstChild();
        assertThat(getInCrtName(LEAF, CHOCOLATE), caseNode.getName(),
                   is(CHOCOLATE));
        assertThat(getInCrtLeafValue(CHOCOLATE, CHOC), caseNode.getValue(),
                   is(CHOC));
    }

    /**
     * Processes module with two choices and a choice having node and a
     * leaf-list.
     */
    @Test
    public void processChoiceWithNodeAndLeafList() {

        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates reproduce container for list predict-1.
        Reproduce reproduce1 = new DefaultReproduce.ReproduceBuilder()
                .yangAutoPrefixCatch((short) 90).build();

        // Assigns predict-1 with the container.
        Predict predict1 = new DefaultPredict.PredictBuilder()
                .reproduce(reproduce1).build();

        // Creates reproduce container for list predict-2.
        Reproduce reproduce2 = new DefaultReproduce.ReproduceBuilder()
                .yangAutoPrefixCatch((short) 100).build();

        // Assigns predict-2 with the container.
        Predict predict2 = new DefaultPredict.PredictBuilder()
                .reproduce(reproduce2).build();

        List<Predict> predictList = new ArrayList<>();
        predictList.add(predict1);
        predictList.add(predict2);

        // Case container is added to the choice content-test.
        ChoiceContainer containerCase = new org.onosproject.yang.gen.v1.yms
                .test.ytb.choice.with.container.and.leaf.yangautoprefixlist
                .rev20160826.ytbchoicewithcontainerandleaflist.contenttest
                .choicecontainer.DefaultChoiceContainer.ChoiceContainerBuilder()
                .predict(predictList).build();

        // Case container is added to the choice content-test.
        ContentTest contentTest = new DefaultChoiceContainer
                .ChoiceContainerBuilder().choiceContainer(containerCase).build();

        // Creates string list for leaf-list final.
        List<String> stringList = new ArrayList<>();
        stringList.add(VAL);
        stringList.add(IND);

        // For choice current value, the leaf list gets added as case.
        CurrentValue currentValue = new DefaultYtbAbsent.YtbAbsentBuilder()
                .yangAutoPrefixFinal(stringList).build();

        // Adds choice as child to the module.
        YtbChoiceWithContainerAndLeafList choiceWithContainerAndLeafList =
                new YtbChoiceWithContainerAndLeafListOpParam
                        .YtbChoiceWithContainerAndLeafListBuilder()
                        .contentTest(contentTest).currentValue(currentValue)
                        .build();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(choiceWithContainerAndLeafList);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, CHOICE_ROOT_NAME, CHOICE_ROOT_NAMESPACE,
                QUERY_CONFIG_REPLY, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, CHOICE_CONT), module.getName(),
                   is(CHOICE_CONT));

        // Gets the first choice content under the module, as container.
        YdtContext choice1 = module.getFirstChild();
        assertThat(getInCrtName(CONTAINER, CONT_CHOICE), choice1.getName(),
                   is(CONT_CHOICE));

        // Gets the first content in the list predict.
        YdtContext list1 = choice1.getFirstChild();
        assertThat(getInCrtName(LIST, PREDICT), list1.getName(), is(PREDICT));

        // Gets the container and its child leaf in the list predict.
        YdtContext container1 = list1.getFirstChild();
        assertThat(getInCrtName(CONTAINER, REPRODUCE), container1.getName(),
                   is(REPRODUCE));
        YdtContext leaf1 = container1.getFirstChild();
        assertThat(getInCrtName(LEAF, CATCH), leaf1.getName(), is(CATCH));
        assertThat(getInCrtLeafValue(CATCH, NINETY), leaf1.getValue(),
                   is(NINETY));

        // Gets the second content in the list predict.
        YdtContext list2 = list1.getNextSibling();
        assertThat(getInCrtName(LIST, PREDICT), list2.getName(), is(PREDICT));

        // Gets the container and its child leaf in the list predict.
        YdtContext container2 = list2.getFirstChild();
        assertThat(getInCrtName(CONTAINER, REPRODUCE), container2.getName(),
                   is(REPRODUCE));
        YdtContext leaf2 = container2.getFirstChild();
        assertThat(getInCrtName(LEAF, CATCH), leaf2.getName(), is(CATCH));
        assertThat(getInCrtLeafValue(CATCH, HUNDRED), leaf2.getValue(),
                   is(HUNDRED));

        // Gets the second choice content under the module, as leaf-list.
        YdtContext choice2 = choice1.getNextSibling();
        assertThat(getInCrtName(LEAF_LIST, FINAL), choice2.getName(),
                   is(FINAL));
        Set value2 = choice2.getValueSet();
        assertThat(getInCrtLeafListValue(FINAL, VAL), value2.contains(VAL),
                   is(true));
        assertThat(getInCrtLeafListValue(FINAL, IND), value2.contains(IND),
                   is(true));
    }

    /**
     * Processes RPC response of a simple output with only a leaf content
     * inside.
     */
    @Test
    public void processSimpleRpcResponse() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.
        RpcOutput output = new DefaultRpcOutput.RpcOutputBuilder()
                .outputLeaf(500).build();

        // Creates request work bench of rpc.
        YangRequestWorkBench workBench = new YangRequestWorkBench(
                RPC_RT_NAME, RPC_RT_NAMESPACE, RPC_REQUEST, registry, true);
        workBench.addChild(RPC_NAME, null, NONE);
        workBench.addChild(RPC, null, NONE);
        workBench.addChild(INPUT, null, NONE);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtForRpcResponse(
                output, workBench);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, RPC_NAME), module.getName(),
                   is(RPC_NAME));

        // Gets the rpc node from the module.
        YdtContext rpc = module.getFirstChild();
        assertThat(getInCrtName(RPC, RPC), rpc.getName(), is(RPC));

        // Gets the output node from the module.
        // TODO: Change assert after YANG utils is merged.
        YdtContext rpcOutput = rpc.getFirstChild();
        //assertThat(rpcOutputNode.getName(), is("output"));

        YdtContext outputLeaf = rpcOutput.getFirstChild();
        assertThat(getInCrtName(LEAF, OUTPUT_LEAF), outputLeaf.getName(),
                   is(OUTPUT_LEAF));
        assertThat(getInCrtLeafValue(OUTPUT_LEAF, FIVE_HUNDRED),
                   outputLeaf.getValue(), is(FIVE_HUNDRED));
    }

    /**
     * Processes RPC response of an output defined with list.
     */
    @Test
    public void processRpcResponseForAdvInputOutput() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.
        List<OutputList> list = createApplicationBuiltObjectForRpc();
        ActivateSoftwareImageOutput output =
                new DefaultActivateSoftwareImageOutput
                        .ActivateSoftwareImageOutputBuilder()
                        .outputList(list).build();

        // Creates request work bench of rpc.
        YangRequestWorkBench workBench = buildYangRequestWorkBenchForRpc(
                registry);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtForRpcResponse(
                output, workBench);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, RPC_ADV_IO), module.getName(),
                   is(RPC_ADV_IO));

        // Gets the rpc node from module.
        YdtContext rpc = module.getFirstChild();
        assertThat(getInCrtName(RPC, ACT_IMG), rpc.getName(), is(ACT_IMG));

        // Gets the output node from the module.
        // TODO: Change assert after YANG utils is merged.
        YdtContext rpcOutput = rpc.getFirstChild();
        //assertThat(rpcOutputNode.getName(), is("output"));

        // Gets the list content 1 as the node from output.
        YdtContext outputList1 = rpcOutput.getFirstChild();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList1.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf key-list from list content1.
        YdtContext keyList1 = outputList1.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList1.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_1), keyList1.getValue(),
                   is(BIN_VAL_1));

        // Gets the content inside container from list content 1.
        YdtContext cont1 = keyList1.getNextSibling();
        assertThat(getInCrtName(CONTAINER, CONT_INSIDE), cont1.getName(),
                   is(CONT_INSIDE));

        // Gets the list content 2 as the node from output.
        YdtContext outputList2 = outputList1.getNextSibling();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList2.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf-list key-list from list content2.
        YdtContext keyList2 = outputList2.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList2.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_2), keyList2.getValue(),
                   is(BIN_VAL_2));

        // Gets the content inside container from list content 2.
        YdtContext cont2 = keyList2.getNextSibling();
        assertThat(getInCrtName(CONTAINER, CONT_INSIDE), cont2.getName(),
                   is(CONT_INSIDE));

        // Gets the leaf-list available inside container.
        YdtContext availLeafList = cont2.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, AVAILABLE), availLeafList.getName(),
                   is(AVAILABLE));
        Set value1 = availLeafList.getValueSet();
        assertThat(getInCrtLeafListValue(AVAILABLE, EIGHTY_NINE),
                   value1.contains(EIGHTY_NINE), is(true));
        assertThat(getInCrtLeafListValue(AVAILABLE, NINETY_EIGHT),
                   value1.contains(NINETY_EIGHT), is(true));

        // Gets the list content 3.
        YdtContext outputList3 = outputList2.getNextSibling();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList3.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf list-key in content 3 of list.
        YdtContext keyList3 = outputList3.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList3.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_3), keyList3.getValue(),
                   is(BIN_VAL_3));
    }

    /**
     * Processes simple self augment file with leaf and container inside
     * augment.
     */
    @Test
    public void processSimpleAugment() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.

        // Creates container cont1s with the leaf.
        org.onosproject.yang.gen.v1.yms.test.ytb.simple.augment.rev20160826
                .ytbsimpleaugment.cont1.cont2.augmentedcont2.cont1s
                .Cont1s cont1s1 = new org.onosproject.yang.gen.v1.yms.test
                .ytb.simple.augment.rev20160826.ytbsimpleaugment.cont1.cont2
                .augmentedcont2.cont1s.DefaultCont1s.Cont1sBuilder().build();

        // Appends the created container into another container.
        Cont1s cont1s = new DefaultCont1s.Cont1sBuilder()
                .cont1s(cont1s1).build();

        // Creates augment with the container and leaf.
        AugmentedCont2 augment = new DefaultAugmentedCont2
                .AugmentedCont2Builder().cont1s(cont1s).leaf4(500).build();

        // Creates for the node which will be getting augmented.
        // Creates cont2 where content will be augmented into.
        DefaultCont2.Cont2Builder augCont2 = new DefaultCont2
                .Cont2Builder();
        augCont2.addYangAugmentedInfo(augment, AugmentedCont2.class);

        // Creates cont1 where cont2 is added.
        Cont1 cont1 = new DefaultCont1.Cont1Builder()
                .cont2(augCont2.build()).build();

        // Creates module with the nodes inside.
        YtbSimpleAugment simpleAugment = new YtbSimpleAugmentOpParam
                .YtbSimpleAugmentBuilder().cont1(cont1).build();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(simpleAugment);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, SIM_AUG, SIM_AUG_NAMESPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Gets the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, SIMPLE_AUG), module.getName(),
                   is(SIMPLE_AUG));

        // Gets the cont1 under module.
        YdtContext container1 = module.getFirstChild();
        assertThat(getInCrtName(CONTAINER, CONT1), container1.getName(),
                   is(CONT1));

        // Gets the cont2 under cont1.
        YdtContext container2 = container1.getFirstChild();
        assertThat(getInCrtName(CONTAINER, CONT2), container2.getName(),
                   is(CONT2));

        // Gets the leaf4 which was augmented under cont2.
        YdtContext leaf4 = container2.getFirstChild();
        assertThat(getInCrtName(LEAF, LEAF4), leaf4.getName(), is(LEAF4));
        assertThat(getInCrtLeafValue(LEAF4, FIVE_HUNDRED), leaf4.getValue(),
                   is(FIVE_HUNDRED));

        // Gets the cont1s which was augmented under cont2.
        YdtContext container1s = leaf4.getNextSibling();
        assertThat(getInCrtName(CONTAINER, CONT1S), container1s.getName(),
                   is(CONT1S));

        // Gets the cont2s which was augmented under cont1s.
        YdtContext container2s = container1s.getFirstChild();
        assertThat(getInCrtName(CONTAINER, CONT1S), container2s.getName(),
                   is(CONT1S));
    }

    /**
     * Processes inter file augment with augmented node as list and the
     * augment having list.
     */
    @Test
    public void processInterFileAugment() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // As an application, creates the object.
        Object opParam = createObjectForInterFileAugment();

        // As YSB or YAB protocol, sets the value for YTB.
        List<Object> objectList = new ArrayList<>();
        objectList.add(opParam);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtBuilderForYo(
                objectList, INTER_AUG, INTER_AUG_NAMESPACE,
                EDIT_CONFIG_REQUEST, registry);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Checks the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, IETF_NW), module.getName(),
                   is(IETF_NW));

        // Checks the container networks from module.
        YdtContext nwksCont = module.getFirstChild();
        assertThat(getInCrtName(CONTAINER, NWS), nwksCont.getName(), is(NWS));

        // Checks the list network from container networks.
        YdtContext nwrkList = nwksCont.getFirstChild();
        assertThat(getInCrtName(LIST, NW), nwrkList.getName(), is(NW));

        // Checks the node list content 1 under network list.
        YdtContext node1 = nwrkList.getFirstChild();
        assertThat(getInCrtName(LIST, NODE), node1.getName(), is(NODE));

        // Checks the node-id leaf for list content 1.
        YdtContext nodeId1 = node1.getFirstChild();
        assertThat(getInCrtName(LEAF, NODE_ID), nodeId1.getName(), is(NODE_ID));
        assertThat(getInCrtLeafValue(NODE_ID, NODE_REF_3), nodeId1.getValue(),
                   is(NODE_REF_3));

        // Checks termination list 1 under node 1, from augment.
        YdtContext terList1 = nodeId1.getNextSibling();
        assertThat(getInCrtName(LIST, TERM_POINT), terList1.getName(),
                   is(TERM_POINT));

        // Checks tp-id leaf from termination list content 1.
        YdtContext tpId1 = terList1.getFirstChild();
        assertThat(getInCrtName(LEAF, TP_ID), tpId1.getName(), is(TP_ID));
        assertThat(getInCrtLeafValue(TP_ID, AUG_TP_ID_1), tpId1.getValue(),
                   is(AUG_TP_ID_1));

        // Checks supporting term point list content1 from term list content 1.
        YdtContext supTerm1 = tpId1.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_TERM_POINT), supTerm1.getName(),
                   is(SUP_TERM_POINT));

        YdtContext nwkRefSupTerm1 = supTerm1.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupTerm1.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, AUG_NW_REF_1),
                   nwkRefSupTerm1.getValue(), is(AUG_NW_REF_1));

        YdtContext nodeRefSupTerm1 = nwkRefSupTerm1.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupTerm1.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, AUG_NODE_REF_1),
                   nodeRefSupTerm1.getValue(), is(AUG_NODE_REF_1));

        YdtContext tpRefSupTerm1 = nodeRefSupTerm1.getNextSibling();
        assertThat(getInCrtName(LEAF, TP_REF), tpRefSupTerm1.getName(),
                   is(TP_REF));
        assertThat(getInCrtLeafValue(TP_REF, AUG_TP_REF_1),
                   tpRefSupTerm1.getValue(), is(AUG_TP_REF_1));

        // Checks termination list 2 under node 1, from augment.
        YdtContext terminationList2 = terList1.getNextSibling();
        assertThat(getInCrtName(LIST, TERM_POINT), terminationList2.getName(),
                   is(TERM_POINT));

        YdtContext terList2 = terminationList2.getFirstChild();
        assertThat(getInCrtName(LEAF, TP_ID), terList2.getName(), is(TP_ID));
        assertThat(getInCrtLeafValue(TP_ID, AUG_TP_ID_B1), terList2.getValue(),
                   is(AUG_TP_ID_B1));

        // Checks supporting term point list content1 from term list content 2.
        YdtContext supTerm2 = terList2.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_TERM_POINT), supTerm2.getName(),
                   is(SUP_TERM_POINT));

        YdtContext nwkRefSupTerm2 = supTerm2.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupTerm2.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, AUG_NW_REF_B1),
                   nwkRefSupTerm2.getValue(), is(AUG_NW_REF_B1));

        YdtContext nodeRefSupTerm2 = nwkRefSupTerm2.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupTerm2.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, AUG_NODE_REF_B1),
                   nodeRefSupTerm2.getValue(), is(AUG_NODE_REF_B1));

        YdtContext tpRefSupTerm2 = nodeRefSupTerm2.getNextSibling();
        assertThat(getInCrtName(LEAF, TP_REF), tpRefSupTerm2.getName(),
                   is(TP_REF));
        assertThat(getInCrtLeafValue(TP_REF, AUG_TP_REF_B1),
                   tpRefSupTerm2.getValue(), is(AUG_TP_REF_B1));

        // Checks the content of the supporting node list content 1 in node 1.
        YdtContext supNode1 = terminationList2.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_NODE), supNode1.getName(),
                   is(SUP_NODE));

        YdtContext nwkRefSupNode1 = supNode1.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupNode1.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, NW_REF), nwkRefSupNode1.getValue(),
                   is(NW_REF));

        YdtContext nodeRefSupNode1 = nwkRefSupNode1.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupNode1.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, NW_REF),
                   nwkRefSupNode1.getValue(), is(NW_REF));

        // Checks the content of the supporting node list content 2 in node 1.
        YdtContext supNode2 = supNode1.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_NODE), supNode2.getName(),
                   is(SUP_NODE));

        YdtContext nwkRefSupNode2 = supNode2.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupNode2.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, NW_REF_2),
                   nwkRefSupNode2.getValue(), is(NW_REF_2));

        YdtContext nodeRefSupNode2 = nwkRefSupNode2.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupNode2.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, NW_REF_2),
                   nwkRefSupNode2.getValue(), is(NW_REF_2));

        // Checks the node list content 2 under network list.
        YdtContext node2 = node1.getNextSibling();
        assertThat(getInCrtName(LIST, NODE), node2.getName(), is(NODE));

        // Checks the node-id leaf for list content 2.
        YdtContext nodeId2 = node2.getFirstChild();
        assertThat(getInCrtName(LEAF, NODE_ID), nodeId2.getName(), is(NODE_ID));
        assertThat(getInCrtLeafValue(NODE_ID, NODE_REF_3B), nodeId2.getValue(),
                   is(NODE_REF_3B));

        // Checks supporting term point list content1 from term list content 2.
        YdtContext supNode3 = nodeId2.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_NODE), supNode3.getName(),
                   is(SUP_NODE));

        YdtContext nwkRefSupNode3 = supNode3.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupNode3.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, NW_REF_B),
                   nwkRefSupNode3.getValue(), is(NW_REF_B));

        YdtContext nodeRefSupNode3 = nwkRefSupNode3.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupNode3.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, NODE_REF_B),
                   nodeRefSupNode3.getValue(), is(NODE_REF_B));

        // Checks supporting term point list content2 from term list content 2.
        YdtContext supNode4 = supNode3.getNextSibling();
        assertThat(getInCrtName(LIST, SUP_NODE), supNode4.getName(),
                   is(SUP_NODE));

        YdtContext nwkRefSupNode4 = supNode4.getFirstChild();
        assertThat(getInCrtName(LEAF, NW_REF), nwkRefSupNode4.getName(),
                   is(NW_REF));
        assertThat(getInCrtLeafValue(NW_REF, NW_REF_2B),
                   nwkRefSupNode4.getValue(), is(NW_REF_2B));

        YdtContext nodeRefSupNode4 = nwkRefSupNode4.getNextSibling();
        assertThat(getInCrtName(LEAF, NODE_REF), nodeRefSupNode4.getName(),
                   is(NODE_REF));
        assertThat(getInCrtLeafValue(NODE_REF, NODE_REF_2B),
                   nodeRefSupNode4.getValue(), is(NODE_REF_2B));
    }

    /**
     * Processes inter file augment with rpc output as its target node.
     */
    @Test
    public void processInterFileAugmentWithRpcInputAsTarget() {
        schemaProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry registry = schemaProvider
                .getDefaultYangSchemaRegistry();

        // Builds RPC request tree in YDT.
        YangRequestWorkBench workBench =
                buildYangRequestWorkBenchForRpc(registry);

        // Creates augment code object.

        // Creates the list of value in, case value in.
        ValueIn valuein1 = new org.onosproject.yang.gen.v1.yms.test.ytb.augment
                .yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput
                .activatesoftwareimage.output.augmentedrpcoutput.selection
                .valuein.DefaultValueIn.ValueInBuilder().kinetic(KIN1)
                .build();
        ValueIn valuein2 = new org.onosproject.yang.gen.v1.yms.test.ytb.augment
                .yangautoprefixfor.rpc.input.rev20160826.ytbaugmentforrpcinput
                .activatesoftwareimage.output.augmentedrpcoutput.selection
                .valuein.DefaultValueIn.ValueInBuilder().kinetic(KIN2)
                .build();

        List<ValueIn> valueInList = new ArrayList<>();
        valueInList.add(valuein1);
        valueInList.add(valuein2);

        // Adds the case value into the choice interface.
        Selection selection = new DefaultValueIn.ValueInBuilder()
                .valueIn(valueInList).build();

        // Augment is created for the object.
        AugmentedRpcOutput augmentRpcOutput = new DefaultAugmentedRpcOutput
                .AugmentedRpcOutputBuilder().selection(selection).build();

        // Create two list object of friction.
        Friction friction1 = new DefaultFriction.FrictionBuilder()
                .speed(BigInteger.valueOf(500)).build();
        Friction friction2 = new DefaultFriction.FrictionBuilder()
                .speed(BigInteger.valueOf(1000)).build();

        List<Friction> fricList = new ArrayList<>();
        fricList.add(friction1);
        fricList.add(friction2);

        // Create augment with the friction object created.
        AugmentedInputOutput augmentedIO = new DefaultAugmentedInputOutput
                .AugmentedInputOutputBuilder().friction(fricList).build();

        // Creates RPC object.
        List<OutputList> outputLists = createApplicationBuiltObjectForRpc();

        // Adds the augment and the rps output values into the output.
        DefaultActivateSoftwareImageOutput
                .ActivateSoftwareImageOutputBuilder output =
                new DefaultActivateSoftwareImageOutput
                        .ActivateSoftwareImageOutputBuilder();
        output.addYangAugmentedInfo(augmentRpcOutput, AugmentedRpcOutput.class);
        output.addYangAugmentedInfo(augmentedIO, AugmentedInputOutput.class);
        output.outputList(outputLists);

        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YdtExtendedBuilder ydtBuilder = treeBuilder.getYdtForRpcResponse(
                output, workBench);

        // Receives YDT context and check the tree that is built.
        YdtContext context = ydtBuilder.getRootNode();

        // Checks the first module from logical root node.
        YdtContext module = context.getFirstChild();
        assertThat(getInCrtName(MODULE, RPC_ADV_IO), module.getName(),
                   is(RPC_ADV_IO));

        // Gets the rpc under module.
        YdtContext rpc = module.getFirstChild();
        assertThat(getInCrtName(RPC, ACT_IMG), rpc.getName(), is(ACT_IMG));

        // Gets the output value under rpc.
        // TODO: Change assert after YANG utils is merged.
        YdtContext rpcOutputNode = rpc.getFirstChild();
        //assertThat(rpcOutputNode.getName(), is("output"));

        YdtContext firstNode = rpcOutputNode.getFirstChild();
        assertThat(firstNode, notNullValue());

        YdtContext secondNode = firstNode.getNextSibling();
        assertThat(secondNode, notNullValue());

        YdtContext thirdNode = secondNode.getNextSibling();
        assertThat(thirdNode, notNullValue());

        YdtContext fourthNode = thirdNode.getNextSibling();
        assertThat(fourthNode, notNullValue());

        // Gets the list content 1 as the node from output.
        YdtContext outputList1 = fourthNode.getNextSibling();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList1.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf key-list from list content1.
        YdtContext keyList1 = outputList1.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList1.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_1), keyList1.getValue(),
                   is(BIN_VAL_1));

        // Gets the content inside container from list content 1.
        YdtContext cont1 = keyList1.getNextSibling();
        assertThat(getInCrtName(CONTAINER, CONT_INSIDE), cont1.getName(),
                   is(CONT_INSIDE));

        // Gets the list content 2 as the node from output.
        YdtContext outputList2 = outputList1.getNextSibling();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList2.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf-list key-list from list content2.
        YdtContext keyList2 = outputList2.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList2.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_2), keyList2.getValue(),
                   is(BIN_VAL_2));

        // Gets the content inside container from list content 2.
        YdtContext cont2 = keyList2.getNextSibling();
        assertThat(getInCrtName(CONTAINER, CONT_INSIDE), cont2.getName(),
                   is(CONT_INSIDE));

        // Gets the leaf-list available inside container.
        YdtContext availLeafList = cont2.getFirstChild();
        assertThat(getInCrtName(LEAF_LIST, AVAILABLE), availLeafList.getName(),
                   is(AVAILABLE));
        Set value1 = availLeafList.getValueSet();
        assertThat(getInCrtLeafListValue(AVAILABLE, EIGHTY_NINE),
                   value1.contains(EIGHTY_NINE), is(true));
        assertThat(getInCrtLeafListValue(AVAILABLE, NINETY_EIGHT),
                   value1.contains(NINETY_EIGHT), is(true));

        // Gets the list content 3.
        YdtContext outputList3 = outputList2.getNextSibling();
        assertThat(getInCrtName(LIST, OUTPUT_LIST), outputList3.getName(),
                   is(OUTPUT_LIST));

        // Gets the leaf list-key in content 3 of list.
        YdtContext keyList3 = outputList3.getFirstChild();
        assertThat(getInCrtName(LEAF, LIST_KEY), keyList3.getName(),
                   is(LIST_KEY));
        assertThat(getInCrtLeafValue(LIST_KEY, BIN_VAL_3), keyList3.getValue(),
                   is(BIN_VAL_3));
    }
}
