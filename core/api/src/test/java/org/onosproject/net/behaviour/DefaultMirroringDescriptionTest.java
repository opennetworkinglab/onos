/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.net.behaviour;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.onlab.packet.VlanId;

import com.google.common.collect.ImmutableList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class DefaultMirroringDescriptionTest {

    private static final MirroringName NAME_1 = MirroringName.mirroringName("mirror1");
    private static final List<String> MONITOR_SRC_PORTS_1 =
            ImmutableList.of("s1", "s2", "s3");
    private static final List<String> MONITOR_DST_PORTS_1 =
            ImmutableList.of("d1", "d2");
    private static final List<VlanId> MONITOR_VLANS_1 = ImmutableList.of(VlanId.ANY);
    private static final Optional<String> MIRROR_PORT_1 = Optional.of("port1");
    private static final Optional<VlanId> MIRROR_VLAN_1 = Optional.of(VlanId.ANY);
    private MirroringDescription md1 =
            new DefaultMirroringDescription(NAME_1, MONITOR_SRC_PORTS_1,
                                            MONITOR_DST_PORTS_1, MONITOR_VLANS_1,
                                            MIRROR_PORT_1, MIRROR_VLAN_1);


    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultMirroringDescription.class);
    }

    @Test
    public void testConstruction() {
        assertThat(md1.name(), is(NAME_1));
        assertThat(md1.monitorSrcPorts(), is(MONITOR_SRC_PORTS_1));
        assertThat(md1.monitorDstPorts(), is(MONITOR_DST_PORTS_1));
        assertThat(md1.monitorVlans(), is(MONITOR_VLANS_1));
        assertThat(md1.mirrorPort(), is(MIRROR_PORT_1));
        assertThat(md1.mirrorVlan(), is(MIRROR_VLAN_1));
    }

    @Test
    public void testToString() {
        String result = md1.toString();
        assertThat(result, notNullValue());
        assertThat(result, containsString("name=" + NAME_1.toString()));
        assertThat(result, containsString("monitorsrcports=" + MONITOR_SRC_PORTS_1.toString()));
        assertThat(result, containsString("monitordstports=" + MONITOR_DST_PORTS_1.toString()));
        assertThat(result, containsString("monitorvlans=" + MONITOR_VLANS_1.toString()));
        assertThat(result, containsString("mirrorport=" + MIRROR_PORT_1.toString()));
        assertThat(result, containsString("mirrorvlan=" + MIRROR_VLAN_1.toString()));
    }
}
