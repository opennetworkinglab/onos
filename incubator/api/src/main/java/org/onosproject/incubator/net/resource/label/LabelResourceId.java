/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.net.resource.ResourceId;

import java.util.Objects;

/**
 * Representation of a label.
 */
@Beta
public final class LabelResourceId implements ResourceId {

    private long labelId;

    public static LabelResourceId labelResourceId(long labelResourceId) {
        return new LabelResourceId(labelResourceId);
    }

    // Public construction is prohibited
    private LabelResourceId(long labelId) {
        this.labelId = labelId;
    }

    public long labelId() {
        return labelId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(labelId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourceId) {
            LabelResourceId that = (LabelResourceId) obj;
            return Objects.equals(this.labelId, that.labelId);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(this.labelId);
    }

}
