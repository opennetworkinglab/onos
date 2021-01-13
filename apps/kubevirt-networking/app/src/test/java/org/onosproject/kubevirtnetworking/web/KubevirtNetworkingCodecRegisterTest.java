/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.codec.KubevirtHostRouteCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtIpPoolCodec;
import org.onosproject.kubevirtnetworking.codec.KubevirtNetworkCodec;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for kubevirt networking codec register.
 */
public class KubevirtNetworkingCodecRegisterTest {

    /**
     * Tests codec register activation and deactivation.
     */
    @Test
    public void testActivateDeactivate() {
        KubevirtNetworkingCodecRegister register = new KubevirtNetworkingCodecRegister();
        CodecService codecService = new TestCodecService();

        TestUtils.setField(register, "codecService", codecService);
        register.activate();

        assertEquals(KubevirtNetworkCodec.class.getName(),
                codecService.getCodec(KubevirtNetwork.class).getClass().getName());
        assertEquals(KubevirtHostRouteCodec.class.getName(),
                codecService.getCodec(KubevirtHostRoute.class).getClass().getName());
        assertEquals(KubevirtIpPoolCodec.class.getName(),
                codecService.getCodec(KubevirtIpPool.class).getClass().getName());

        register.deactivate();
    }

    private static class TestCodecService implements CodecService {

        private Map<String, JsonCodec> codecMap = Maps.newConcurrentMap();

        @Override
        public Set<Class<?>> getCodecs() {
            return ImmutableSet.of();
        }

        @Override
        public <T> JsonCodec<T> getCodec(Class<T> entityClass) {
            return codecMap.get(entityClass.getName());
        }

        @Override
        public <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec) {
            codecMap.put(entityClass.getName(), codec);
        }

        @Override
        public void unregisterCodec(Class<?> entityClass) {
            codecMap.remove(entityClass.getName());
        }
    }
}
