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

import org.onosproject.yang.gen.v1.mseacfm.rev20160229.mseacfm.mefcfm.maintenancedomain.maintenanceassociation.MaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.AugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.mefcfm.maintenancedomain.maintenanceassociation.maintenanceassociationendpoint.DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.remotemepgroup.RemoteMep;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.mseasoampm.remotemepgroup.remotemep.MepId;

/**
 * This is a workaround for Checkstyle issue.
 * https://github.com/checkstyle/checkstyle/issues/3850
 *
 */
public final class MepIdUtil {
    private MepIdUtil() {
        //Hidden
    }

    public static MepId convertRemoteMepId(RemoteMep rmep) {
        return (MepId) rmep;
    }

    public static AugmentedMseaCfmMaintenanceAssociationEndPoint
        convertPmAugmentedMep(MaintenanceAssociationEndPoint mep) {
        AugmentedMseaCfmMaintenanceAssociationEndPoint augmentedMep =
            (AugmentedMseaCfmMaintenanceAssociationEndPoint) mep
            .augmentation(
                    DefaultAugmentedMseaCfmMaintenanceAssociationEndPoint.class);
        return augmentedMep;
    }
}
