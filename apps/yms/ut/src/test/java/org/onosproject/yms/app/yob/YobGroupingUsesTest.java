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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.YmsIetfNetworkOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ymsietfnetwork.networksstate.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.YmsNetworkTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ymsnetworktopology.networks.network.AugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ymsnetworktopology.networks.network.DefaultAugmentedNdNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.ymsnetworktopology.networks.network.augmentedndnetwork.Link;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.YmsIetfTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.YmsIetfTeTopologyOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.TeAdminStatus;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.networks.network.link.AugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.networks.network.link.DefaultAugmentedNtLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.teadminstatus.TeAdminStatusEnum;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.telinkconfig.bundlestacklevel.DefaultBundle;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160317.ymsietftetopology.telinkconfig.bundlestacklevel.bundle.bundledlinks.BundledLink;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.onosproject.yms.app.yob.YobTestUtils.ADMIN_STATUS;
import static org.onosproject.yms.app.yob.YobTestUtils.BUNDLED_LINK;
import static org.onosproject.yms.app.yob.YobTestUtils.BUNDLED_LINKS;
import static org.onosproject.yms.app.yob.YobTestUtils.CONFIG;
import static org.onosproject.yms.app.yob.YobTestUtils.IETF_TE_TOPOLOGY;
import static org.onosproject.yms.app.yob.YobTestUtils.LINK;
import static org.onosproject.yms.app.yob.YobTestUtils.NETWORK;
import static org.onosproject.yms.app.yob.YobTestUtils.NETWORKS;
import static org.onosproject.yms.app.yob.YobTestUtils.NETWORKS_STATE;
import static org.onosproject.yms.app.yob.YobTestUtils.NETWORK_REF;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;
import static org.onosproject.yms.app.yob.YobTestUtils.SEQUENCE;
import static org.onosproject.yms.app.yob.YobTestUtils.STR_LEAF_VALUE;
import static org.onosproject.yms.app.yob.YobTestUtils.TE;
import static org.onosproject.yms.app.yob.YobTestUtils.TE_LINK_TEMPLATE;
import static org.onosproject.yms.app.yob.YobTestUtils.TE_NODE_ATTRIBUTES;
import static org.onosproject.yms.app.yob.YobTestUtils.TE_NODE_EVENT;
import static org.onosproject.yms.app.yob.YobTestUtils.UP;
import static org.onosproject.yms.app.yob.YobTestUtils.YMS_IETF_NETWORK;
import static org.onosproject.yms.app.yob.YobTestUtils.YMS_NETWORK_TOPOLOGY;
import static org.onosproject.yms.ydt.YdtContextOperationType.CREATE;

/**
 * Test the YANG object building for the YANG data tree based on the grouping
 * and uses nodes.
 */
public class YobGroupingUsesTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void testGroupingUsesLeaf() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild(YMS_IETF_NETWORK, null, CREATE);
        ydtBuilder.addChild(NETWORKS_STATE, null);
        ydtBuilder.addChild(NETWORK, null);
        ydtBuilder.addLeaf(NETWORK_REF, null, STR_LEAF_VALUE);
        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertThat(yangObject, is(notNullValue()));
        YmsIetfNetworkOpParam ietfNetwork = (YmsIetfNetworkOpParam) yangObject;
        Network network = ietfNetwork.networksState().network().iterator().next();
        assertThat(network.networkRef(), is(STR_LEAF_VALUE));
    }

    @Test
    public void testGroupingUsesContainer() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild(IETF_TE_TOPOLOGY, null, CREATE);
        ydtBuilder.addChild(TE_NODE_EVENT, null);
        ydtBuilder.addChild(TE_NODE_ATTRIBUTES, null);
        ydtBuilder.addLeaf(ADMIN_STATUS, null, UP);
        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertThat(yangObject, is(notNullValue()));
        YmsIetfTeTopologyOpParam ietfTeTopology = (YmsIetfTeTopologyOpParam)
                yangObject;
        TeAdminStatus adminStatus = ietfTeTopology.teNodeEvent()
                .teNodeAttributes()
                .adminStatus();
        assertThat(adminStatus.enumeration(), is(TeAdminStatusEnum.UP));
    }

    @Test
    public void testGroupingUsesInterfile() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild(YMS_IETF_NETWORK, null, CREATE);
        ydtBuilder.addChild(NETWORKS, null);
        ydtBuilder.addChild(NETWORK, null);
        ydtBuilder.addChild(LINK, YMS_NETWORK_TOPOLOGY);
        ydtBuilder.addChild(TE, IETF_TE_TOPOLOGY);
        ydtBuilder.addChild(CONFIG, IETF_TE_TOPOLOGY);
        ydtBuilder.addChild(BUNDLED_LINKS, IETF_TE_TOPOLOGY);
        ydtBuilder.addChild(BUNDLED_LINK, IETF_TE_TOPOLOGY);
        ydtBuilder.addLeaf(SEQUENCE, null, "1");
        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertThat(yangObject, is(notNullValue()));
        YmsIetfNetworkOpParam ietfNetwork = (YmsIetfNetworkOpParam) yangObject;

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.rev20151208.ymsietfnetwork
                .networks.Network network = ietfNetwork.networks().network().get(0);

        DefaultAugmentedNdNetwork augmentedNdNetworks = (DefaultAugmentedNdNetwork) network
                .yangAugmentedInfo(AugmentedNdNetwork.class);
        assertThat(augmentedNdNetworks.yangAugmentedNdNetworkOpType(),
                   is(YmsNetworkTopology.OnosYangOpType.CREATE));

        Link link = augmentedNdNetworks.link().get(0);
        DefaultAugmentedNtLink augmentedNtLink = (DefaultAugmentedNtLink) link
                .yangAugmentedInfo(AugmentedNtLink.class);
        assertThat(augmentedNtLink.yangAugmentedNtLinkOpType(),
                   is(YmsIetfTeTopology.OnosYangOpType.CREATE));

        DefaultBundle bundleStackLevel = (DefaultBundle) augmentedNtLink.te()
                .config().bundleStackLevel();
        assertThat(bundleStackLevel.yangBundleOpType(),
                   is(YmsIetfTeTopology.OnosYangOpType.CREATE));

        BundledLink bundledLink = bundleStackLevel.bundledLinks().bundledLink().get(0);
        assertThat(bundledLink.yangBundledLinkOpType(),
                   is(YmsIetfTeTopology.OnosYangOpType.CREATE));
        assertThat(bundledLink.sequence(), is(1L));
    }

    @Test
    public void testGroupingUsesAugment() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);
        ydtBuilder.addChild(YMS_IETF_NETWORK, null, CREATE);
        ydtBuilder.addChild(NETWORKS, null);
        ydtBuilder.addChild(NETWORK, null);
        ydtBuilder.addChild(LINK, YMS_NETWORK_TOPOLOGY);
        ydtBuilder.addChild(TE, IETF_TE_TOPOLOGY);
        ydtBuilder.addChild(CONFIG, IETF_TE_TOPOLOGY);
        ydtBuilder.addLeaf(TE_LINK_TEMPLATE, null, "1");
        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertThat(yangObject, is(notNullValue()));
        YmsIetfNetworkOpParam ietfNetwork = (YmsIetfNetworkOpParam) yangObject;

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.rev20151208.ymsietfnetwork
                .networks.Network network = ietfNetwork.networks().network().get(0);

        DefaultAugmentedNdNetwork augmentedNdNetworks = (DefaultAugmentedNdNetwork) network
                .yangAugmentedInfo(AugmentedNdNetwork.class);
        assertThat(augmentedNdNetworks.yangAugmentedNdNetworkOpType(),
                   is(YmsNetworkTopology.OnosYangOpType.CREATE));

        Link link = augmentedNdNetworks.link().get(0);
        DefaultAugmentedNtLink augmentedNtLink = (DefaultAugmentedNtLink) link
                .yangAugmentedInfo(AugmentedNtLink.class);
        assertThat(augmentedNtLink.yangAugmentedNtLinkOpType(),
                   is(YmsIetfTeTopology.OnosYangOpType.CREATE));

        assertThat(augmentedNtLink.te().config().teLinkTemplate().get(0),
                   is("1"));
    }
}

