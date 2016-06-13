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
package org.onosproject.flowapi;

        import com.google.common.testing.EqualsTester;
        import org.junit.Test;
        import org.onlab.packet.IpAddress;
        import org.onlab.packet.IpPrefix;

        import java.util.ArrayList;
        import java.util.List;

/**
 * Test for extended prefix value attribute.
 */
public class DefaultExtPrefixTest {

    private List<IpPrefix> prefix = new ArrayList<>();
    private List<IpPrefix> prefix1 = new ArrayList<>();
    private IpAddress address = IpAddress.valueOf("192.168.1.1");
    private IpPrefix opVal = IpPrefix.valueOf(address, 16);
    private IpPrefix opVal1 = IpPrefix.valueOf(address, 20);
    private ExtFlowTypes.ExtType type = ExtFlowTypes.ExtType.IPV4_DST_PFX;

    @Test
    public void basics() {
        prefix.add(opVal);
        prefix1.add(opVal1);
        DefaultExtPrefix data = new DefaultExtPrefix(prefix, type);
        DefaultExtPrefix sameAsData = new DefaultExtPrefix(prefix, type);
        DefaultExtPrefix diffData = new DefaultExtPrefix(prefix1, type);
        new EqualsTester().addEqualityGroup(data, sameAsData)
                .addEqualityGroup(diffData).testEquals();
    }
}