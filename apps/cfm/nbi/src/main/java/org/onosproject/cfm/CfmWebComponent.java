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
package org.onosproject.cfm;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;
import org.onosproject.cfm.web.ComponentCodec;
import org.onosproject.cfm.web.FngAddressCodec;
import org.onosproject.cfm.web.MaintenanceAssociationCodec;
import org.onosproject.cfm.web.MaintenanceDomainCodec;
import org.onosproject.cfm.web.MepCodec;
import org.onosproject.cfm.web.MepEntryCodec;
import org.onosproject.cfm.web.MepLbCreateCodec;
import org.onosproject.cfm.web.MepLbEntryCodec;
import org.onosproject.cfm.web.MepLtCreateCodec;
import org.onosproject.cfm.web.RemoteMepEntryCodec;
import org.onosproject.cfm.web.VidCodec;
import org.onosproject.codec.CodecService;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.FngAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.RemoteMepEntry;
import org.onosproject.incubator.net.l2monitoring.soam.StartTime;
import org.onosproject.incubator.net.l2monitoring.soam.StopTime;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStat;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStat;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossAvailabilityStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate.CounterOption;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStat;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementThreshold;
import org.onosproject.soam.web.DelayMeasurementStatCodec;
import org.onosproject.soam.web.DelayMeasurementStatCurrentCodec;
import org.onosproject.soam.web.DelayMeasurementStatHistoryCodec;
import org.onosproject.soam.web.DmCreateCodec;
import org.onosproject.soam.web.DmEntryCodec;
import org.onosproject.soam.web.DmMeasurementOptionCodec;
import org.onosproject.soam.web.LmCounterOptionCodec;
import org.onosproject.soam.web.LmCreateCodec;
import org.onosproject.soam.web.LmEntryCodec;
import org.onosproject.soam.web.LmThresholdOptionCodec;
import org.onosproject.soam.web.LossAvailabilityStatCodec;
import org.onosproject.soam.web.LossAvailabilityStatCurrentCodec;
import org.onosproject.soam.web.LossAvailabilityStatHistoryCodec;
import org.onosproject.soam.web.LossMeasurementStatCodec;
import org.onosproject.soam.web.LossMeasurementStatCurrentCodec;
import org.onosproject.soam.web.LossMeasurementStatHistoryCodec;
import org.onosproject.soam.web.LossMeasurementThresholdCodec;
import org.onosproject.soam.web.StartTimeCodec;
import org.onosproject.soam.web.StopTimeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables the CFM REST Web Service component at /onos/cfm.
 * Each codec for the rest interfaces should be registered here.
 */
@Component(immediate = true)
public class CfmWebComponent {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    /**
     * On activation of this component register these codecs with the codec service.
     */
    @Activate
    public void activate() {
        codecService.registerCodec(MaintenanceDomain.class,
                                        new MaintenanceDomainCodec());
        codecService.registerCodec(MaintenanceAssociation.class,
                                        new MaintenanceAssociationCodec());
        codecService.registerCodec(org.onosproject.incubator.net.l2monitoring.cfm.Component.class,
                                        new ComponentCodec());
        codecService.registerCodec(VlanId.class, new VidCodec());
        codecService.registerCodec(Mep.class, new MepCodec());
        codecService.registerCodec(MepEntry.class, new MepEntryCodec());
        codecService.registerCodec(MepLbCreate.class, new MepLbCreateCodec());
        codecService.registerCodec(MepLbEntry.class, new MepLbEntryCodec());
        codecService.registerCodec(MepLtCreate.class, new MepLtCreateCodec());
        codecService.registerCodec(RemoteMepEntry.class, new RemoteMepEntryCodec());
        codecService.registerCodec(FngAddress.class, new FngAddressCodec());


        codecService.registerCodec(DelayMeasurementCreate.class,
                                        new DmCreateCodec());
        codecService.registerCodec(DelayMeasurementEntry.class,
                                        new DmEntryCodec());
        codecService.registerCodec(DelayMeasurementStat.class,
                                        new DelayMeasurementStatCodec());
        codecService.registerCodec(DelayMeasurementStatCurrent.class,
                                        new DelayMeasurementStatCurrentCodec());
        codecService.registerCodec(DelayMeasurementStatHistory.class,
                                        new DelayMeasurementStatHistoryCodec());
        codecService.registerCodec(MeasurementOption.class,
                                        new DmMeasurementOptionCodec());

        codecService.registerCodec(LossMeasurementCreate.class,
                                        new LmCreateCodec());
        codecService.registerCodec(LossMeasurementThreshold.class,
                                        new LossMeasurementThresholdCodec());
        codecService.registerCodec(LossMeasurementEntry.class,
                                        new LmEntryCodec());
        codecService.registerCodec(LossMeasurementStat.class,
                                        new LossMeasurementStatCodec());
        codecService.registerCodec(LossMeasurementStatCurrent.class,
                                        new LossMeasurementStatCurrentCodec());
        codecService.registerCodec(LossMeasurementStatHistory.class,
                                        new LossMeasurementStatHistoryCodec());
        codecService.registerCodec(LossAvailabilityStat.class,
                                        new LossAvailabilityStatCodec());
        codecService.registerCodec(LossAvailabilityStatCurrent.class,
                                        new LossAvailabilityStatCurrentCodec());
        codecService.registerCodec(LossAvailabilityStatHistory.class,
                                        new LossAvailabilityStatHistoryCodec());
        codecService.registerCodec(CounterOption.class,
                                        new LmCounterOptionCodec());
        codecService.registerCodec(LossMeasurementThreshold.ThresholdOption.class,
                                        new LmThresholdOptionCodec());

        codecService.registerCodec(StartTime.class, new StartTimeCodec());
        codecService.registerCodec(StopTime.class, new StopTimeCodec());

        log.info("CFM Web Component Started");
    }

    /**
     * On deactivation of this component unregister these codecs from the codec service.
     */
    @Deactivate
    public void deactivate() {
        log.info("CFM Web Component Stopped");
        codecService.unregisterCodec(MaintenanceDomain.class);
        codecService.unregisterCodec(MaintenanceAssociation.class);
        codecService.unregisterCodec(org.onosproject.incubator.net.l2monitoring.cfm.Component.class);
        codecService.unregisterCodec(VlanId.class);
        codecService.unregisterCodec(Mep.class);
        codecService.unregisterCodec(MepEntry.class);
        codecService.unregisterCodec(MepLbCreate.class);
        codecService.unregisterCodec(MepLbEntry.class);
        codecService.unregisterCodec(MepLtCreate.class);
        codecService.unregisterCodec(RemoteMepEntry.class);
        codecService.unregisterCodec(FngAddress.class);

        codecService.unregisterCodec(DelayMeasurementCreate.class);
        codecService.unregisterCodec(DelayMeasurementEntry.class);
        codecService.unregisterCodec(DelayMeasurementStat.class);
        codecService.unregisterCodec(DelayMeasurementStatCurrent.class);
        codecService.unregisterCodec(DelayMeasurementStatHistory.class);
        codecService.unregisterCodec(MeasurementOption.class);
        codecService.unregisterCodec(StartTime.class);
        codecService.unregisterCodec(StopTime.class);

        codecService.unregisterCodec(LossMeasurementCreate.class);
        codecService.unregisterCodec(LossMeasurementThreshold.class);
        codecService.unregisterCodec(LossMeasurementEntry.class);
        codecService.unregisterCodec(LossMeasurementStat.class);
        codecService.unregisterCodec(LossMeasurementStatCurrent.class);
        codecService.unregisterCodec(LossMeasurementStatHistory.class);
        codecService.unregisterCodec(LossAvailabilityStat.class);
        codecService.unregisterCodec(LossAvailabilityStatCurrent.class);
        codecService.unregisterCodec(LossAvailabilityStatHistory.class);
        codecService.unregisterCodec(CounterOption.class);
        codecService.unregisterCodec(LossMeasurementThreshold.ThresholdOption.class);

    }
}
