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
package org.onosproject.yms.app.ydt;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.onosproject.yms.app.yob.DefaultYobBuilder;
import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.app.ytb.DefaultYangTreeBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtListener;
import org.onosproject.yms.ydt.YdtType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A1;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A2;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A2L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A3;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A4;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A5;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A5L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A6;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A6L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.BACKSLASH;
import static org.onosproject.yms.app.ydt.YdtTestConstants.BIT;
import static org.onosproject.yms.app.ydt.YdtTestConstants.BITNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.BOOL;
import static org.onosproject.yms.app.ydt.YdtTestConstants.BOOLNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSINT16;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSINT64;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSUINT16;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSUINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSUINT64;
import static org.onosproject.yms.app.ydt.YdtTestConstants.CAPSUINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.COUSTOMNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.ELNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.EMPNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.EMPTY;
import static org.onosproject.yms.app.ydt.YdtTestConstants.EMPTYNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.ENUM;
import static org.onosproject.yms.app.ydt.YdtTestConstants.ENUMNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.E_LEAF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.IETF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.IETFNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INT16NS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INT32NS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INT64NS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INT8NS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.LIST;
import static org.onosproject.yms.app.ydt.YdtTestConstants.LISTNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.LWC;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MATERIALNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT16;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT64;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MERCHNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINVALUE;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MRV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NETNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NIWMF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NWF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PERIOD;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PIWMF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PURCHASNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PWF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SINT16;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SLINK;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SMALLINT64;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SMALLINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SMALLUINT64;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SMALLUINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.STP;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SUINT16;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SUINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TOPONS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TRADNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestConstants.WAREHNS;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REPLY;

public class YdtTestUtils implements YdtListener {

    private static List<String> kValList = new ArrayList<>();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static YangSchemaRegistry schemaRegistry;

    private static TestYangSchemaNodeProvider schemaProvider =
            new TestYangSchemaNodeProvider();

    // Logger list is used for walker testing.
    private static final List<String> LOGGER = new ArrayList<>();

    /**
     * Returns the LOGGER with log for testing the YDT walker.
     *
     * @return list of logs
     */
    public static List<String> getLogger() {
        return LOGGER;
    }

    /**
     * Clear the LOGGER array.
     */
    public static void resetLogger() {
        LOGGER.clear();
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
        LOGGER.add("Entry Node is " + ydtContext.getName() + PERIOD);
    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {
        LOGGER.add("Exit Node is " + ydtContext.getName() + PERIOD);
    }

    /**
     * Returns schema registry of YDT.
     *
     * @return schema registry
     */
    public static YangSchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

    /**
     * Sets the ydt schema registry.
     *
     * @param registry schema registry
     */
    public static void setSchemaRegistry(YangSchemaRegistry registry) {
        schemaRegistry = registry;
    }

    /**
     * Returns the ydt builder for food module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench foodArenaYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("foodarena", "food", "ydt.food", MERGE);
        ydtBuilder.addChild("food", "ydt.food");
//        ydtBuilder.addChild("snack", null, "ydt.food");
//        ydtBuilder.addChild("latenight", null, "ydt.food");
        ydtBuilder.addLeaf("chocolate", "ydt.food", "dark");

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for empty leaf list module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench emptyLeafListYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("empty", "EmptyLeafList", ELNS, MERGE);
        ydtBuilder.addChild("l1", ELNS);
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("l2", ELNS);
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("l3", ELNS);
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("list1", ELNS);
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("list2", ELNS);
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("list3", ELNS);
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for yms-ietf-network module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench ietfNetwork1Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(IETF, IETF, IETFNS, MERGE);
        // Adding container
        ydtBuilder.addChild("networks", null);
        // Adding list inside container
        ydtBuilder.addChild("network", null);
        // Adding key element network Id
        ydtBuilder.addLeaf("network-id", null, "network1");
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("node", null);
        // Adding key element node-id
        ydtBuilder.addLeaf("node-id", null, "node1");
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-node", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network3");
        ydtBuilder.traverseToParent();

        // Adding key element node-ref
        ydtBuilder.addLeaf("node-ref", null, "network4");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding container
        ydtBuilder.addChild("networks-state", null);
        // Adding list inside container
        ydtBuilder.addChild("network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network5");
        ydtBuilder.traverseToParent();
        // Adding leaf server-provided
        ydtBuilder.addLeaf("server-provided", null, "true");

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for yms-ietf-network-topology module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench ietfNetworkTopologyYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(IETF, IETF, IETFNS, MERGE);
        // Adding container
        ydtBuilder.addChild("networks", IETFNS, MERGE);
        // Adding list inside container
        ydtBuilder.addChild("network", IETFNS, MERGE);

        // Adding key element network Id
        ydtBuilder.addLeaf("network-id", null, "network1");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("id1");
        // adding the augmented node
        ydtBuilder.addMultiInstanceChild("link", TOPONS, kValList, MERGE);
        // container source
        ydtBuilder.addChild("source", TOPONS, MERGE);
        ydtBuilder.addLeaf("source-node", null, "source1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("source-tp", null, "source2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // container destination
        ydtBuilder.addChild("destination", TOPONS, MERGE);
        ydtBuilder.addLeaf("dest-node", null, "dest1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("dest-tp", null, "dest2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("network1");
        kValList.add("id2");
        // adding the supporting-link list node
        ydtBuilder.addMultiInstanceChild(SLINK, TOPONS, kValList, MERGE);
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("network2");
        kValList.add("id3");
        // adding the supporting-link list another instance
        ydtBuilder.addMultiInstanceChild(SLINK, TOPONS, kValList, MERGE);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("node", null);
        // Adding key element node-id
        ydtBuilder.addLeaf("node-id", null, "node1");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("tp_id1");
        //adding augmented termination-point list
        ydtBuilder.addMultiInstanceChild("t-point", TOPONS,
                                         kValList, MERGE);
        kValList.clear();
        kValList.add("network-ref");
        kValList.add("node-ref");
        kValList.add("tp-ref");
        //adding supporting-termination-point
        ydtBuilder.addMultiInstanceChild(STP, TOPONS, kValList, MERGE);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-node", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network3");
        ydtBuilder.traverseToParent();

        // Adding key element node-ref
        ydtBuilder.addLeaf("node-ref", null, "network4");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("link-id", TOPONS, "id1");
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding container
        ydtBuilder.addChild("networks-state", null);
        // Adding list inside container
        ydtBuilder.addChild("network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network5");
        ydtBuilder.traverseToParent();
        // Adding leaf server-provided
        ydtBuilder.addLeaf("server-provided", null, "true");

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for augmented module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench augmentNetworkYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(IETF, IETF, IETFNS, MERGE);
        // Adding container
        ydtBuilder.addChild("networks", IETFNS, MERGE);
        // Adding list inside container
        ydtBuilder.addChild("network", IETFNS, MERGE);

        // Adding key element network Id
        ydtBuilder.addLeaf("network-id", null, "network1");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("id1");
        // adding the augmented node
        ydtBuilder.addMultiInstanceChild("link", TOPONS, kValList, MERGE);
        // container source
        ydtBuilder.addChild("source", TOPONS, MERGE);
        ydtBuilder.addLeaf("source-node", null, "source1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("source-tp", null, "source2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // container destination
        ydtBuilder.addChild("destination", TOPONS, MERGE);
        ydtBuilder.addLeaf("dest-node", null, "dest1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("dest-tp", null, "dest2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("network1");
        kValList.add("id2");
        // adding the supporting-link list node
        ydtBuilder.addMultiInstanceChild(SLINK, TOPONS, kValList, MERGE);
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("network2");
        kValList.add("id3");
        // adding the supporting-link list another instance
        ydtBuilder.addMultiInstanceChild(SLINK, TOPONS, kValList, MERGE);
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("1");
        ydtBuilder.addMultiInstanceChild("augment1", A1, kValList, MERGE);
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("1");
        kValList.add("2");
        ydtBuilder.addMultiInstanceChild("augment2", A2, kValList, MERGE);

        ydtBuilder.addChild("augment5", A5, DELETE);

        ydtBuilder.addMultiInstanceChild(A6L, A6, kValList, DELETE);
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("value5", null, "5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addMultiInstanceChild(A5L, A5, kValList, DELETE);
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("augment3", A3, MERGE);

        ydtBuilder.addChild("augment4", A4, DELETE);
        ydtBuilder.addLeaf("value4", null, "4");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("augment5", A5, MERGE);

        ydtBuilder.addLeaf("leaf6", A6, "6");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("value5", null, "5");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("augment6", A6, DELETE);
        ydtBuilder.addLeaf("value6", null, "6");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("value3", null, "3");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("augment3leaf", A3, "3");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addMultiInstanceChild(A2L, A2, kValList, MERGE);
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("node", null);
        // Adding key element node-id
        ydtBuilder.addLeaf("node-id", null, "node1");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("tp_id1");
        //adding augmented termination-point list
        ydtBuilder.addMultiInstanceChild("t-point", TOPONS,
                                         kValList, MERGE);
        kValList.clear();
        kValList.add("network-ref");
        kValList.add("node-ref");
        kValList.add("tp-ref");
        //adding supporting-termination-point
        ydtBuilder.addMultiInstanceChild(STP, TOPONS, kValList, MERGE);

        // Adding augmented container1 inside supporting-termination-point
        augmentTerminationPointYdt(ydtBuilder);

        return ydtBuilder;
    }

    /**
     * Adds augments inside supporting-termination-point in augmented module.
     *
     * @param ydtBuilder ydt builder which need to be updated
     */
    private static void augmentTerminationPointYdt(YangRequestWorkBench ydtBuilder) {

        ydtBuilder.addChild("augment1", A1);
        ydtBuilder.addLeaf("value1", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("augment1-leaf", A1, "1");

        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("augment2", A2, MERGE);

        ydtBuilder.addChild("augment3", A3, MERGE);
        ydtBuilder.addLeaf("value3", null, "3");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("augment4leaf", A4, "4");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("value2", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("1");
        kValList.add("2");
        ydtBuilder.addMultiInstanceChild(A2L, A2, kValList, MERGE);
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("augment2leaf", A2, "2");
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding list inside list
        ydtBuilder.addChild("supporting-node", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network3");
        ydtBuilder.traverseToParent();

        // Adding key element node-ref
        ydtBuilder.addLeaf("node-ref", null, "network4");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("link-id", TOPONS, "id1");
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        // Adding container
        ydtBuilder.addChild("networks-state", null);
        // Adding list inside container
        ydtBuilder.addChild("network", null);
        // Adding key element network-ref
        ydtBuilder.addLeaf("network-ref", null, "network5");
        ydtBuilder.traverseToParent();
        // Adding leaf server-provided
        ydtBuilder.addLeaf("server-provided", null, "true");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        augmentNetworkYdt(ydtBuilder);
    }

    /**
     * Adds augmented module augment-network under logical node ietf-network.
     *
     * @param ydtBuilder ydt builder which need to be updated
     */
    private static void augmentNetworkYdt(YangRequestWorkBench ydtBuilder) {
        ydtBuilder.addChild("augmentNetwork", NETNS);

        //adding list with name node under module node
        ydtBuilder.addChild("node", null);

        //adding key leaf for node
        ydtBuilder.addLeaf("name", null, "node1");
        ydtBuilder.traverseToParent();

        // adding augmented container cont1s under list
        ydtBuilder.addChild("cont1s", null);
        // adding container cont1s under cont1s
        ydtBuilder.addChild("cont1s", null);
        //adding leaf under cont1s
        ydtBuilder.addLeaf("fine", null, "leaf");

        //adding augmented list node bu augment-topology1 under container
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("augment1", A1, DELETE);
        //adding key leaf for list node augment1
        ydtBuilder.addLeaf("value1", null, "1");
    }

    /**
     * Returns the ydt builder for rootlist module with listwithcontainer node
     * using addMultiInstanceChild interface for adding multi instance node.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench listWithContainerYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("list", "rootlist", "ydt.rootlist", MERGE);
        kValList.clear();
        kValList.add("12");
        kValList.add("12");
        ydtBuilder.addMultiInstanceChild(LWC, null, kValList, MERGE);
        ydtBuilder.addLeaf(INV, null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("interface", null);
        ydtBuilder.addLeaf(INV, null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid", null, "121");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for rootlist module with listwithcontainer
     * node using addChild interface for adding multi instance node.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench listWithContainer1Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("list", "rootlist", "ydt.rootlist", MERGE);
        ydtBuilder.addChild(LWC, null);
        ydtBuilder.addLeaf("invalid", null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("interface", null);
        ydtBuilder.addLeaf(INV, null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid", null, "121");
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for rootlist module with multiple instances of
     * listwithcontainer node using addMultiInstanceChild interface for adding
     * multi instance node.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench listWithContainer2Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("list", "rootlist", "ydt.rootlist", MERGE);
        kValList.clear();
        kValList.add("1222");
        kValList.add("1212");
        ydtBuilder.addMultiInstanceChild(LWC, null, kValList, MERGE);

        kValList.clear();
        kValList.add("12");
        kValList.add("1");
        ydtBuilder.addMultiInstanceChild(INV, null, kValList, MERGE);
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, null, "122");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, null, "2222");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("1222");
        kValList.add("1212");
        ydtBuilder.addMultiInstanceChild(INV, null, kValList, MERGE);
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("interface", null);
        ydtBuilder.addLeaf(INV, null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for rootlist module with listwithoutcontainer
     * node.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench listWithoutContainerYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("list", "rootlist", "ydt.rootlist", MERGE);
        ydtBuilder.addChild("listwithoutcontainer", null);
        ydtBuilder.addLeaf(INV, null, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for logisticsmanager module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench logisticsManagerYdt() {

        Set<String> valueSet = new HashSet();
        valueSet.add("1");
        valueSet.add("2");
        valueSet.add("3");
        valueSet.add("4");
        valueSet.add("5");

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("logisticsmanager", "customssupervisor",
                                   null, MERGE);
        ydtBuilder.addLeaf("supervisor", COUSTOMNS, "abc");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("merchandisersupervisor", MERCHNS, MERGE);
        ydtBuilder.addLeaf("supervisor", MERCHNS, "abc");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("materialsupervisor", MATERIALNS, MERGE);
        ydtBuilder.addChild("supervisor", MATERIALNS);
        ydtBuilder.addLeaf("name", MATERIALNS, "abc");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("departmentId", MATERIALNS, "xyz");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("supervisor", MATERIALNS);
        ydtBuilder.addLeaf("name", MATERIALNS, "ab");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("departmentId", MATERIALNS, "xy");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("purchasingsupervisor", PURCHASNS, MERGE);
        ydtBuilder.addChild("supervisor", PURCHASNS);
        ydtBuilder.addLeaf("purchasing-specialist", PURCHASNS, "abc");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("support", "ydt.purchasing-supervisor", "xyz");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("warehousesupervisor", WAREHNS, MERGE);
        ydtBuilder.addLeaf("supervisor", WAREHNS, valueSet);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("tradingsupervisor", TRADNS, MERGE);
        ydtBuilder.addLeaf("supervisor", TRADNS, "abc");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("employeeid", EMPNS, MERGE);
        ydtBuilder.addLeaf("employeeid", EMPNS, valueSet);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for bit module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench bitYdt() {
        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("builtInType", "bit", "ydt.bit", MERGE);
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
        ydtBuilder.addChild("bitList", null);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for bool module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench booleanYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("builtInType", "bool", "ydt.boolean", MERGE);
        ydtBuilder.addChild("booleanList", null);
        ydtBuilder.addLeaf("boolean", null, "true");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("booleanList", null);
        ydtBuilder.addLeaf("boolean", null, "false");
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for emptydata module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench emptyTypeYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "emptydata", "ydt.emptydata", MERGE);
        ydtBuilder.addChild("emptyList", null);
        ydtBuilder.addLeaf("empty", null, "");

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for enumtest module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench enumYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "enumtest", "ydt.enumtest", MERGE);
        ydtBuilder.addChild("enumList", null);
        ydtBuilder.addLeaf("enumleaf", null, "ten");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("enumList", null);
        ydtBuilder.addLeaf("enumleaf", null, "hundred");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild("enumList", null);
        ydtBuilder.addLeaf("enumleaf", null, "thousand");

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for augmentSequence module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench augmentSequenceYdt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "augment", "augmentSequence", "ydt.augmentSequence", MERGE);
        ydtBuilder.addChild("l1", null);
        ydtBuilder.addLeaf("leaf1", null, "1");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("c1", "ydt.augmentSequence1");
        ydtBuilder.addLeaf("leaf2", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild("c2", "ydt.augmentSequence2");
        ydtBuilder.addLeaf("leaf2", null, "3");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for crypto-base module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench identityRefYdt() {

        Set<String> valueSet = new HashSet();
        valueSet.add("crypto-alg");
        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "identityref", "crypto-base", "ydt.crypto-base", MERGE);
        ydtBuilder.addLeaf("crypto", null, "crypto-alg");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("abc-zeunion", null, "crypto-alg");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("level2", null, "crypto-alg2");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("level3", null, "crypto-alg3");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("level4", null, "crypto-alg3");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("abc-type", null, valueSet);
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for builtin type integer8 module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench integer8Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "integer8", "ydt.integer8", MERGE);
        ydtBuilder.addLeaf("negInt", null, "-128");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("posInt", null, "127");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("minUInt", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUInt", null, "255");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midUIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minUIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("UnInteger", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revInteger", null, "-128");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "127");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "255");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for builtin type integer16 module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench integer16Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "integer16", "ydt.integer16", MERGE);
        ydtBuilder.addLeaf("negInt", null, "-32768");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("posInt", null, "32767");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("minUInt", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUInt", null, "65535");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midUIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minUIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("UnInteger", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revInteger", null, "-32768");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "32767");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "65535");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for builtin type integer32 module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench integer32Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "integer32", "ydt.integer32", MERGE);
        ydtBuilder.addLeaf("negInt", null, "-2147483648");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("posInt", null, "2147483647");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("minUInt", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUInt", null, "4294967295");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midUIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minUIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("UnInteger", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revInteger", null, "-2147483648");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "2147483647");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "4294967295");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for builtin type integer64 module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench integer64Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "integer64", "ydt.integer64", MERGE);
        ydtBuilder.addLeaf("negInt", null, "-9223372036854775808");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("posInt", null, "9223372036854775807");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("minUInt", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUInt", null, "18446744073709551615");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addLeaf("midUIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minUIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxUIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("integer", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("UnInteger", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("UnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revInteger", null, "-9223372036854775808");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revInteger", null, "9223372036854775807");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "0");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "1");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "2");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null, YdtType.MULTI_INSTANCE_NODE);
        ydtBuilder.addLeaf("revUnInteger", null, "18446744073709551615");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder for builtin type decimal64 module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench decimal64Ydt() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "builtInType", "decimal64", "ydt.decimal64", MERGE);
        ydtBuilder.addLeaf("negInt", null, "-92233720368547758.08");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("posInt", null, "92233720368547758.07");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(NIWMF, null, "-922337203685477580.8");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(PIWMF, null, "922337203685477580.7");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(NWF, null, "-9.223372036854775808");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(PWF, null, "9.223372036854775807");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("midIntWithRange", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("minIntWithRange", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("maxIntWithRange", null, "100");
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "11");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "40");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "50");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "55");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("decimal", null, "100");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "-92233720368547758.08");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "2.505");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "3.14");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "10");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "20");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "92233720368547757");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.addChild(MRV, null);
        ydtBuilder.addLeaf("revDecimal", null, "92233720368547758.07");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the ydt builder with requested logical root name and module name.
     *
     * @param rootName   logical rootNode name
     * @param moduleName application(module) name
     * @param nameSpace  namespace of module
     * @param opType     operation type
     * @return ydt builder
     */
    public static YangRequestWorkBench getYdtBuilder(String rootName, String
            moduleName, String nameSpace, YdtContextOperationType opType) {
        setSchemaRegistry(schemaProvider.getDefaultYangSchemaRegistry());
        YangRequestWorkBench ydtBuilder;
        schemaProvider.processSchemaRegistry(null);
        ydtBuilder = new YangRequestWorkBench(
                rootName, null, null, schemaProvider
                .getDefaultYangSchemaRegistry(), true);
        ydtBuilder.addChild(moduleName, nameSpace, opType);
        return ydtBuilder;
    }

    /**
     * Compares the two value sets.
     */
    public static void compareValueSet(Set<String> valueSet,
                                       Set<String> userInputValueSet) {
        // Check the value against user input.
        assertTrue("Expected 'valueSet' and 'userInputValueSet' to be equal.",
                   valueSet.containsAll(userInputValueSet));
    }

    /**
     * Returns the ydt builder for Hello_ONOS module.
     *
     * @return ydt builder
     */
    public static YangRequestWorkBench helloOnos() {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder(
                "Hello-ONOS", "Hello_ONOS", "ydt:hello_onos", MERGE);
        ydtBuilder.addChild("hello-world", null);
        ydtBuilder.addChild("input", null);
        ydtBuilder.addLeaf("name", null, "onos");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("surName", null, "yang");
        ydtBuilder.traverseToParent();

        kValList.clear();
        kValList.add("ON");
        kValList.add("LAB");
        ydtBuilder.addMultiInstanceChild("stringList", null, kValList,
                                         MERGE);
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        return ydtBuilder;
    }

    /**
     * Returns the error message for requested node.
     *
     * @param value    value in string format
     * @param dataType requested data type
     * @return error string
     */
    static String getErrorString(String value, String dataType) {
        StringBuilder msg = new StringBuilder();
        switch (dataType) {
            case SINT16:
            case SINT32:
            case SMALLINT8:
            case SMALLINT64:
            case SMALLUINT8:
            case SMALLUINT64:
            case SUINT16:
            case SUINT32:
            case CAPSINT8:
            case CAPSINT16:
            case CAPSINT32:
            case CAPSINT64:
            case CAPSUINT8:
            case CAPSUINT16:
            case CAPSUINT32:
            case CAPSUINT64:
            case BIT:
            case BOOL:
            case ENUM:
                msg.append("YANG file error : Input value ").append(BACKSLASH)
                        .append(value).append(BACKSLASH)
                        .append(" is not a valid ").append(dataType);
                break;
            case EMPTY:
                msg.append("YANG file error : Input value ").append(BACKSLASH)
                        .append(value).append(BACKSLASH).append(
                        " is not allowed for a data type EMPTY");
                break;
            case MINVALUE:
                msg.append("YANG file error : ").append(value)
                        .append(" is lesser than minimum value ")
                        .append(MINVALUE).append(PERIOD);
                break;
            case MAXUINT8:
            case MAXUINT16:
            case MAXUINT32:
            case MAXUINT64:
                msg.append("YANG file error : ").append(value)
                        .append(" is greater than maximum value ")
                        .append(dataType).append(PERIOD);
                break;
            default:
                return null;
        }
        return msg.toString();
    }

    /**
     * Validates the error message which is obtained by checking the given
     * value against its data type restrictions.
     *
     * @param name      leaf name
     * @param nameSpace leaf namespace
     * @param val       leaf value
     * @param type      data type suffix string for exception message
     * @param childName child name
     */
    public static void validateErrMsg(String name, String nameSpace,
                                      String val, String type, String childName) {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(nameSpace);
        boolean isExpOccurred = false;
        /*
         * If childName exist then leaf need to be added under the
         * child node with the given childName
         */
        if (childName != null) {
            ydtBuilder.addChild(childName, nameSpace);
        }
        /*
         * This try catch is explicitly written to use as utility in other
         * test cases.
         */
        try {
            ydtBuilder.addLeaf(name, nameSpace, val);
        } catch (IllegalArgumentException e) {
            isExpOccurred = true;
            assertEquals(e.getMessage(), getErrorString(val, type));
        }
        assertEquals(E_LEAF + name, isExpOccurred, true);
    }

    /**
     * Returns ydt builder for requested namespace.
     *
     * @param namespace namespace of the requested yang data tree
     * @return ydt builder
     */
    public static YangRequestWorkBench getTestYdtBuilder(String namespace) {

        switch (namespace) {
            case INT8NS:
                return getYdtBuilder(TYPE, "integer8", INT8NS, MERGE);
            case INT16NS:
                return getYdtBuilder(TYPE, "integer16", INT16NS, MERGE);
            case INT32NS:
                return getYdtBuilder(TYPE, "integer32", INT32NS, MERGE);
            case INT64NS:
                return getYdtBuilder(TYPE, "integer64", INT64NS, MERGE);
            case BITNS:
                return getYdtBuilder(TYPE, "bit", BITNS, MERGE);
            case BOOLNS:
                return getYdtBuilder(TYPE, "bool", BOOLNS, MERGE);
            case EMPTYNS:
                return getYdtBuilder(TYPE, "emptydata", EMPTYNS, MERGE);
            case ENUMNS:
                return getYdtBuilder(TYPE, "enumtest", ENUMNS, MERGE);
            case LISTNS:
                return getYdtBuilder(LIST, "rootlist", LISTNS, MERGE);
            default:
                return null;
        }
    }

    /**
     * Validates the contents of node like name, namespace and operation type.
     *
     * @param ydtNode node need to be validate
     * @param name    name of the node
     * @param opType  operation type of the node
     */
    public static void validateNodeContents(YdtNode ydtNode, String name,
                                            YdtContextOperationType opType) {
        assertEquals(ydtNode.getName(), name);
        assertEquals(ydtNode.getYdtContextOperationType(), opType);
    }

    /**
     * Validates the contents of leaf node like name, namespace and operation
     * type.
     *
     * @param ydtNode node need to be validate
     * @param name    name of the node
     * @param value   value of the leaf node
     */
    public static void validateLeafContents(YdtNode ydtNode, String name,
                                            String value) {
        validateNodeContents(ydtNode, name, null);
        assertEquals(ydtNode.getValue(), value);
    }

    /**
     * Validates the contents of leaf-list node like name, namespace and
     * operation type.
     *
     * @param ydtNode  node need to be validate
     * @param name     name of the node
     * @param valueSet value of the leaf node
     */
    public static void validateLeafListContents(YdtNode ydtNode, String name,
                                                Set<String> valueSet) {
        validateNodeContents(ydtNode, name, null);
        compareValueSet(ydtNode.getValueSet(), valueSet);
    }

    /**
     * Validates the contents of ydt application logical node.
     *
     * @param ydtAppNode node need to be validate
     */
    public static void validateAppLogicalNodeContents(
            YdtAppContext ydtAppNode) {

        assertNull(ydtAppNode.getOperationType());
        assertNull(ydtAppNode.getParent());
        assertNull(ydtAppNode.getNextSibling());
        assertNull(ydtAppNode.getPreviousSibling());
        assertNotNull(ydtAppNode.getFirstChild());
        assertNotNull(ydtAppNode.getLastChild());
    }

    /**
     * Validates the contents of ydt application module node.
     *
     * @param ydtAppNode node need to be validate
     * @param name       name of the node
     * @param opType     operation type of the app node
     */
    public static void validateAppModuleNodeContents(
            YdtAppContext ydtAppNode, String name,
            YdtAppNodeOperationType opType) {

        assertEquals(ydtAppNode.getModuleContext().getName(), name);
        assertEquals(ydtAppNode.getOperationType(), opType);
    }

    /**
     * Validates the contents of ydt application node like name, namespace
     * and operation type.
     *
     * @param ydtAppNode node need to be validate
     * @param name       name of the schema node
     * @param ns         namespace of the schema node
     * @param opType     operation type of the app node
     */
    public static void validateAppNodeContents(
            YdtAppContext ydtAppNode, String name, String ns,
            YdtAppNodeOperationType opType) {
        assertEquals(ydtAppNode.getAugmentingSchemaNode().getName(), name);
        assertEquals(ydtAppNode.getAugmentingSchemaNode().getNameSpace()
                             .getModuleNamespace(), ns);
        assertEquals(ydtAppNode.getOperationType(), opType);
    }

    /**
     * Walks in the given built ydt and validates it.
     */
    public static void walkINTree(YangRequestWorkBench ydtBuilder,
                                  String[] expected) {
        DefaultYdtWalker ydtWalker = new DefaultYdtWalker();
        resetLogger();

        YdtTestUtils utils = new YdtTestUtils();
        // Assign root node as starting node to walk the whole tree.
        ydtWalker.walk(utils, ydtBuilder.getRootNode());
        // Logger list is used for walker testing.
        List<String> logger = getLogger();

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], logger.get(i));
        }
    }

    /**
     * Creates Ydt from YO using YTB.
     */
    static YangRequestWorkBench validateYangObject(
            YangRequestWorkBench ydtBuilder, String name, String namespace) {

        YdtContext rootCtx = ydtBuilder.getRootNode();

        YdtContext childCtx = rootCtx.getFirstChild();

        DefaultYobBuilder builder = new DefaultYobBuilder();

        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, YdtTestUtils
                        .getSchemaRegistry());

        List<Object> list = new LinkedList<>();
        list.add(yangObject);
        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) treeBuilder.getYdtBuilderForYo(
                        list, name, namespace, EDIT_CONFIG_REPLY, YdtTestUtils
                                .getSchemaRegistry());
        return defaultYdtBuilder;
    }
}
