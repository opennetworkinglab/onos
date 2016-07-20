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
package org.onosproject.flowapi;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extended traffic rate implementation.
 */
public class DefaultExtTrafficRate implements ExtTrafficRate {

    private Short asn;
    private Float rate;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTrafficRate which contains traffic rate action.
     *
     * @param asn is a AS number
     * @param rate is a traffic rate in bytes per second
     * @param type ExtType type
     */
    DefaultExtTrafficRate(short asn, float rate, ExtType type) {
        this.asn = Short.valueOf(asn);
        this.rate = Float.valueOf(rate);
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public Short asn() {
        return asn;
    }

    @Override
    public Float rate() {
        return rate;
    }

    @Override
    public boolean exactMatch(ExtTrafficRate value) {
        return this.equals(value) &&
                Objects.equals(this.asn, value.asn())
                && Objects.equals(this.rate, value.rate())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(asn, rate, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTrafficRate) {
            DefaultExtTrafficRate that = (DefaultExtTrafficRate) obj;
            return Objects.equals(asn, that.asn())
                    && Objects.equals(this.rate, that.rate())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("asn", asn.toString())
                .add("rate", rate.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extended traffic rate rule.
     */
    public static class Builder implements ExtTrafficRate.Builder {
        private Short asn;
        private Float rate;
        private ExtType type;

        @Override
        public Builder setAsn(short asn) {
            this.asn = Short.valueOf(asn);
            return this;
        }

        @Override
        public Builder setRate(float rate) {
            this.rate = Float.valueOf(rate);
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTrafficRate build() {
            checkNotNull(asn, "asn cannot be null");
            checkNotNull(rate, "rate cannot be null");
            return new DefaultExtTrafficRate(asn, rate, type);
        }
    }
}
