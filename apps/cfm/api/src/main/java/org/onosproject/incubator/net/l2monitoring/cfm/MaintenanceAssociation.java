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

import java.time.Duration;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.NetworkResource;

/**
 * A model of the Maintenance Association.
 *
 * See IEEE 802.1Q Section 12.14 CFM entities
 * Direct child of {@link MaintenanceDomain}
 */
public interface MaintenanceAssociation extends NetworkResource {
    /**
     * Get the ID of the Maintenance Association.
     * @return The id object
     */
    MaIdShort maId();

    /**
     * Get the CCM interval for the Maintenance Association.
     * @return An enumerated value
     */
    CcmInterval ccmInterval();

    /**
     * A list of components each of which can be managed in a manner essentially equivalent to an 802.1Q bridge.
     * @return A collection of Components
     */
    Collection<Component> componentList();

    /**
     * Create a new component collection.
     * @param componentList A collection of component objects
     * @return A new Maintenance Association
     */
    MaintenanceAssociation withComponentList(Collection<Component> componentList);

    /**
     * Get the list of the Remote Mep Ids.
     * @return A list of Remote Mep Ids
     */
    Collection<MepId> remoteMepIdList();

    /**
     * Create a new Maintenance Association from the current with the specified RemoteMepId list.
     * @param remoteMepIdList A list of RemoteMepIds
     * @return A new Maintenance Association
     */
    MaintenanceAssociation withRemoteMepIdList(Collection<MepId> remoteMepIdList);

    /**
     * Numeric identifier.
     * Some systems require to have a placeholder for a numeric identifier in
     * addition to the MaId
     * @return A short numeric id that's been assigned to the MA
     */
    short maNumericId();

    /**
     * Enumerated values from IEEE 802.1Q Table 21-16—CCM Interval field encoding.
     */
    enum CcmInterval {
        INVALID(0),
        INTERVAL_3MS(3),
        INTERVAL_10MS(10),
        INTERVAL_100MS(100),
        INTERVAL_1S(1000),
        INTERVAL_10S(10000),
        INTERVAL_1MIN(60000),
        INTERVAL_10MIN(600000);

        private final int millis;

        CcmInterval(int millis) {
            this.millis = millis;
        }

        public int millis() {
            return millis;
        }

        public Duration duration() {
            return Duration.ofMillis(millis);
        }
    }

    /**
     * Builder for {@link MaintenanceAssociation}.
     */
    interface MaBuilder {

        MaBuilder ccmInterval(CcmInterval ccmInterval);

        MaBuilder addToRemoteMepIdList(MepId remoteMep);

        MaBuilder removeFromRemoteMepIdList(MepId remoteMep);

        MaBuilder addToComponentList(Component component);

        MaBuilder maNumericId(short maNumericId);

        MaintenanceAssociation build() throws CfmConfigException;
    }
}
