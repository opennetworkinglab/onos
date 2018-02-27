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

package org.onosproject.drivers.microsemi;


import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceNetconfService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter.Unit;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;
import org.onosproject.net.meter.MeterProvider;
import org.onosproject.net.meter.MeterProviderRegistry;
import org.onosproject.net.meter.MeterProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.CosColorType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.PriorityType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.coscolortype.CosColorTypeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.DefaultMefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.MefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.DefaultProfiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.Profiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.BwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.Cos;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.DefaultBwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.DefaultCos;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.Bwp;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.DefaultBwp;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.cos.costypechoice.DefaultEvcCosTypeEvcColorId;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.cos.costypechoice.EvcCosTypeEvcColorId;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.cos.costypechoice.evccostypeevccolorid.DefaultEvcCosTypeAll8PrioTo1EvcColor;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.cos.costypechoice.evccostypeevccolorid.EvcCosTypeAll8PrioTo1EvcColor;
import org.slf4j.Logger;

/**
 * Provider which uses an NETCONF controller to handle meters.
 *
 * TODO: move this to an architecture similar to FlowRuleDriverProvider in order
 * to use a behavior to discover meters.
 */
@Component(immediate = true, enabled = true)
public class EA1000MeterProvider extends AbstractProvider implements MeterProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MseaUniEvcServiceNetconfService mseaUniEvcServiceSvc;

    private MeterProviderService providerService;

    private static final int COS_INDEX_1 = 1;
    private static final short DEFAULT_OUTGOING_PRIO = 3;

    /**
     * Creates a OpenFlow meter provider.
     */
    public EA1000MeterProvider() {
        super(new ProviderId("netconf", "org.onosproject.provider.meter.microsemi"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);

    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);

        providerService = null;
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperations meterOps) {
        log.debug("Adding meterOps to Microsemi Meter Store");
    }

    @Override
    public void performMeterOperation(DeviceId deviceId, MeterOperation meterOp) {
        if (meterOp == null || deviceId == null) {
            log.warn("Missing argument for performMeterOperation()");
            return;
        }
        log.debug("{} meterOp {} to Microsemi Meter Store", meterOp.type(), meterOp);

        long meterId = meterOp.meter().id().id();
        String deviceName = deviceId.uri().getSchemeSpecificPart();
        Unit unit = meterOp.meter().unit();

        Profiles profiles = new DefaultProfiles();
        if (meterOp.type() == MeterOperation.Type.ADD || meterOp.type() == MeterOperation.Type.MODIFY) {
            Bwp bwp = new DefaultBwp();
            bwp.cosIndex(COS_INDEX_1);
            bwp.name("BWP-" + String.valueOf(meterId) + "-" + deviceName);

            long cirRateKbps = 0L;
            long cbsRateKbps = 0L;
            long eirRateKbps = 0L;
            long ebsRateKbps = 0L;
            for (Band band:meterOp.meter().bands()) {
                if (band.type() == Band.Type.REMARK) {
                    //This relates to CIR/CBS
                    cirRateKbps = toBitsPerSec(band.rate(), unit);
                    cbsRateKbps = band.burst(); //Already in kbps
                } else if (band.type() == Band.Type.DROP) {
                    //This relates to EIR/EBS
                    eirRateKbps = toBitsPerSec(band.rate(), unit);
                    ebsRateKbps = band.burst(); //Already in kbps
                }
            }
            bwp.committedInformationRate(cirRateKbps);
            bwp.excessInformationRate(eirRateKbps - cirRateKbps);
            if (meterOp.meter().isBurst()) {
                bwp.committedBurstSize(cbsRateKbps);
                bwp.excessBurstSize(ebsRateKbps - cbsRateKbps);
            }

            BwpGroup bwpg = new DefaultBwpGroup();
            bwpg.groupIndex((short) meterId);
            bwpg.addToBwp(bwp);

            //Create cos-1 as referenced above - we only support 1 at the moment
            Cos cos = new DefaultCos();
            cos.cosIndex(COS_INDEX_1);
            cos.name("COS-1");
            cos.outgoingCosValue(PriorityType.of(DEFAULT_OUTGOING_PRIO));
            cos.colorAware(true);
            cos.colorForward(true);

            EvcCosTypeAll8PrioTo1EvcColor ect =
                    new DefaultEvcCosTypeAll8PrioTo1EvcColor();
            ect.evcAll8ColorTo(CosColorType.of(CosColorTypeEnum.GREEN));
            profiles.addToBwpGroup(bwpg);

            EvcCosTypeEvcColorId cid = new DefaultEvcCosTypeEvcColorId();
            cid.evcCosTypeAll8PrioTo1EvcColor(ect);
            cos.cosTypeChoice(cid);
            profiles.addToCos(cos);
        } else if (meterOp.type() == MeterOperation.Type.REMOVE) {
            BwpGroup bwpg = new DefaultBwpGroup();
            bwpg.groupIndex((short) meterId);

            profiles.addToBwpGroup(bwpg);
        }

        MefServices mefServices = new DefaultMefServices();
        mefServices.profiles(profiles);

        MseaUniEvcServiceOpParam mseaUniEvcServiceFilter = new MseaUniEvcServiceOpParam();
        mseaUniEvcServiceFilter.mefServices(mefServices);

        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();
        try {
            if (meterOp.type() == MeterOperation.Type.REMOVE) {
                mseaUniEvcServiceSvc.deleteMseaUniEvcService(mseaUniEvcServiceFilter,
                        session, DatastoreId.RUNNING);
            } else {
                mseaUniEvcServiceSvc.setMseaUniEvcService(mseaUniEvcServiceFilter,
                        session, DatastoreId.RUNNING);
            }
        } catch (NetconfException e) {
            //This can fail if the BWP Group is deleted before the EVC that is dependent on it
            //The delete of the EVC will be called on a separate thread to that should proceed
            //within a few seconds after which we should try again
            AtomicInteger retry = new AtomicInteger(4);
            if (meterOp.type() == MeterOperation.Type.REMOVE &&
                    e.getMessage().startsWith("Failed to run edit-config through NETCONF")) {
                while (retry.getAndDecrement() > 0) {
                    try {
                        Thread.sleep(1000L);
                        log.debug("Retrying deletion of Bandwith Profile Group {}",
                                String.valueOf(meterId));
                        mseaUniEvcServiceSvc.setMseaUniEvcService(mseaUniEvcServiceFilter,
                                session, DatastoreId.RUNNING);
                        return; //If it did not throw an exception
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        log.debug("Error when deleting BWP profile on EA1000" +
                                " - trying again in 1 sec", e1);
                    } catch (NetconfException e1) {
                        log.debug("NETCONF failed to delete profile - trying again in 1 sec", e1);
                    }
                }
                log.error("Error deleting BWPGroup {} from {} after 4 tries: {}",
                        meterId, deviceId, e.getMessage());
            } else {
                log.error("Error adding BWPGroup {} from {}: {}",
                        meterId, deviceId, e.getMessage());
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private static long toBitsPerSec(long rate, Unit unit) {
        if (unit == Unit.KB_PER_SEC) {
            return rate * 8;
        } else {
            return -1;
        }
    }
}
