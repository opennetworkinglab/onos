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
package org.onosproject.incubator.net.l2monitoring.soam.impl;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepTsCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamDmProgrammable;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DefaultDelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * A dummy implementation of the SoamDmProgrammable for test purposes.
 */
public class TestSoamDmProgrammable extends AbstractHandlerBehaviour implements SoamDmProgrammable {
    private DelayMeasurementEntry dmEntry1;

    public TestSoamDmProgrammable() throws SoamConfigException {
        long nowMs = System.currentTimeMillis();
        long lastSecond = nowMs - nowMs % 1000;
        DelayMeasurementStatCurrent current =
                (DelayMeasurementStatCurrent) DefaultDelayMeasurementStatCurrent
                        .builder(Duration.ofSeconds(37), false)
                    .startTime(Instant.ofEpochMilli(lastSecond))
                    .build();

        long lastMinute = nowMs - nowMs % (60 * 1000);
        DelayMeasurementStatHistory history1 =
                (DelayMeasurementStatHistory) DefaultDelayMeasurementStatHistory
                .builder(SoamId.valueOf(67), Duration.ofSeconds(60), false)
                .endTime(Instant.ofEpochMilli(lastMinute))
                .frameDelayForwardMin(Duration.ofMillis(107))
                .frameDelayForwardMax(Duration.ofMillis(109))
                .frameDelayForwardAvg(Duration.ofMillis(108))
                .build();

        long lastMinute2 = lastMinute - (60 * 1000);
        DelayMeasurementStatHistory history2 =
                (DelayMeasurementStatHistory) DefaultDelayMeasurementStatHistory
                        .builder(SoamId.valueOf(66), Duration.ofSeconds(60), false)
                        .endTime(Instant.ofEpochMilli(lastMinute2))
                        .frameDelayForwardMin(Duration.ofMillis(117))
                        .frameDelayForwardMax(Duration.ofMillis(119))
                        .frameDelayForwardAvg(Duration.ofMillis(118))
                        .build();

        dmEntry1 = DefaultDelayMeasurementEntry
                .builder(SoamManagerTest.DMID101, DelayMeasurementCreate.DmType.DM1DMTX,
                        DelayMeasurementCreate.Version.Y17312011,
                        MepId.valueOf((short) 11), Mep.Priority.PRIO5)
                .currentResult(current)
                .addToHistoricalResults(history1)
                .addToHistoricalResults(history2)
                .build();
    }

    @Override
    public Collection<DelayMeasurementEntry> getAllDms(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException, SoamConfigException {
        Collection<DelayMeasurementEntry> dmEntries = new ArrayList<>();
        if (mdName.equals(SoamManagerTest.MDNAME1) && maName.equals(SoamManagerTest.MANAME1)
                && mepId.equals(SoamManagerTest.MEPID1)) {
            dmEntries.add(dmEntry1);
            return dmEntries;
        }
        return new ArrayList<>();
    }

    @Override
    public DelayMeasurementEntry getDm(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException, SoamConfigException {
        if (mdName.equals(SoamManagerTest.MDNAME1) && maName.equals(SoamManagerTest.MANAME1)
                && mepId.equals(SoamManagerTest.MEPID1)) {
            return dmEntry1;
        }
        return null;
    }

    @Override
    public DelayMeasurementStatCurrent getDmCurrentStat(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException, SoamConfigException {
        if (mdName.equals(SoamManagerTest.MDNAME1) && maName.equals(SoamManagerTest.MANAME1)
                && mepId.equals(SoamManagerTest.MEPID1)) {
            return dmEntry1.currentResult();
        }
        return null;
    }

    @Override
    public Collection<DelayMeasurementStatHistory> getDmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException, SoamConfigException {
        if (mdName.equals(SoamManagerTest.MDNAME1) && maName.equals(SoamManagerTest.MANAME1)
                && mepId.equals(SoamManagerTest.MEPID1)) {
            return dmEntry1.historicalResults();
        }
        return null;
    }

    @Override
    public Optional<SoamId> createDm(
            MdId mdName, MaIdShort maName, MepId mepId, DelayMeasurementCreate dm)
            throws CfmConfigException, SoamConfigException {
        return Optional.ofNullable(SoamId.valueOf(1000));
    }

    @Override
    public void abortDm(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

    }

    @Override
    public void abortDm(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException {

    }

    @Override
    public void clearDelayHistoryStats(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

    }

    @Override
    public void clearDelayHistoryStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId dmId)
            throws CfmConfigException {

    }

    @Override
    public Collection<LossMeasurementEntry> getAllLms(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException, SoamConfigException {
        return null;
    }

    @Override
    public LossMeasurementEntry getLm(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId)
            throws CfmConfigException, SoamConfigException {
        return null;
    }

    @Override
    public LossMeasurementStatCurrent getLmCurrentStat(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId) {
        return null;
    }

    @Override
    public Collection<LossMeasurementStatCurrent> getLmHistoricalStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId) {
        return new ArrayList<LossMeasurementStatCurrent>();
    }

    @Override
    public Optional<SoamId> createLm(
            MdId mdName, MaIdShort maName, MepId mepId, LossMeasurementCreate lm)
            throws CfmConfigException, SoamConfigException {
        return Optional.empty();
    }

    @Override
    public void abortLm(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

    }

    @Override
    public void abortLm(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId)
            throws CfmConfigException {

    }

    @Override
    public void clearLossHistoryStats(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

    }

    @Override
    public void clearLossHistoryStats(
            MdId mdName, MaIdShort maName, MepId mepId, SoamId lmId)
            throws CfmConfigException {

    }

    @Override
    public void createTestSignal(
            MdId mdName, MaIdShort maName, MepId mepId, MepTsCreate tsCreate)
            throws CfmConfigException {

    }

    @Override
    public void abortTestSignal(
            MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException {

    }
}