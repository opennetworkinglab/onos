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
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of dpdk config.
 */
public final class DefaultDpdkConfig implements DpdkConfig {

    private final DatapathType datapathType;
    private final String socketDir;
    private final Collection<DpdkInterface> dpdkIntfs;

    private static final String NOT_NULL_MSG = "% cannot be null";

    private DefaultDpdkConfig(DatapathType datapathType,
                             String socketDir,
                             Collection<DpdkInterface> dpdkIntfs) {
        this.datapathType = datapathType;
        this.socketDir = socketDir;
        this.dpdkIntfs = dpdkIntfs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultDpdkConfig)) {
            return false;
        }
        DefaultDpdkConfig that = (DefaultDpdkConfig) o;
        return datapathType == that.datapathType &&
                Objects.equals(socketDir, that.socketDir) &&
                Objects.equals(dpdkIntfs, that.dpdkIntfs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datapathType,
                socketDir,
                dpdkIntfs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("datapathType", datapathType)
                .add("socketDir", socketDir)
                .add("dpdkIntfs", dpdkIntfs)
                .toString();
    }

    /**
     * Returns the data path type.
     *
     * @return data path type; normal or netdev
     */
    @Override
    public DatapathType datapathType() {
        return datapathType;
    }

    /**
     * Returns socket directory which dpdk port bound to.
     *
     * @return socket directory
     */
    @Override
    public String socketDir() {
        return socketDir;
    }

    /**
     * Returns a collection of dpdk interfaces.
     *
     * @return dpdk interfaces
     */
    @Override
    public Collection<DpdkInterface> dpdkIntfs() {
        if (dpdkIntfs == null) {
            return new ArrayList<>();
        }
        return ImmutableList.copyOf(dpdkIntfs);
    }

    /**
     * Returns new builder instance.
     *
     * @return dpdk config builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of DpdkConfig instance.
     */
    public static final class Builder implements DpdkConfig.Builder {
        private DatapathType datapathType;
        private String socketDir;
        private Collection<DpdkInterface> dpdkIntfs;

        private Builder() {

        }

        @Override
        public DpdkConfig build() {
            checkArgument(datapathType != null, NOT_NULL_MSG, "datapathType");
            return new DefaultDpdkConfig(datapathType,
                    socketDir,
                    dpdkIntfs);
        }

        @Override
        public Builder datapathType(DatapathType datapathType) {
            this.datapathType = datapathType;
            return this;
        }

        @Override
        public Builder socketDir(String socketDir) {
            this.socketDir = socketDir;
            return this;
        }

        @Override
        public Builder dpdkIntfs(Collection<DpdkInterface> dpdkIntfs) {
            this.dpdkIntfs = dpdkIntfs;
            return this;
        }
    }
}
