/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.flow;

import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents for 3rd-party private original flow.
 *
 * @deprecated in Junco release
 */
@Deprecated
public final class FlowRuleExtPayLoad {
    private final byte[] payLoad;

    /**
     * private constructor.
     *
     * @param payLoad private flow
     */
    private FlowRuleExtPayLoad(byte[] payLoad) {
        this.payLoad = payLoad;
    }

    /**
     * Creates a FlowRuleExtPayLoad.
     *
     * @param payLoad payload byte data
     * @return FlowRuleExtPayLoad payLoad
     */
    public static FlowRuleExtPayLoad flowRuleExtPayLoad(byte[] payLoad) {
        return new FlowRuleExtPayLoad(payLoad);
    }

    /**
     * Returns private flow.
     *
     * @return payLoad private flow
     */
    public byte[] payLoad() {
        return payLoad;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(payLoad);
    }

    public int hash() {
        return Arrays.hashCode(payLoad);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowRuleExtPayLoad) {
            FlowRuleExtPayLoad that = (FlowRuleExtPayLoad) obj;
            return Arrays.equals(payLoad, that.payLoad);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("payLoad", payLoad).toString();
    }
}
