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

package org.onosproject.tetunnel.api.tunnel;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.path.DefaultTePath;
import org.onosproject.tetunnel.api.tunnel.path.TePath;

import java.util.List;

/**
 * Unit tests for default TE tunnel implementation.
 */
public class DefaultTeTunnelTest {

    /**
     * Tests constructor of TeTunnelKey.
     */
    @Test
    public void testConstructorOfTeTunnelKey() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final int teTunnelId = 4;

        TeTunnelKey key = new TeTunnelKey(providerId, clientId, topologyId,
                                          teTunnelId);

        Assert.assertEquals(key.teTunnelId(), teTunnelId);
        Assert.assertEquals(key.teTopologyKey(),
                            new TeTopologyKey(providerId, clientId,
                                              topologyId));
        Assert.assertTrue(key.equals(
                          new TeTunnelKey(providerId, clientId, topologyId,
                                          teTunnelId)));
    }

    /**
     * Tests constructor of TeLspKey.
     */
    @Test
    public void testConstructorOfTeLspKey() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final int teLspId = 4;

        TeLspKey key = new TeLspKey(providerId, clientId, topologyId,
                                       teLspId);

        Assert.assertEquals(key.teLspId(), teLspId);
        Assert.assertEquals(key.teTopologyKey(),
                            new TeTopologyKey(providerId, clientId,
                                              topologyId));
        Assert.assertTrue(key.equals(
                new TeLspKey(providerId, clientId, topologyId,
                                teLspId)));
    }

    /**
     * Tests builder of the DefaultTeTunnel.
     */
    @Test
    public void testDefaultTeTunnelBuilder() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final int srcNodeId = 4;
        final int srcTtpId = 5;
        final int dstNodeId = 6;
        final int dstTtpId = 7;
        final int teTunnelId = 8;
        final String teTunnelName = "Test TE tunnel";
        final List<TePath> paths = Lists.newArrayList(
                new DefaultTePath(TePath.Type.DYNAMIC, null, null, null));
        final int segTunnelId1 = 1001;
        final int segTunnelId2 = 1002;
        final int segTunnelId3 = 1003;

        TeTunnel teTunnel = DefaultTeTunnel.builder()
                .teTunnelKey(
                        new TeTunnelKey(providerId, clientId, topologyId,
                                        teTunnelId))
                .srcNode(new TeNodeKey(providerId, clientId, topologyId,
                                       srcNodeId))
                .srcTp(new TtpKey(providerId, clientId, topologyId, srcNodeId,
                                  srcTtpId))
                .dstNode(new TeNodeKey(providerId, clientId, topologyId,
                                       dstNodeId))
                .dstTp(new TtpKey(providerId, clientId, topologyId, dstNodeId,
                                  dstTtpId))
                .name(teTunnelName)
                .adminState(TeTunnel.State.UP)
                .lspProtectionType(TeTunnel.LspProtectionType.LSP_PROT_REROUTE)
                .type(TeTunnel.Type.P2P)
                .primaryPaths(paths)
                .build();

        Assert.assertEquals(teTunnel.teTunnelKey().teTopologyKey(),
                            new TeTopologyKey(providerId, clientId,
                                              topologyId));
        Assert.assertEquals(teTunnel.teTunnelKey().teTunnelId(), teTunnelId);
        Assert.assertEquals(teTunnel.srcNode(),
                            new TeNodeKey(providerId, clientId, topologyId,
                                                              srcNodeId));
        Assert.assertEquals(teTunnel.dstNode(),
                            new TeNodeKey(providerId, clientId, topologyId,
                                          dstNodeId));
        Assert.assertEquals(teTunnel.srcTp(),
                            new TtpKey(providerId, clientId, topologyId,
                                       srcNodeId, srcTtpId));
        Assert.assertEquals(teTunnel.dstTp(),
                            new TtpKey(providerId, clientId, topologyId,
                                       dstNodeId, dstTtpId));
        Assert.assertEquals(teTunnel.name(), teTunnelName);
        Assert.assertEquals(teTunnel.adminStatus(), TeTunnel.State.UP);
        Assert.assertEquals(teTunnel.lspProtectionType(),
                            TeTunnel.LspProtectionType.LSP_PROT_REROUTE);
        Assert.assertEquals(teTunnel.type(), TeTunnel.Type.P2P);
        Assert.assertEquals(teTunnel.primaryPaths().get(0).type(),
                            TePath.Type.DYNAMIC);
        Assert.assertEquals(teTunnel.primaryPaths(), paths);

        TeTunnelKey segTunnel1 = new TeTunnelKey(providerId, clientId,
                                                 topologyId, segTunnelId1);
        TeTunnelKey segTunnel2 = new TeTunnelKey(providerId, clientId,
                                                 topologyId, segTunnelId2);
        TeTunnelKey segTunnel3 = new TeTunnelKey(providerId, clientId,
                                                 topologyId, segTunnelId3);
        List<TeTunnelKey> segTunnels = Lists.newArrayList(segTunnel1,
                                                          segTunnel2,
                                                          segTunnel3);
        teTunnel.segmentTunnels(segTunnels);
        Assert.assertEquals(teTunnel.segmentTunnels(), segTunnels);
    }
}
