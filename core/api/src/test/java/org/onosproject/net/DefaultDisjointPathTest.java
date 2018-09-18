/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.onlab.graph.ScalarWeight;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.PID;

/**
 * Unit tests for the DefaultDisjointPathTest.
 */
public class DefaultDisjointPathTest {
    private static DefaultLink link1 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev1", 1))
                    .dst(NetTestTools.connectPoint("dev2", 1)).build();

    private static DefaultLink link2 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev1", 1))
                    .dst(NetTestTools.connectPoint("dev2", 1)).build();

    private static DefaultLink link3 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev2", 1))
                    .dst(NetTestTools.connectPoint("dev3", 1)).build();

    private static List<Link> links1 = ImmutableList.of(link1, link2);
    private static DefaultPath path1 =
            new DefaultPath(PID, links1, ScalarWeight.toWeight(1.0));

    private static List<Link> links2 = ImmutableList.of(link2, link1);
    private static DefaultPath path2 =
            new DefaultPath(PID, links2, ScalarWeight.toWeight(2.0));

    private static List<Link> links3 = ImmutableList.of(link1, link2, link3);
    private static DefaultPath path3 =
            new DefaultPath(PID, links3, ScalarWeight.toWeight(3.0));

    private static DefaultDisjointPath disjointPath1 =
            new DefaultDisjointPath(PID, path1, path2);
    private static DefaultDisjointPath sameAsDisjointPath1 =
            new DefaultDisjointPath(PID, path1, path2);
    private static DefaultDisjointPath disjointPath2 =
            new DefaultDisjointPath(PID, path2, path1);
    private static DefaultDisjointPath disjointPath3 =
            new DefaultDisjointPath(PID, path1, path3);
    private static DefaultDisjointPath disjointPath4 =
            new DefaultDisjointPath(PID, path1, null);


    /**
     * Tests construction and fetching of member data.
     */
    @Test
    public void testConstruction() {
        assertThat(disjointPath1.primary(), is(path1));
        assertThat(disjointPath1.backup(), is(path2));
        assertThat(disjointPath1.links(), is(links1));
        assertThat(disjointPath1.weight(), is(ScalarWeight.toWeight(1.0)));
    }

    /**
     * Tests equals(), hashCode(), and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(disjointPath1, sameAsDisjointPath1, disjointPath2)
                .addEqualityGroup(disjointPath3)
                .addEqualityGroup(disjointPath4)
                .testEquals();
    }
}
