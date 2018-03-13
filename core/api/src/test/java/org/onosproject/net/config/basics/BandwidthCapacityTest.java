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
import org.onlab.util.Bandwidth;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.ConfigApplyDelegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class BandwidthCapacityTest {

    @Test
    public void testConstruction() {
        BandwidthCapacity config = new BandwidthCapacity();
        ConfigApplyDelegate delegate = configApply -> { };
        ObjectMapper mapper = new ObjectMapper();
        ConnectPoint cp = NetTestTools.connectPoint("cp1", 3);

        config.init(cp, "KEY", JsonNodeFactory.instance.objectNode(), mapper, delegate);

        double expectedBw = 1.0;
        config.capacity(Bandwidth.mbps(expectedBw));
        assertThat(config.isValid(), is(true));
        assertThat(config.toString(), containsString("capacity"));

    }

}