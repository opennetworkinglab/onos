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
package org.onosproject.vtnrsc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for ServiceFunctionGroup class.
 */
public class ServiceFunctionGroupTest {

    /**
     * Checks that the ServiceFunctionGroup class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(ServiceFunctionGroup.class);
    }

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        String name1 = "Firewall";
        String description1 = "Firewall service function";
        Map<PortPairId, Integer> portPairLoadMap1 = new ConcurrentHashMap<>();
        PortPairId portPairId1 = PortPairId.of("a4444444-4a56-2a6e-cd3a-9dee4e2ec345");
        portPairLoadMap1.put(portPairId1, Integer.valueOf(2));
        ServiceFunctionGroup sfg1 = new ServiceFunctionGroup(name1, description1, portPairLoadMap1);
        ServiceFunctionGroup sameAsSfg1 = new ServiceFunctionGroup(name1, description1, portPairLoadMap1);

        String name2 = "Dpi";
        String description2 = "Dpi service function";
        Map<PortPairId, Integer> portPairLoadMap2 = new ConcurrentHashMap<>();
        PortPairId portPairId2 = PortPairId.of("b6666666-4a56-2a6e-cd3a-9dee4e2ec345");
        portPairLoadMap2.put(portPairId2, Integer.valueOf(3));
        ServiceFunctionGroup sfg2 = new ServiceFunctionGroup(name2, description2, portPairLoadMap2);

        new EqualsTester().addEqualityGroup(sfg1, sameAsSfg1).addEqualityGroup(sfg2).testEquals();
    }

    /**
     * Checks the construction of a ServiceFunctionGroup object.
     */
    @Test
    public void testConstruction() {

        String name = "Firewall";
        String description = "Firewall service function";
        Map<PortPairId, Integer> portPairLoadMap = new ConcurrentHashMap<>();
        PortPairId portPairId = PortPairId.of("a4444444-4a56-2a6e-cd3a-9dee4e2ec345");
        portPairLoadMap.put(portPairId, Integer.valueOf(2));
        ServiceFunctionGroup sfg = new ServiceFunctionGroup(name, description, portPairLoadMap);

        assertThat(name, is(sfg.name()));
        assertThat(description, is(sfg.description()));
        assertThat(2, is(sfg.portPairLoadMap().get(portPairId)));
    }
}
