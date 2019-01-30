/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.codec.rest.FlowInfoJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.StatsFlowRuleJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.StatsInfoJsonCodec;
import org.onosproject.openstacktelemetry.codec.rest.TelemetryConfigJsonCodec;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for openstack telemetry codec register.
 */
public final class OpenstackRestCodecRegisterTest {

    private OpenstackRestCodecRegister register;

    /**
     * Tests codec register activation and deactivation.
     */
    @Test
    public void testActivateDeactivate() {
        register = new OpenstackRestCodecRegister();
        CodecService codecService = new TestCodecService();

        TestUtils.setField(register, "codecService", codecService);
        register.activate();

        assertEquals(StatsInfoJsonCodec.class.getName(),
                codecService.getCodec(StatsInfo.class).getClass().getName());
        assertEquals(FlowInfoJsonCodec.class.getName(),
                codecService.getCodec(FlowInfo.class).getClass().getName());
        assertEquals(StatsFlowRuleJsonCodec.class.getName(),
                codecService.getCodec(StatsFlowRule.class).getClass().getName());
        assertEquals(TelemetryConfigJsonCodec.class.getName(),
                codecService.getCodec(TelemetryConfig.class).getClass().getName());

        register.deactivate();

        assertNull(codecService.getCodec(StatsInfo.class));
        assertNull(codecService.getCodec(FlowInfo.class));
        assertNull(codecService.getCodec(StatsFlowRule.class));
        assertNull(codecService.getCodec(TelemetryConfig.class));
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
