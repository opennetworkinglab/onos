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

package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation class of neutron config.
 */
public final class DefaultNeutronConfig implements NeutronConfig {

    private final boolean useMetadataProxy;
    private final String metadataProxySecret;

    private static final String NOT_NULL_MSG = "% cannot be null";

    private DefaultNeutronConfig(boolean useMetadataProxy, String metadataProxySecret) {
        this.useMetadataProxy = useMetadataProxy;
        this.metadataProxySecret = metadataProxySecret;
    }

    @Override
    public boolean useMetadataProxy() {
        return useMetadataProxy;
    }

    @Override
    public String metadataProxySecret() {
        return metadataProxySecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof DefaultNeutronConfig) {
            DefaultNeutronConfig that = (DefaultNeutronConfig) o;
            return Objects.equals(useMetadataProxy, that.useMetadataProxy) &&
                    Objects.equals(metadataProxySecret, that.metadataProxySecret);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return Objects.hash(useMetadataProxy, metadataProxySecret);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("useMetadataProxy", useMetadataProxy)
                .add("metadataProxySecret", metadataProxySecret)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return neutron config instance builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for neutron config.
     */
    public static final class Builder implements NeutronConfig.Builder {

        private boolean useMetadataProxy;
        private String metadataProxySecret;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public NeutronConfig build() {
            checkArgument(metadataProxySecret != null,
                                        NOT_NULL_MSG, "metadataProxySecret");

            return new DefaultNeutronConfig(useMetadataProxy, metadataProxySecret);
        }

        @Override
        public NeutronConfig.Builder useMetadataProxy(boolean useMetadataProxy) {
            this.useMetadataProxy = useMetadataProxy;
            return this;
        }

        @Override
        public NeutronConfig.Builder metadataProxySecret(String metadataProxySecret) {
            this.metadataProxySecret = metadataProxySecret;
            return this;
        }
    }
}
