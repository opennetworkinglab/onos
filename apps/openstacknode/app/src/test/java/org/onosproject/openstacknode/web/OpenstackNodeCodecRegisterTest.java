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
package org.onosproject.openstacknode.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
import org.onosproject.openstacknode.codec.DpdkConfigCodec;
import org.onosproject.openstacknode.codec.DpdkInterfaceCodec;
import org.onosproject.openstacknode.codec.OpenstackAuthCodec;
import org.onosproject.openstacknode.codec.OpenstackControllerCodec;
import org.onosproject.openstacknode.codec.OpenstackNodeCodec;
import org.onosproject.openstacknode.codec.OpenstackPhyInterfaceCodec;
import org.onosproject.openstacknode.codec.OpenstackSshAuthCodec;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for openstack node codec register.
 */
public final class OpenstackNodeCodecRegisterTest {

    private OpenstackNodeCodecRegister register;

    /**
     * Tests codec register activation and deactivation.
     */
    @Test
    public void testActivateDeactivate() {
        register = new OpenstackNodeCodecRegister();
        CodecService codecService = new TestCodecService();

        TestUtils.setField(register, "codecService", codecService);
        register.activate();

        assertEquals(OpenstackNodeCodec.class.getName(),
                codecService.getCodec(OpenstackNode.class).getClass().getName());
        assertEquals(OpenstackAuthCodec.class.getName(),
                codecService.getCodec(OpenstackAuth.class).getClass().getName());
        assertEquals(OpenstackPhyInterfaceCodec.class.getName(),
                codecService.getCodec(OpenstackPhyInterface.class).getClass().getName());
        assertEquals(OpenstackControllerCodec.class.getName(),
                codecService.getCodec(ControllerInfo.class).getClass().getName());
        assertEquals(OpenstackSshAuthCodec.class.getName(),
                codecService.getCodec(OpenstackSshAuth.class).getClass().getName());
        assertEquals(DpdkConfigCodec.class.getName(),
                codecService.getCodec(DpdkConfig.class).getClass().getName());
        assertEquals(DpdkInterfaceCodec.class.getName(),
                codecService.getCodec(DpdkInterface.class).getClass().getName());

        register.deactivate();

        assertNull(codecService.getCodec(OpenstackNode.class));
        assertNull(codecService.getCodec(OpenstackAuth.class));
        assertNull(codecService.getCodec(OpenstackPhyInterface.class));
        assertNull(codecService.getCodec(ControllerInfo.class));
        assertNull(codecService.getCodec(OpenstackSshAuth.class));
        assertNull(codecService.getCodec(DpdkConfig.class));
        assertNull(codecService.getCodec(DpdkInterface.class));
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
