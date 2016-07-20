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

package org.onosproject.bgpio.types;

import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGP flow specification type operator and value implementation.
 */
public class BgpFsOperatorValue {

    protected static final Logger log = LoggerFactory.getLogger(BgpFsOperatorValue.class);

    private final byte option;
    private final byte[] value;

    /**
     * constructor to initialize.
     *
     * @param option for a specific flow type
     * @param value for a specific flow type
     */
    public BgpFsOperatorValue(byte option, byte[] value) {
        this.option = option;
        this.value = Arrays.copyOf(value, value.length);
    }

    /**
     * Returns option of the flow type.
     *
     * @return option of the flow type
     */
    public byte option() {
        return option;
    }

    /**
     * Returns value of the flow type.
     *
     * @return value of the flow type
     */
    public byte[] value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(option, Arrays.hashCode(value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsOperatorValue) {
            BgpFsOperatorValue other = (BgpFsOperatorValue) obj;
            return Objects.equals(this.option, other.option) && Arrays.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("option", option).add("value", value).toString();
    }
}
