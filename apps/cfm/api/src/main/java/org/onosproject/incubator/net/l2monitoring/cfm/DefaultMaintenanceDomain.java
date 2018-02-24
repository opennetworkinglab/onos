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

import java.util.ArrayList;
import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * The default implementation of {@link MaintenanceDomain}.
 */
public final class DefaultMaintenanceDomain implements MaintenanceDomain {

    private final MdId mdId;
    private final MdLevel mdLevel;
    private final Collection<MaintenanceAssociation> maintenanceAssociationList;
    private final short mdNumericId;

    private DefaultMaintenanceDomain(DefaultMdBuilder builder) throws CfmConfigException {
        this.mdId = builder.mdId;
        this.mdLevel = builder.mdLevel;
        this.maintenanceAssociationList = builder.maList;
        this.mdNumericId = builder.mdNumericId;
    }

    private DefaultMaintenanceDomain(DefaultMaintenanceDomain md, Collection<MaintenanceAssociation> maList) {
        this.mdId = md.mdId;
        this.mdLevel = md.mdLevel;
        this.maintenanceAssociationList = maList;
        this.mdNumericId = md.mdNumericId;
    }

    @Override
    public MdId mdId() {
        return mdId;
    }

    @Override
    public MdLevel mdLevel() {
        return mdLevel;
    }

    @Override
    public Collection<MaintenanceAssociation> maintenanceAssociationList() {
        if (maintenanceAssociationList != null) {
            return Lists.newArrayList(maintenanceAssociationList);
        }
        return Lists.newArrayList();
    }

    @Override
    public MaintenanceDomain withMaintenanceAssociationList(
            Collection<MaintenanceAssociation> maintenanceAssociationList) {
        return new DefaultMaintenanceDomain(this, maintenanceAssociationList);
    }

    @Override
    public short mdNumericId() {
        return mdNumericId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mdId == null) ? 0 : mdId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultMaintenanceDomain other = (DefaultMaintenanceDomain) obj;
        if (mdId == null) {
            if (other.mdId != null) {
                return false;
            }
        } else if (!mdId.equals(other.mdId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mdId", mdId.toString())
                .add("level", mdLevel).toString();
    }

    public static MdBuilder builder(MdId mdId)
            throws CfmConfigException {
        return new DefaultMdBuilder(mdId);
    }

    //A method for creating a builder from an existing MD
    public static MdBuilder builder(MaintenanceDomain md)
            throws CfmConfigException {
        return new DefaultMdBuilder(md);
    }


    private static final class DefaultMdBuilder implements MaintenanceDomain.MdBuilder {
        private final MdId mdId;
        private MdLevel mdLevel = MdLevel.LEVEL0;
        private Collection<MaintenanceAssociation> maList;
        private short mdNumericId;

        private DefaultMdBuilder(MdId mdId)
                throws CfmConfigException, NumberFormatException {
            this.mdId = mdId;
            this.maList = new ArrayList<>();
        }

        private DefaultMdBuilder(MaintenanceDomain md)
                throws CfmConfigException, NumberFormatException {
            this.mdId = md.mdId();
            this.mdNumericId = md.mdNumericId();
            this.mdLevel = md.mdLevel();
            this.maList = new ArrayList<>();
            md.maintenanceAssociationList().forEach(ma -> this.maList.add(ma));
        }

        @Override
        public MdBuilder mdLevel(MdLevel mdLevel) {
            this.mdLevel = mdLevel;
            return this;
        }

        @Override
        public MdBuilder addToMaList(MaintenanceAssociation ma) {
            this.maList.add(ma);
            return this;
        }

        @Override
        public MdBuilder deleteFromMaList(MaIdShort maName) {
            this.maList.removeIf(ma -> ma.maId().equals(maName));
            return this;
        }

        @Override
        public boolean checkMaExists(MaIdShort maName) {
            return this.maList.stream().anyMatch(ma -> ma.maId().equals(maName));
        }

        @Override
        public MdBuilder mdNumericId(short mdNumericId) {
            if (mdNumericId <= 0) {
                throw new IllegalArgumentException(
                        "numericId must be > 0. Rejecting " + mdNumericId);
            }
            this.mdNumericId = mdNumericId;
            return this;
        }

        @Override
        public MaintenanceDomain build() throws CfmConfigException {
            return new DefaultMaintenanceDomain(this);
        }
    }
}
