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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.Beta;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.InvalidFieldException;

import java.io.InputStream;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * Tests for class {@link HostLearningConfig}.
 */
@Beta
public class HostLearningConfigTest {
    private ConnectPoint cp;
    private HostLearningConfig config;
    private HostLearningConfig invalidConfig;

    /**
     * Initialize test related variables.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        InputStream jsonStream = HostLearningConfigTest.class
                .getResourceAsStream("/host-learning-config.json");
        InputStream invalidJsonStream = HostLearningConfigTest.class
                .getResourceAsStream("/host-learning-config-invalid.json");

        cp = new ConnectPoint(DeviceId.deviceId(new URI("of:0000000000000202")), PortNumber.portNumber((long) 5));
        ConnectPoint subject = cp;
        String key = CoreService.CORE_APP_NAME;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        JsonNode invalidJsonNode = mapper.readTree(invalidJsonStream);
        ConfigApplyDelegate delegate = new MockDelegate();

        config = new HostLearningConfig();
        config.init(subject, key, jsonNode, mapper, delegate);
        invalidConfig = new HostLearningConfig();
        invalidConfig.init(subject, key, invalidJsonNode, mapper, delegate);
    }

    /**
     * Tests config validity.
     *
     */
    @Test(expected = InvalidFieldException.class)
    public void isValid() {
        assertTrue(config.isValid());
        assertFalse(invalidConfig.isValid());
    }

    /**
     * Tests enabled setter.
     *
     */
    @Test
    public void testEnableLearning() {
        config.setEnabled("false");
        assertTrue(!config.hostLearningEnabled());
        config.setEnabled("true");
        assertTrue(config.hostLearningEnabled());
    }

    private class MockDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
        }
    }
}
