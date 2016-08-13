/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Label as an ElementType.
 */
public class Label implements ElementType {
    private final long value;

    /**
     * Creates an instance of Label.
     *
     * @param label label value
     */
    public Label(long label) {
        this.value = label;
    }

    /**
     * Returns the label.
     *
     * @return value of the label
     */
    public long label() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Label) {
            Label other = (Label) obj;
            return
                 Objects.equals(value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("value", value)
            .toString();
    }
}
