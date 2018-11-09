/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.tunnel;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of label stack.
 */
public class DefaultLabelStack implements LabelStack {

    private final List<LabelResourceId> labelResources;

    /**
     * Creates label stack.
     *
     * @param labelResources contiguous label ids that comprise the path
     */
    public DefaultLabelStack(List<LabelResourceId> labelResources) {
        this.labelResources = ImmutableList.copyOf(labelResources);
    }

    @Override
    public List<LabelResourceId> labelResources() {
        return labelResources;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("labelResources", labelResources)
                .toString();
    }

    @Override
    public int hashCode() {
        return labelResources.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultLabelStack) {
            final DefaultLabelStack other = (DefaultLabelStack) obj;
            return Objects.equals(this.labelResources, other.labelResources);
        }
        return false;
    }
}
