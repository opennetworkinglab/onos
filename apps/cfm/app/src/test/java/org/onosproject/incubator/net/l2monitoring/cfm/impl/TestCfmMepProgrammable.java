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
package org.onosproject.incubator.net.l2monitoring.cfm.impl;

import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMep;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepProgrammable;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.onosproject.incubator.net.l2monitoring.cfm.impl.CfmMepManagerTest.*;

/**
 * A dummy implementation of the CfmMepProgrammable for test purposes.
 */
public class TestCfmMepProgrammable extends AbstractHandlerBehaviour implements CfmMepProgrammable {

    private List<Mep> deviceMepList;

    public TestCfmMepProgrammable() throws CfmConfigException {
        deviceMepList = new ArrayList<>();

        deviceMepList.add(DefaultMep.builder(MEPID1, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build());
        deviceMepList.add(DefaultMep.builder(MEPID11, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build());
        deviceMepList.add(DefaultMep.builder(MEPID12, DEVICE_ID1, PortNumber.P0,
                Mep.MepDirection.UP_MEP, MDNAME2, MANAME2).build());
        deviceMepList.add(DefaultMep.builder(MEPID2, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build());
        deviceMepList.add(DefaultMep.builder(MEPID21, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME1, MANAME1).build());
        deviceMepList.add(DefaultMep.builder(MEPID22, DEVICE_ID2, PortNumber.portNumber(2),
                Mep.MepDirection.UP_MEP, MDNAME2, MANAME2).build());
    }

    @Override
    public MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {
        for (Mep mep:deviceMepList) {
            if (mep.mdId().equals(mdName) && mep.maId().equals(maName) && mep.mepId().equals(mepId)) {
                return DefaultMepEntry.builder(mep).buildEntry();
            }
        }
        return null;
    }

    @Override
    public boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId,
                             Optional<MaintenanceDomain> oldMd) throws CfmConfigException {
        return true;
    }

    @Override
    public boolean createMep(MdId mdName, MaIdShort maName, Mep mep) throws CfmConfigException {
        return true;
    }

    @Override
    public boolean deleteMdOnDevice(MdId mdId, Optional<MaintenanceDomain> oldMd) throws CfmConfigException {
        return false;
    }

    @Override
    public boolean deleteMaOnDevice(MdId mdId, MaIdShort maId,
                                    Optional<MaintenanceDomain> oldMd) throws CfmConfigException {
        return false;
    }

    @Override
    public boolean createMdOnDevice(MdId mdId) throws CfmConfigException {
        return false;
    }

    @Override
    public boolean createMaOnDevice(MdId mdId, MaIdShort maId) throws CfmConfigException {
        return false;
    }

    @Override
    public boolean createMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException {
        return false;
    }

    @Override
    public boolean deleteMaRemoteMepOnDevice(MdId mdId, MaIdShort maId, MepId remoteMep) throws CfmConfigException {
        return false;
    }

    @Override
    public void transmitLoopback(MdId mdName, MaIdShort maName, MepId mepId, MepLbCreate lbCreate)
            throws CfmConfigException {

    }

    @Override
    public void abortLoopback(MdId mdName, MaIdShort maName, MepId mepId) throws CfmConfigException {

    }

    @Override
    public void transmitLinktrace(MdId mdName, MaIdShort maName, MepId mepId, MepLtCreate ltCreate)
            throws CfmConfigException {

    }
}
