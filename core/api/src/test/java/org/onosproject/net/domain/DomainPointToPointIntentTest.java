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

package org.onosproject.net.domain;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ConnectivityIntentTest;
import org.onosproject.net.provider.ProviderId;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.domain.DomainPointToPointIntent.resources;

/**
 * Suite of tests of the point-to-point domain intent descriptor.
 */
public class DomainPointToPointIntentTest extends ConnectivityIntentTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final Link L1 = DefaultLink.builder().providerId(PID).src(FP1.connectPoint())
            .dst(FP2.connectPoint()).type(DIRECT).state(ACTIVE).build();
    private static final List<Link> LINKS_SET = new LinkedList(Arrays.asList(L1));

    @Test
    public void basics() {
        DomainPointToPointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect ingress",
                     intent.filteredIngressPoints().size(), 1);
        assertEquals("incorrect ingress",
                     intent.filteredIngressPoints().iterator().next(), FP1);
        assertEquals("incorrect ingress",
                     intent.filteredEgressPoints().size(), 1);
        assertEquals("incorrect ingress",
                     intent.filteredEgressPoints().iterator().next(), FP2);
        assertEquals(intent.links().size(), 0);
    }

    @Test
    public void links() {
        DomainPointToPointIntent intent = createAnother();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect ingress",
                     intent.filteredIngressPoints().size(), 1);
        assertEquals("incorrect ingress",
                     intent.filteredIngressPoints().iterator().next(), FP2);
        assertEquals("incorrect ingress",
                     intent.filteredEgressPoints().size(), 1);
        assertEquals("incorrect ingress",
                     intent.filteredEgressPoints().iterator().next(), FP1);
        assertEquals("links are not correctly assigned",
                     intent.links(), LINKS_SET);
        assertEquals("resources are not correctly assigned",
                     intent.resources(), resources(LINKS_SET));
    }

    @Override
    protected DomainPointToPointIntent createOne() {
        return DomainPointToPointIntent.builder()
                .appId(APPID)
                .filteredIngressPoint(FP1)
                .filteredEgressPoint(FP2)
                .links(ImmutableList.of()).build();

    }

    @Override
    protected DomainPointToPointIntent createAnother() {
        return DomainPointToPointIntent.builder()
                .appId(APPID)
                .filteredIngressPoint(FP2)
                .filteredEgressPoint(FP1)
                .links(LINKS_SET).build();
    }

}