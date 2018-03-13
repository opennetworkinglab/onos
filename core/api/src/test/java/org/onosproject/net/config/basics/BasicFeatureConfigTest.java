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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Test;
import org.onosproject.net.config.ConfigApplyDelegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BasicFeatureConfigTest {

    class TestConfig extends BasicFeatureConfig<String> {
        TestConfig(boolean defaultValue) {
            super(defaultValue);
        }
    }

    @Test
    public void basicTest() {
        ConfigApplyDelegate delegate = configApply -> { };
        ObjectMapper mapper = new ObjectMapper();

        TestConfig enabled = new TestConfig(true);
        TestConfig disabled = new TestConfig(false);

        enabled.init("enabled", "KEY", JsonNodeFactory.instance.objectNode(), mapper, delegate);
        disabled.init("disabled", "KEY", JsonNodeFactory.instance.objectNode(), mapper, delegate);

        assertThat(enabled.enabled(), is(true));
        assertThat(disabled.enabled(), is(false));

        disabled.enabled(true);
        enabled.enabled(false);
        assertThat(enabled.enabled(), is(false));
        assertThat(disabled.enabled(), is(true));
    }
}