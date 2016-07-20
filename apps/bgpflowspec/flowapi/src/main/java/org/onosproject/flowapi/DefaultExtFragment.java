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

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fragment extension implementation.
 */
public class DefaultExtFragment implements ExtFragment {

    private List<ExtOperatorValue> fragment;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtFragment which contains fragment operator value list.
     *
     * @param fragment is a fragment value rule list
     * @param type ExtType type
     */
    DefaultExtFragment(List<ExtOperatorValue> fragment, ExtType type) {
        this.fragment = fragment;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public List<ExtOperatorValue> fragment() {
        return fragment;
    }

    @Override
    public boolean exactMatch(ExtFragment value) {
        return this.equals(value) &&
                Objects.equals(this.fragment, value.fragment())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fragment, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtFragment) {
            DefaultExtFragment that = (DefaultExtFragment) obj;
            return Objects.equals(fragment, that.fragment())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("fragment", fragment.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extension fragment value.
     */
    public static class Builder implements ExtFragment.Builder {
        private List<ExtOperatorValue> fragment;
        private ExtType type;

        @Override
        public Builder setFragment(List<ExtOperatorValue> fragment) {
            this.fragment = fragment;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtFragment build() {
            checkNotNull(fragment, "fragment cannot be null");
            return new DefaultExtFragment(fragment, type);
        }
    }
}
