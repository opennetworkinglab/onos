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

package org.onosproject.tetunnel.api.tunnel.path;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.lsp.TeLspKey;

import java.util.List;

/**
 * Unit tests for default TE path implementation.
 */
public class DefaultTePathTest {

    /**
     * Tests constructor of DefaultTePathSelection.
     */
    @Test
    public void testConstructorOfDefaultTePathSelection() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final long costLimit = 4;
        final short hopLimit = 5;

        TePathSelection tePathSelection = new DefaultTePathSelection(
                new TeTopologyKey(providerId, clientId, topologyId),
                costLimit, hopLimit);
        Assert.assertEquals(tePathSelection.teTopologyKey(),
                            new TeTopologyKey(providerId, clientId, topologyId));
        Assert.assertEquals(tePathSelection.costLimit(), costLimit);
        Assert.assertEquals(tePathSelection.hopLimit(), hopLimit);
    }

    /**
     * Tests constructor of DefaultTeRouteUnnumberedLink.
     */
    @Test
    public void testConstructorOfDefaultTeRouteUnnumberedLink() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final int teNodeId = 4;
        final int teTtpId = 5;

        TeRouteUnnumberedLink teRouteUnnumberedLink =
                new DefaultTeRouteUnnumberedLink(
                        new TeNodeKey(providerId, clientId,
                                      topologyId, teNodeId),
                        new TtpKey(providerId, clientId,
                                   topologyId, teNodeId, teTtpId));

        Assert.assertEquals(teRouteUnnumberedLink.type(),
                            TeRouteSubobject.Type.UNNUMBERED_LINK);
        Assert.assertEquals(teRouteUnnumberedLink.node(),
                            new TeNodeKey(providerId, clientId,
                                          topologyId, teNodeId));
        Assert.assertEquals(teRouteUnnumberedLink.ttp(),
                            new TtpKey(providerId, clientId,
                                       topologyId, teNodeId, teTtpId));
    }

    /**
     * Tests constructor of DefaultTePath.
     */
    @Test
    public void testConstructorOfDefaultTePath() {
        final int providerId = 1;
        final int clientId = 2;
        final int topologyId = 3;
        final int teNodeId = 4;
        final int teTtpId = 5;
        final int teLspId = 6;

        List<TeLspKey> lspKeys = Lists.newArrayList(
                new TeLspKey(providerId, clientId, topologyId, teLspId));

        TeRouteUnnumberedLink teRouteUnnumberedLink =
                new DefaultTeRouteUnnumberedLink(
                        new TeNodeKey(providerId, clientId,
                                      topologyId, teNodeId),
                        new TtpKey(providerId, clientId,
                                   topologyId, teNodeId, teTtpId));
        List<TeRouteSubobject> explicitRoute = Lists.newArrayList(
                teRouteUnnumberedLink);

        List<TePath> secondaryPaths = Lists.newArrayList(
                new DefaultTePath(TePath.Type.DYNAMIC, null, null, null)
        );

        TePath tePath = new DefaultTePath(TePath.Type.EXPLICIT, lspKeys,
                                          explicitRoute, secondaryPaths);

        Assert.assertEquals(tePath.type(), TePath.Type.EXPLICIT);
        Assert.assertEquals(tePath.explicitRoute(), explicitRoute);
        Assert.assertEquals(tePath.lsps(), lspKeys);
        Assert.assertEquals(tePath.secondaryPaths(), secondaryPaths);
    }
}
