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
package org.onosproject.incubator.net.l2monitoring.cfm;

import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementStatHistory;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatCurrent;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatHistory;
import org.onosproject.net.NetworkResource;

/**
 * A model of the Maintenance Domain.
 *
 * See IEEE 802.1Q Section 12.14.5.1.3 CFM entities.<br>
 * This is the root of the L2 Monitoring hierarchy<br>
 * |-Maintenance-Domain*<br>
 *   |-{@link MdId}
 *               (MdIdCharStr or MdIdDomainName or MdIdMacUint or MdIdNone)<br>
 *   |-{@link MaintenanceAssociation Maintenance-Association}*<br>
 *     |-{@link MaIdShort}
 *      ({@link MaIdCharStr}
 *      or {@link MaIdPrimaryVid}
 *      or {@link MaId2Octet}
 *      or {@link MaIdRfc2685VpnId}
 *      or {@link MaIdIccY1731})<br>
 *     |-{@link Component}*<br>
 *     |-{@link Mep}* (Maintenance-Association-EndPoint)
 *     and {@link MepEntry}*<br>
 *     |  |-{@link MepId}<br>
 *     |  |-{@link MepLbEntry}<br>
 *     |  |-{@link MepLtEntry}<br>
 *     |  |  |-{@link MepLtTransactionEntry}*<br>
 *     |  |     |-{@link MepLtReply}*<br>
 *     |  |        |-{@link SenderIdTlv}<br>
 *     |  |-{@link DelayMeasurementCreate} (SOAM)*
 *       and {@link DelayMeasurementEntry}<br>
 *     |  |  |-{@link SoamId DmId}<br>
 *     |  |  |-{@link DelayMeasurementStatCurrent}<br>
 *     |  |  |-{@link DelayMeasurementStatHistory}*<br>
 *     |  |-{@link LossMeasurementCreate} (SOAM)*
 *      and {@link LossMeasurementEntry}<br>
 *     |  |  |-{@link SoamId LmId}<br>
 *     |  |  |-{@link LossMeasurementStatCurrent}<br>
 *     |  |  |-{@link LossMeasurementStatHistory}*<br>
 *     |  |-{@link RemoteMepEntry}*<br>
 *     |  |  |-{@link MepId RemoteMepId}<br>
 *     |-{@link MepId RemoteMepId}*<br>
 *<br>
 * *above indicates 0-many can be created
 * -Create suffix means the Object is part of a request
 * -Entry suffix means the Object is part of a reply
 */
public interface MaintenanceDomain extends NetworkResource {
    /**
     * Retrieve the id of the Maintenance Domain.
     * @return The id object
     */
    MdId mdId();

    /**
     * Retrieve the level of the MD.
     * @return An enumerated value for the level
     */
    MdLevel mdLevel();

    /**
     * Retrieve the MA list of the MD.
     * @return The collection of Maintenance Associations
     */
    Collection<MaintenanceAssociation> maintenanceAssociationList();

    /**
     * Replace the MA list of the MD.
     * @param maintenanceAssociationList A list of MAs to replace the existing one
     * @return A new version of the MD with the given MA list
     */
    MaintenanceDomain withMaintenanceAssociationList(
            Collection<MaintenanceAssociation> maintenanceAssociationList);

    /**
     * Numeric identifier.
     * Some systems require to have a placeholder for a numeric identifier in
     * addition to the MdId
     * @return A short numeric id that's been assigned to the MD
     */
    short mdNumericId();

    /**
     * An enumerated set of values to represent MD Level.
     */
    public enum MdLevel {
        LEVEL0, LEVEL1, LEVEL2, LEVEL3, LEVEL4, LEVEL5, LEVEL6, LEVEL7
    }

    /**
     * Builder for {@link MaintenanceDomain}.
     */
    interface MdBuilder {
        MdBuilder mdLevel(MdLevel mdLevel);

        MdBuilder mdNumericId(short mdNumericId);

        MdBuilder addToMaList(MaintenanceAssociation ma);

        MdBuilder deleteFromMaList(MaIdShort maName);

        boolean checkMaExists(MaIdShort maName);

        MaintenanceDomain build() throws CfmConfigException;
    }
}
