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
 * Extended traffic redirect implementation.
 */
public class DefaultExtTrafficRedirect implements ExtTrafficRedirect {

    private String redirect;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTrafficRedirect which contains traffic redirect action.
     *
     * @param redirect is a redirect rule
     * @param type ExtType type
     */
    DefaultExtTrafficRedirect(String redirect, ExtType type) {
        this.redirect = redirect;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    @Override
    public String redirect() {
        return redirect;
    }

    @Override
    public boolean exactMatch(ExtTrafficRedirect value) {
        return this.equals(value) &&
                Objects.equals(this.redirect, value.redirect())
                && Objects.equals(this.type, value.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(redirect, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTrafficRedirect) {
            DefaultExtTrafficRedirect that = (DefaultExtTrafficRedirect) obj;
            return Objects.equals(redirect, that.redirect())
                    && Objects.equals(this.type, that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("redirect", redirect.toString())
                .add("type", type.toString())
                .toString();
    }

    /**
     * Builder class for extended redirect value rule.
     */
    public static class Builder implements ExtTrafficRedirect.Builder {
        private String redirect;
        private ExtType type;

        @Override
        public Builder setRedirect(String redirect) {
            this.redirect = redirect;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTrafficRedirect build() {
            checkNotNull(redirect, "redirect cannot be null");
            return new DefaultExtTrafficRedirect(redirect, type);
        }
    }
}
