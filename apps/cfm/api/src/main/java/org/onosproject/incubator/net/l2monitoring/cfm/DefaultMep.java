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

import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * The default implementation of {@link Mep}.
 */
public class DefaultMep implements Mep {

    protected final MepId mepId;
    protected final DeviceId deviceId;
    protected final PortNumber port;
    protected final MepDirection direction;
    protected final MdId mdId;
    protected final MaIdShort maId;
    protected final VlanId primaryVid;
    protected final boolean administrativeState;
    protected final boolean cciEnabled;
    protected final Priority ccmLtmPriority;
    protected final FngAddress fngAddress;
    protected final LowestFaultDefect lowestFaultPriorityDefect;
    protected final Duration defectPresentTime;
    protected final Duration defectAbsentTime;

    protected DefaultMep(DefaultMepBuilder mepBuilder) {
        this.mepId = mepBuilder.mepId;
        this.deviceId = mepBuilder.deviceId;
        this.port = mepBuilder.port;
        this.direction = mepBuilder.direction;
        this.mdId = mepBuilder.mdId;
        this.maId = mepBuilder.maId;
        this.primaryVid = mepBuilder.primaryVid;
        this.administrativeState = mepBuilder.administrativeState;
        this.cciEnabled = mepBuilder.cciEnabled;
        this.ccmLtmPriority = mepBuilder.ccmLtmPriority;
        this.fngAddress = mepBuilder.fngAddress;
        this.lowestFaultPriorityDefect = mepBuilder.lowestFaultPriorityDefect;
        this.defectPresentTime = mepBuilder.defectPresentTime;
        this.defectAbsentTime = mepBuilder.defectAbsentTime;
    }

    protected DefaultMep(DefaultMep mep, AttributeName attrName, Object attrValue) {
        this.mepId = mep.mepId;
        this.deviceId = mep.deviceId;
        this.port = mep.port;
        this.direction = mep.direction;
        this.mdId = mep.mdId;
        this.maId = mep.maId;
        if (attrName == AttributeName.PRIMARY_VID) {
            this.primaryVid = (VlanId) attrValue;
        } else {
            this.primaryVid = mep.primaryVid;
        }
        if (attrName == AttributeName.ADMINISTRATIVE_STATE) {
            this.administrativeState = (boolean) attrValue;
        } else {
            this.administrativeState = mep.administrativeState;
        }

        if (attrName == AttributeName.CCI_ENABLED) {
            this.cciEnabled = (boolean) attrValue;
        } else {
            this.cciEnabled = mep.cciEnabled;
        }

        if (attrName == AttributeName.CCM_LTM_PRIORITY) {
            this.ccmLtmPriority = (Priority) attrValue;
        } else {
            this.ccmLtmPriority = mep.ccmLtmPriority;
        }

        if (attrName == AttributeName.FNG_ADDRESS) {
            this.fngAddress = (FngAddress) attrValue;
        } else {
            this.fngAddress = mep.fngAddress;
        }

        if (attrName == AttributeName.LOWEST_FAULT_PRIORITY_DEFECT) {
            this.lowestFaultPriorityDefect = (LowestFaultDefect) attrValue;
        } else {
            this.lowestFaultPriorityDefect = mep.lowestFaultPriorityDefect;
        }

        if (attrName == AttributeName.DEFECT_PRESENT_TIME) {
            this.defectPresentTime = (Duration) attrValue;
        } else {
            this.defectPresentTime = mep.defectPresentTime;
        }

        if (attrName == AttributeName.DEFECT_ABSENT_TIME) {
            this.defectAbsentTime = (Duration) attrValue;
        } else {
            this.defectAbsentTime = mep.defectAbsentTime;
        }
    }

    @Override
    public MepId mepId() {
        return mepId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public PortNumber port() {
        return port;
    }

    @Override
    public MepDirection direction() {
        return direction;
    }

    @Override
    public MdId mdId() {
        return mdId;
    }

    @Override
    public MaIdShort maId() {
        return maId;
    }

    @Override
    public VlanId primaryVid() {
        return primaryVid;
    }

    @Override
    public Mep withPrimaryVid(VlanId primaryVid) {
        return new DefaultMep(this, AttributeName.PRIMARY_VID, primaryVid);
    }

    @Override
    public boolean administrativeState() {
        return administrativeState;
    }

    @Override
    public Mep withAdministrativeState(boolean adminState) {
        return new DefaultMep(this, AttributeName.ADMINISTRATIVE_STATE, adminState);
    }

    @Override
    public Boolean cciEnabled() {
        return cciEnabled;
    }

    @Override
    public Mep withCciEnabled(boolean cciEnabled) {
        return new DefaultMep(this, AttributeName.CCI_ENABLED, cciEnabled);
    }

    @Override
    public Priority ccmLtmPriority() {
        return ccmLtmPriority;
    }

    @Override
    public Mep withCcmLtmPriority(Priority priority) {
        return new DefaultMep(this, AttributeName.CCM_LTM_PRIORITY, priority);
    }

    @Override
    public Mep withFngAddress(FngAddress fngAddress) {
        return new DefaultMep(this, AttributeName.FNG_ADDRESS, fngAddress);
    }

    @Override
    public FngAddress fngAddress() {
        return fngAddress;
    }

    @Override
    public LowestFaultDefect lowestFaultPriorityDefect() {
        return lowestFaultPriorityDefect;
    }

    @Override
    public Mep withLowestFaultPriorityDefect(LowestFaultDefect lowestFdType) {
        return new DefaultMep(this, AttributeName.LOWEST_FAULT_PRIORITY_DEFECT, lowestFdType);
    }

    @Override
    public Duration defectPresentTime() {
        return defectPresentTime;
    }

    @Override
    public Mep withDefectPresentTime(Duration duration) {
        return new DefaultMep(this, AttributeName.DEFECT_PRESENT_TIME, duration);
    }

    @Override
    public Duration defectAbsentTime() {
        return defectAbsentTime;
    }

    @Override
    public Mep withDefectAbsentTime(Duration duration) {
        return new DefaultMep(this, AttributeName.DEFECT_ABSENT_TIME, duration);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultMep that = (DefaultMep) o;

        if (administrativeState != that.administrativeState) {
            return false;
        }
        if (cciEnabled != that.cciEnabled) {
            return false;
        }
        if (!mepId.equals(that.mepId)) {
            return false;
        }
        if (!deviceId.equals(that.deviceId)) {
            return false;
        }
        if (!port.equals(that.port)) {
            return false;
        }
        if (direction != that.direction) {
            return false;
        }
        if (!mdId.equals(that.mdId)) {
            return false;
        }
        if (!maId.equals(that.maId)) {
            return false;
        }
        if (primaryVid != null ? !primaryVid.equals(that.primaryVid) : that.primaryVid != null) {
            return false;
        }
        if (ccmLtmPriority != that.ccmLtmPriority) {
            return false;
        }
        if (fngAddress != null ? !fngAddress.equals(that.fngAddress) : that.fngAddress != null) {
            return false;
        }
        if (lowestFaultPriorityDefect != that.lowestFaultPriorityDefect) {
            return false;
        }
        if (defectPresentTime != null ?
                !defectPresentTime.equals(that.defectPresentTime) : that.defectPresentTime != null) {
            return false;
        }
        return defectAbsentTime != null ?
                defectAbsentTime.equals(that.defectAbsentTime) : that.defectAbsentTime == null;
    }

    @Override
    public int hashCode() {
        int result = mepId.hashCode();
        result = 31 * result + deviceId.hashCode();
        result = 31 * result + port.hashCode();
        result = 31 * result + (direction != null ? direction.hashCode() : 0);
        result = 31 * result + mdId.hashCode();
        result = 31 * result + maId.hashCode();
        result = 31 * result + (primaryVid != null ? primaryVid.hashCode() : 0);
        result = 31 * result + (administrativeState ? 1 : 0);
        result = 31 * result + (cciEnabled ? 1 : 0);
        result = 31 * result + (ccmLtmPriority != null ? ccmLtmPriority.hashCode() : 0);
        result = 31 * result + (fngAddress != null ? fngAddress.hashCode() : 0);
        result = 31 * result + (lowestFaultPriorityDefect != null ? lowestFaultPriorityDefect.hashCode() : 0);
        result = 31 * result + (defectPresentTime != null ? defectPresentTime.hashCode() : 0);
        result = 31 * result + (defectAbsentTime != null ? defectAbsentTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DefaultMep{" +
                "mepId=" + mepId +
                ", deviceId=" + deviceId +
                ", port=" + port +
                ", direction=" + direction +
                ", mdId=" + mdId +
                ", maId=" + maId +
                ", primaryVid=" + primaryVid +
                ", administrativeState=" + administrativeState +
                ", cciEnabled=" + cciEnabled +
                ", ccmLtmPriority=" + ccmLtmPriority +
                ", fngAddress=" + fngAddress +
                ", lowestFaultPriorityDefect=" + lowestFaultPriorityDefect +
                ", defectPresentTime=" + defectPresentTime +
                ", defectAbsentTime=" + defectAbsentTime +
                '}';
    }

    public static MepBuilder builder(
            MepId mepId,
            DeviceId deviceId,
            PortNumber port,
            MepDirection direction,
            MdId mdId,
            MaIdShort maId)
            throws CfmConfigException {
        return new DefaultMepBuilder(mepId, deviceId, port, direction, mdId, maId);
    }

    /**
     * Builder for {@link Mep}.
     */
    protected static class DefaultMepBuilder implements Mep.MepBuilder {
        protected final MepId mepId;
        protected final DeviceId deviceId;
        protected final PortNumber port;
        protected final MepDirection direction;
        protected final MdId mdId;
        protected final MaIdShort maId;
        protected VlanId primaryVid;
        protected boolean administrativeState;
        protected boolean cciEnabled;
        protected Priority ccmLtmPriority;
        protected FngAddress fngAddress;
        protected LowestFaultDefect lowestFaultPriorityDefect;
        protected Duration defectPresentTime;
        protected Duration defectAbsentTime;

        protected DefaultMepBuilder(MepId mepId, DeviceId deviceId, PortNumber port,
                MepDirection direction, MdId mdId, MaIdShort maId)
                throws CfmConfigException {
            if (port.isLogical()) {
                throw new CfmConfigException("Port must be physical. Rejecting; " + port.toString());
            } else if (mepId == null) {
                throw new CfmConfigException("MepId is null");
            } else if (mdId == null) {
                throw new CfmConfigException("MdId is null");
            } else if (maId == null) {
                throw new CfmConfigException("MaId is null");
            }
            this.mepId = mepId;
            this.deviceId = deviceId;
            this.port = port;
            this.direction = direction;
            this.mdId = mdId;
            this.maId = maId;
        }

        public DefaultMepBuilder(Mep mep) {
            this.mepId = mep.mepId();
            this.deviceId = mep.deviceId();
            this.port = mep.port();
            this.direction = mep.direction();
            this.mdId = mep.mdId();
            this.maId = mep.maId();
            this.primaryVid = mep.primaryVid();
            this.administrativeState = mep.administrativeState();
            this.cciEnabled = mep.cciEnabled();
            this.ccmLtmPriority = mep.ccmLtmPriority();
            this.fngAddress = mep.fngAddress();
            this.lowestFaultPriorityDefect = mep.lowestFaultPriorityDefect();
            this.defectPresentTime = mep.defectPresentTime();
            this.defectAbsentTime = mep.defectAbsentTime();
        }

        @Override
        public MepBuilder primaryVid(VlanId primaryVid) {
            this.primaryVid = primaryVid;
            return this;
        }

        @Override
        public MepBuilder administrativeState(boolean administrativeState) {
            this.administrativeState = administrativeState;
            return this;
        }

        @Override
        public MepBuilder cciEnabled(boolean cciEnabled) {
            this.cciEnabled = cciEnabled;
            return this;
        }

        @Override
        public MepBuilder ccmLtmPriority(Priority ccmLtmPriority) {
            this.ccmLtmPriority = ccmLtmPriority;
            return this;
        }

        @Override
        public MepBuilder fngAddress(FngAddress fngAddress) {
            this.fngAddress = fngAddress;
            return this;
        }

        @Override
        public MepBuilder lowestFaultPriorityDefect(
                LowestFaultDefect lowestFaultPriorityDefect) {
            this.lowestFaultPriorityDefect = lowestFaultPriorityDefect;
            return this;
        }

        @Override
        public MepBuilder defectPresentTime(Duration defectPresentTime) {
            this.defectPresentTime = defectPresentTime;
            return this;
        }

        @Override
        public MepBuilder defectAbsentTime(Duration defectAbsentTime) {
            this.defectAbsentTime = defectAbsentTime;
            return this;
        }

        @Override
        public Mep build() throws CfmConfigException {
            return new DefaultMep(this);
        }
    }

    private enum AttributeName {
        PRIMARY_VID,
        ADMINISTRATIVE_STATE,
        CCI_ENABLED,
        CCM_LTM_PRIORITY,
        FNG_ADDRESS,
        LOWEST_FAULT_PRIORITY_DEFECT,
        DEFECT_PRESENT_TIME,
        DEFECT_ABSENT_TIME;
    }
}
