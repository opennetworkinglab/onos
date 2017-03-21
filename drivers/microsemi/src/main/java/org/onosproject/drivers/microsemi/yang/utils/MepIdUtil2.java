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
package org.onosproject.drivers.microsemi.yang.utils;

import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.transmitloopback.transmitloopbackinput.TargetAddress;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.MepId;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.targetaddressgroup.addresstype.MacAddress;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.mseasoamfm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;

/**
 * This is a workaround for Checkstyle issue.
 * https://github.com/checkstyle/checkstyle/issues/3850
 *
 */
public final class MepIdUtil2 {
    private MepIdUtil2() {
        //Hidden
    }

    public static MepId convertTargetAddrToMepId(TargetAddress targetMep) {
        return (MepId) targetMep.addressType();
    }

    public static MacAddress convertTargetAddrToMacAddress(TargetAddress targetMep) {
        return (MacAddress) targetMep.addressType();
    }

    public static AugmentedMseaCfmMaintenanceAssociationEndPoint
        convertFmAugmentedMep(MaintenanceAssociationEndPoint mep) {
        return (AugmentedMseaCfmMaintenanceAssociationEndPoint) mep
            .augmentation(DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);
    }
}
