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
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * The default implementation of {@link MaintenanceAssociation}.
 */
public final class DefaultMaintenanceAssociation implements MaintenanceAssociation {

    private MaIdShort maId;
    private final CcmInterval ccmInterval;
    private final Collection<Component> componentList;
    private final Collection<MepId> remoteMepIdList;
    private final short maNumericId;

    private DefaultMaintenanceAssociation(DefaultMaBuilder builder)
            throws CfmConfigException {
        this.maId = builder.maId;
        this.ccmInterval = builder.ccmInterval;
        this.componentList = builder.componentList;
        this.remoteMepIdList = builder.remoteMepIdList;
        this.maNumericId = builder.maNumericId;
    }

    private DefaultMaintenanceAssociation(MaintenanceAssociation ma,
                                          AttributeName attrName, Object attrValue) {
        this.maId = ma.maId();
        this.ccmInterval = ma.ccmInterval();

        if (attrName == AttributeName.COMPONENTLIST) {
            this.componentList = (Collection<Component>) attrValue;
        } else {
            this.componentList = ma.componentList();
        }

        if (attrName == AttributeName.REMOTEMEPIDLIST) {
            this.remoteMepIdList = (Collection<MepId>) attrValue;
        } else {
            this.remoteMepIdList = ma.remoteMepIdList();
        }

        this.maNumericId = ma.maNumericId();
    }

    @Override
    public MaIdShort maId() {
        return maId;
    }

    @Override
    public CcmInterval ccmInterval() {
        return ccmInterval;
    }

    @Override
    public Collection<Component> componentList() {
        if (componentList != null) {
            return Lists.newArrayList(componentList);
        }
        return Lists.newArrayList();
    }

    @Override
    public MaintenanceAssociation withComponentList(Collection<Component> componentList) {
        return new DefaultMaintenanceAssociation(this, AttributeName.COMPONENTLIST, componentList);
    }

    @Override
    public Collection<MepId> remoteMepIdList() {
        if (remoteMepIdList != null) {
            return Lists.newArrayList(remoteMepIdList);
        }
        return Lists.newArrayList();
    }

    @Override
    public MaintenanceAssociation withRemoteMepIdList(Collection<MepId> remoteMepIdList) {
        return new DefaultMaintenanceAssociation(this, AttributeName.REMOTEMEPIDLIST, remoteMepIdList);
    }

    @Override
    public short maNumericId() {
        return maNumericId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((ccmInterval == null) ? 0 : ccmInterval.hashCode());
        result = prime * result
                + ((componentList == null) ? 0 : componentList.hashCode());
        result = prime * result + ((maId == null) ? 0 : maId.hashCode());
        result = prime * result
                + ((remoteMepIdList == null) ? 0 : remoteMepIdList.hashCode());
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
        DefaultMaintenanceAssociation other = (DefaultMaintenanceAssociation) obj;
        if (ccmInterval != other.ccmInterval) {
            return false;
        }
        if (componentList == null) {
            if (other.componentList != null) {
                return false;
            }
        } else if (!componentList.equals(other.componentList)) {
            return false;
        }
        if (!maId.equals(other.maId)) {
            return false;
        }
        if (remoteMepIdList == null) {
            if (other.remoteMepIdList != null) {
                return false;
            }
        } else if (!remoteMepIdList.equals(other.remoteMepIdList)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("maId", maId).toString();
    }

    public static MaBuilder builder(MaIdShort maId, int mdNameAndTypeLen)
            throws CfmConfigException {
        return new DefaultMaBuilder(maId, mdNameAndTypeLen);
    }

    public static MaBuilder builder(MaintenanceAssociation ma)
            throws CfmConfigException {
        return new DefaultMaBuilder(ma);
    }

    private static final class DefaultMaBuilder implements MaintenanceAssociation.MaBuilder {

        private final MaIdShort maId;
        private CcmInterval ccmInterval;
        private Collection<Component> componentList;
        private Collection<MepId> remoteMepIdList;
        private short maNumericId;

        public DefaultMaBuilder(MaIdShort maId, int mdNameAndTypeLen)
                throws CfmConfigException {
            if (maId.getNameLength() + mdNameAndTypeLen > 48) {
                throw new CfmConfigException("Combined length of MD name and "
                        + "MA name exceeds 48. Cannot create");
            }
            this.maId = maId;
            this.componentList = new ArrayList<>();
            this.remoteMepIdList = new ArrayList<>();
        }

        public DefaultMaBuilder(MaintenanceAssociation ma)
                throws CfmConfigException {

            this.maId = ma.maId();
            this.maNumericId = ma.maNumericId();
            this.componentList = new ArrayList<>();
            ma.componentList().forEach(comp -> this.componentList.add(comp));
            this.remoteMepIdList = new ArrayList<>();
            ma.remoteMepIdList().forEach((rmep -> this.remoteMepIdList.add(rmep)));
        }

        @Override
        public MaBuilder addToComponentList(Component component) {
            this.componentList.add(component);
            return this;
        }

        @Override
        public MaBuilder addToRemoteMepIdList(MepId remoteMep) {
            this.remoteMepIdList.add(remoteMep);
            return this;
        }

        @Override
        public MaBuilder removeFromRemoteMepIdList(MepId remoteMep) {
            this.remoteMepIdList.remove(remoteMep);
            return this;
        }

        @Override
        public MaBuilder ccmInterval(CcmInterval ccmInterval) {
            this.ccmInterval = ccmInterval;
            return this;
        }

        @Override
        public MaBuilder maNumericId(short maNumericId) {
            if (maNumericId <= 0) {
                throw new IllegalArgumentException(
                        "numericId must be > 0. Rejecting " + maNumericId);
            }
            this.maNumericId = maNumericId;
            return this;
        }

        @Override
        public MaintenanceAssociation build() throws CfmConfigException {
            return new DefaultMaintenanceAssociation(this);
        }
    }

    private enum AttributeName {
        REMOTEMEPIDLIST,
        COMPONENTLIST
    }

}
