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

package org.onosproject.pce.pceservice;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.pce.pceservice.PathComputationTest.D2;
import static org.easymock.EasyMock.createMock;

import com.google.common.testing.EqualsTester;


import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.net.intent.constraint.BandwidthConstraint;

import java.util.List;

/**
 * Unit tests for DefaultPcePath class.
 */
public class DefaultPcePathTest {
    private PceStore pceStore = createMock(PceStore.class);

    @Before
    public void setup() {

       ServiceDirectory testDirectory = new TestServiceDirectory()
                   .add(PceStore.class, pceStore);
       BaseResource.setServiceDirectory(testDirectory);
    }

    @After
    public void tearDownTest() {
    }
    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // create same two pce-path objects.
        final String cost1 = "1";
        final String bandwidth1 = "200";
        final String src1 = "foo";
        final String dst1 = "bee";
        final String type1 = "1";
        final String name1 = "pcc";
        final List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D2.deviceId());
        explicitPathInfoList.add(obj);
        PcePath path1 = DefaultPcePath.builder()
                .source(src1)
                .destination(dst1)
                .lspType(type1)
                .name(name1)
                .costConstraint(cost1)
                .bandwidthConstraint(bandwidth1)
                .explicitPathInfo(explicitPathInfoList)
                .build();
        path1.id(TunnelId.valueOf("1"));

        // create same as above object
        PcePath samePath1 = DefaultPcePath.builder()
                .source(src1)
                .destination(dst1)
                .lspType(type1)
                .name(name1)
                .costConstraint(cost1)
                .bandwidthConstraint(bandwidth1)
                .explicitPathInfo(explicitPathInfoList)
                .build();
        samePath1.id(TunnelId.valueOf("1"));

        // Create different pce-path object.
        final String cost2 = "1";
        final String bandwidth2 = "200";
        final String src2 = "google";
        final String dst2 = "yahoo";
        final String type2 = "2";
        final String name2 = "pcc2";

        PcePath path2 = DefaultPcePath.builder()
                .source(src2)
                .destination(dst2)
                .lspType(type2)
                .name(name2)
                .costConstraint(cost2)
                .bandwidthConstraint(bandwidth2)
                .explicitPathInfo(explicitPathInfoList)
                .build();
        path2.id(TunnelId.valueOf("2"));

        new EqualsTester().addEqualityGroup(path1, samePath1).addEqualityGroup(path2).testEquals();
    }

    /**
     * Checks the construction of a DefaultPcePath object.
     */
    @Test
    public void testConstruction() {
        final String cost = "1";
        final String bandwidth = "600";
        final String src = "indiatimes";
        final String dst = "deccan";
        final String type = "2";
        final String name = "pcc4";
        final List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D2.deviceId());
        explicitPathInfoList.add(obj);
        PcePath path = DefaultPcePath.builder()
                .source(src)
                .destination(dst)
                .lspType(type)
                .name(name)
                .costConstraint(cost)
                .bandwidthConstraint(bandwidth)
                .explicitPathInfo(explicitPathInfoList)
                .build();

        assertThat(path.source(), is(src));
        assertThat(path.destination(), is(dst));
        assertThat(path.lspType(), is(LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR));
        assertThat(path.name(), is(name));
        CostConstraint costConstExpected = CostConstraint.of(CostConstraint.Type.values()[Integer.valueOf(cost) - 1]);
        CostConstraint costConstActual = (CostConstraint) path.costConstraint();
        assertThat(costConstActual.type(), is(costConstExpected.type()));
        BandwidthConstraint bandwidthActual = (BandwidthConstraint) path.bandwidthConstraint();
        assertThat(bandwidthActual.bandwidth().bps(), is(Double.valueOf(bandwidth)));
    }
}
