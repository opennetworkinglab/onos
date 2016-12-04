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
package org.onosproject.pce.pcestore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.testing.EqualsTester;

import java.util.List;
import java.util.LinkedList;

import org.junit.Test;
import org.onlab.util.DataRateUnit;
import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.pce.pceservice.LspType;

/**
 * Unit tests for PcePathInfo class.
 */
public class PcePathInfoTest {

    /**
     * Checks the operation of equals() methods.
     */
    @Test
    public void testEquals() {
        // create same two objects.
        DeviceId src1 = DeviceId.deviceId("foo1");
        DeviceId dst1 = DeviceId.deviceId("goo1");
        String name1 = "pcc1";
        LspType lspType1 = LspType.WITH_SIGNALLING;
        List<Constraint> constraints1 = new LinkedList<>();
        Constraint bandwidth11 = BandwidthConstraint.of(100, DataRateUnit.BPS);
        constraints1.add(bandwidth11);
        Constraint bandwidth12 = BandwidthConstraint.of(200, DataRateUnit.BPS);
        constraints1.add(bandwidth12);
        Constraint bandwidth13 = BandwidthConstraint.of(300, DataRateUnit.BPS);
        constraints1.add(bandwidth13);

        PcePathInfo pathInfo1 = new PcePathInfo(src1, dst1, name1, constraints1, lspType1, null);

        // create same object as above object
        PcePathInfo samePathInfo1 = new PcePathInfo(src1, dst1, name1, constraints1, lspType1, null);

        // Create different object.
        DeviceId src2 = DeviceId.deviceId("foo2");
        DeviceId dst2 = DeviceId.deviceId("goo2");
        String name2 = "pcc2";
        LspType lspType2 = LspType.SR_WITHOUT_SIGNALLING;
        List<Constraint> constraints2 = new LinkedList<>();
        Constraint bandwidth21 = BandwidthConstraint.of(400, DataRateUnit.BPS);
        constraints2.add(bandwidth21);
        Constraint bandwidth22 = BandwidthConstraint.of(800, DataRateUnit.BPS);
        constraints2.add(bandwidth22);

        PcePathInfo pathInfo2 = new PcePathInfo(src2, dst2, name2, constraints2, lspType2, null);

        new EqualsTester().addEqualityGroup(pathInfo1, samePathInfo1)
                          .addEqualityGroup(pathInfo2)
                          .testEquals();
    }

    /**
     * Checks the construction of a PcePathInfo object.
     */
    @Test
    public void testConstruction() {
        DeviceId src = DeviceId.deviceId("foo2");
        DeviceId dst = DeviceId.deviceId("goo2");
        String name = "pcc2";
        LspType lspType = LspType.SR_WITHOUT_SIGNALLING;
        List<Constraint> constraints = new LinkedList<>();
        Constraint bandwidth1 = BandwidthConstraint.of(100, DataRateUnit.BPS);
        constraints.add(bandwidth1);
        Constraint bandwidth2 = BandwidthConstraint.of(200, DataRateUnit.BPS);
        constraints.add(bandwidth2);
        Constraint bandwidth3 = BandwidthConstraint.of(300, DataRateUnit.BPS);
        constraints.add(bandwidth3);

        PcePathInfo pathInfo = new PcePathInfo(src, dst, name, constraints, lspType, null);

        assertThat(src, is(pathInfo.src()));
        assertThat(dst, is(pathInfo.dst()));
        assertThat(name, is(pathInfo.name()));
        assertThat(constraints, is(pathInfo.constraints()));
        assertThat(lspType, is(pathInfo.lspType()));
    }
}
