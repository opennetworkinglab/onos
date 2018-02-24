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

import com.google.common.collect.Lists;
import org.onlab.packet.VlanId;

/**
 * The default implementation of {@link Component}.
 */
public final class DefaultComponent implements Component {

    private final int componentId;
    private final Collection<VlanId> vidList;
    private final MhfCreationType mhfCreationType;
    private final IdPermissionType idPermission;
    private final TagType tagType;

    private DefaultComponent(DefaultComponentBuilder builder) {
        this.componentId = builder.componentId;
        this.vidList = builder.vidList;
        this.mhfCreationType = builder.mhfCreationType;
        this.idPermission = builder.idPermission;
        this.tagType = builder.tagType;
    }

    @Override
    public int componentId() {
        return componentId;
    }

    @Override
    public Collection<VlanId> vidList() {
        if (vidList != null) {
            return Lists.newArrayList(vidList);
        } else {
            return null;
        }
    }

    @Override
    public MhfCreationType mhfCreationType() {
        return mhfCreationType;
    }

    @Override
    public IdPermissionType idPermission() {
        return idPermission;
    }

    @Override
    public TagType tagType() {
        return tagType;
    }



    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + componentId;
        result = prime * result
                + ((idPermission == null) ? 0 : idPermission.hashCode());
        result = prime * result
                + ((mhfCreationType == null) ? 0 : mhfCreationType.hashCode());
        result = prime * result + ((tagType == null) ? 0 : tagType.hashCode());
        result = prime * result + ((vidList == null) ? 0 : vidList.hashCode());
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
        DefaultComponent other = (DefaultComponent) obj;
        if (componentId != other.componentId) {
            return false;
        }
        if (idPermission != other.idPermission) {
            return false;
        }
        if (mhfCreationType != other.mhfCreationType) {
            return false;
        }
        if (tagType != other.tagType) {
            return false;
        }
        if (vidList == null) {
            if (other.vidList != null) {
                return false;
            }
        } else if (!vidList.equals(other.vidList)) {
            return false;
        }
        return true;
    }

    public static ComponentBuilder builder(int componentId) {
        return new DefaultComponentBuilder(componentId);
    }

    private static final class DefaultComponentBuilder implements ComponentBuilder {
        private final int componentId;
        private Collection<VlanId> vidList;
        private MhfCreationType mhfCreationType;
        private IdPermissionType idPermission;
        private TagType tagType;

        private DefaultComponentBuilder(int componentId) {
            this.componentId = componentId;
            vidList = new ArrayList<>();
        }

        @Override
        public ComponentBuilder addToVidList(VlanId vid) {
            this.vidList.add(vid);
            return this;
        }

        @Override
        public ComponentBuilder mhfCreationType(MhfCreationType mhfCreationType) {
            this.mhfCreationType = mhfCreationType;
            return this;
        }

        @Override
        public ComponentBuilder idPermission(IdPermissionType idPermission) {
            this.idPermission = idPermission;
            return this;
        }

        @Override
        public ComponentBuilder tagType(TagType tagType) {
            this.tagType = tagType;
            return this;
        }

        public Component build() {
            return new DefaultComponent(this);
        }
    }
}
