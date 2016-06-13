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
 * Extended traffic action implementation.
 */
public class DefaultExtTrafficAction implements ExtTrafficAction {

    private boolean terminal;
    private boolean sample;
    private boolean rpd;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTrafficAction which contains traffic action flag.
     *
     * @param terminal traffic action terminal bit
     * @param sample is a traffic action sampling
     * @param rpd traffic action rpd bit
     * @param type ExtType type
     */
    DefaultExtTrafficAction(boolean terminal, boolean sample, boolean rpd, ExtType type) {
        this.terminal = terminal;
        this.sample = sample;
        this.rpd = rpd;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public boolean terminal() {
        return terminal;
    }

    @Override
    public boolean sample() {
        return sample;
    }

    @Override
    public boolean rpd() {
        return rpd;
    }

    @Override
    public boolean exactMatch(ExtTrafficAction value) {
        return this.equals(value) &&
                Objects.equals(this.terminal, value.terminal())
                && Objects.equals(this.sample, value.sample())
                && Objects.equals(this.rpd, value.rpd())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminal, sample, rpd, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTrafficAction) {
            DefaultExtTrafficAction that = (DefaultExtTrafficAction) obj;
            return Objects.equals(terminal, that.terminal())
                    && Objects.equals(this.sample, that.sample())
                    && Objects.equals(this.rpd, that.rpd())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("terminal", Boolean.valueOf(terminal).toString())
                .add("sample",  Boolean.valueOf(sample).toString())
                .add("rpd",  Boolean.valueOf(rpd).toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extended traffic rate rule.
     */
    public static class Builder implements ExtTrafficAction.Builder {
        private boolean terminal;
        private boolean sample;
        private boolean rpd;
        private ExtType type;

        @Override
        public Builder setTerminal(boolean terminal) {
            this.terminal = terminal;
            return this;
        }

        @Override
        public Builder setSample(boolean sample) {
            this.sample = sample;
            return this;
        }

        @Override
        public Builder setRpd(boolean rpd) {
            this.rpd = rpd;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTrafficAction build() {
            checkNotNull(terminal, "terminal cannot be null");
            checkNotNull(sample, "sample cannot be null");
            checkNotNull(rpd, "rpd cannot be null");
            return new DefaultExtTrafficAction(terminal, sample, rpd, type);
        }
    }
}
