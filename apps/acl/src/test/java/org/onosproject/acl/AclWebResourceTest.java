/*
 * Copyright 2015-present Open Networking Laboratory
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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li and Heng Qi
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */

package org.onosproject.acl;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.rest.BaseResource;
import org.onosproject.core.IdGenerator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Test class for ACL application REST resource.
 */
public class AclWebResourceTest extends JerseyTest {

    final AclService mockAclService = createMock(AclService.class);
    final AclStore mockAclStore = createMock(AclStore.class);
    final List<AclRule> rules = new ArrayList<>();

    /**
     * Constructs a control metrics collector resource test instance.
     */
    public AclWebResourceTest() {
        super(ResourceConfig.forApplicationClass(AclWebApplication.class));
    }

    @Before
    public void setUpMock() {
        expect(mockAclService.getAclRules()).andReturn(rules).anyTimes();
        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(AclService.class, mockAclService)
                .add(AclStore.class, mockAclStore);
        BaseResource.setServiceDirectory(testDirectory);

        IdGenerator idGenerator = new MockIdGenerator();
        AclRule.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        verify(mockAclService);
    }

    /**
     * Mock id generator for testing.
     */
    private class MockIdGenerator implements IdGenerator {
        private AtomicLong nextId = new AtomicLong(0);

        @Override
        public long getNewId() {
            return nextId.getAndIncrement();
        }
    }

    @Test
    @Ignore("FIXME: This needs to get reworked")
    public void addRule() throws IOException {
        WebTarget wt = target();
        String response;
        String json;

        replay(mockAclService);

        // FIXME: following code snippet requires refactoring by extracting
        // json string as a separated file

        // input a invalid JSON string that contains neither nw_src and nw_dst
        json = "{\"ipProto\":\"TCP\",\"dstTpPort\":\"80\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Failed! Either srcIp or dstIp must be assigned."));

        // input a invalid JSON string that doesn't contain CIDR mask bits
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.1\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Malformed IPv4 prefix string: 10.0.0.1. " +
                                                    "Address must take form \"x.x.x.x/y\""));

        // input a invalid JSON string that contains a invalid IP address
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.256/32\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Invalid IP address string: 10.0.0.256"));

        // input a invalid JSON string that contains a invalid IP address
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.01/32\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Invalid IP address string: 10.0.01"));

        // input a invalid JSON string that contains a invalid CIDR mask bits
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.1/a\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Failed! For input string: \"a\""));

        // input a invalid JSON string that contains a invalid CIDR mask bits
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.1/33\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("Invalid prefix length 33. The value must be in the interval [0, 32]"));

        // input a invalid JSON string that contains a invalid ipProto value
        json = "{\"ipProto\":\"ARP\",\"srcIp\":\"10.0.0.1/32\",\"dstTpPort\":\"80\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("ipProto must be assigned to TCP, UDP, or ICMP."));

        // input a invalid JSON string that contains a invalid dstTpPort value
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.1/32\",\"dstTpPort\":\"a\",\"action\":\"DENY\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("dstTpPort must be assigned to a numerical value."));

        // input a invalid JSON string that contains a invalid action value
        json = "{\"ipProto\":\"TCP\",\"srcIp\":\"10.0.0.1/32\",\"dstTpPort\":\"80\",\"action\":\"PERMIT\"}";
        response = wt.request().post(Entity.json(json), String.class);
        assertThat(response, containsString("action must be assigned to ALLOW or DENY."));
    }
}
