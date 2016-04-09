/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.utilities;

import com.google.common.base.MoreObjects;

import java.util.Map;
import java.util.Objects;

/**
 * Class that contains the element base key and a map with all the values to
 * set or retrieved with their relative key.
 */
public class YangElement {

    private final String baseKey;
    private final Map<String, String> keysAndValues;

    public YangElement(String baseKey, Map<String, String> keysAndValues) {
        this.baseKey = baseKey;
        this.keysAndValues = keysAndValues;
    }

    public Map<String, String> getKeysAndValues() {
        return keysAndValues;
    }

    public String getBaseKey() {
        return baseKey;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("baseKey", baseKey)
                .add("keysAndValues", keysAndValues)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        YangElement element = (YangElement) o;

        if (baseKey != null ? !baseKey.equals(element.baseKey) : element.baseKey != null) {
            return false;
        }
        return (keysAndValues == null ? element.keysAndValues == null :
                keysAndValues.equals(element.keysAndValues));

    }

    @Override
    public int hashCode() {
        return Objects.hash(baseKey, keysAndValues);
    }
}
