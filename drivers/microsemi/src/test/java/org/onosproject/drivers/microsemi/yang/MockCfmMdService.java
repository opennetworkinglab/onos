/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.drivers.microsemi.yang;

import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.impl.CfmMdManager;
import static org.easymock.EasyMock.*;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.MdStore;

import java.util.Optional;

/**
 * Supports testing of services that reply on the CfmMdService.
 */
public class MockCfmMdService extends CfmMdManager {

    @Override
    public void activate() {
        store = createMock(MdStore.class);

        try {
            MaintenanceAssociation ma = DefaultMaintenanceAssociation
                    .builder(MaIdCharStr.asMaId("ma-1-1"), 6)
                    .maNumericId((short) 1)
                    .ccmInterval(MaintenanceAssociation.CcmInterval.INTERVAL_3MS)
                    .addToRemoteMepIdList(MepId.valueOf((short) 10))
                    .addToRemoteMepIdList(MepId.valueOf((short) 20))
                    .addToComponentList(
                            DefaultComponent.builder(1)
                                    .addToVidList(VlanId.vlanId((short) 101)).build())
                    .build();

            MdId md1Name = MdIdCharStr.asMdId("md-1");
            MaintenanceDomain md1 = DefaultMaintenanceDomain
                    .builder(md1Name)
                    .mdNumericId((short) 1)
                    .mdLevel(MaintenanceDomain.MdLevel.LEVEL3)
                    .addToMaList(ma)
                    .build();

            MdId md2Name = MdIdCharStr.asMdId("md-2");
            MaintenanceDomain md2 = DefaultMaintenanceDomain
                    .builder(md1Name)
                    .mdNumericId((short) 2)
                    .mdLevel(MaintenanceDomain.MdLevel.LEVEL2)
                    .build();

            expect(store.createUpdateMaintenanceDomain(md1))
                    .andReturn(true);
            expect(store.createUpdateMaintenanceDomain(md2))
                    .andReturn(true);
            expect(store.getMaintenanceDomain(md1Name))
                    .andReturn(Optional.of(md1)).anyTimes();
            expect(store.getMaintenanceDomain(md2Name))
                    .andReturn(Optional.of(md2)).anyTimes();
            replay(store);

        } catch (CfmConfigException e) {
            throw new IllegalArgumentException("Error creating MDs for test", e);
        }
    }
}
