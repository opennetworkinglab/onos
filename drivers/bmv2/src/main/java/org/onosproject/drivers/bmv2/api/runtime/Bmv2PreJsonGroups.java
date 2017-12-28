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
 *
 */

package org.onosproject.drivers.bmv2.api.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents mc group list retrieved from BMv2 PRE.
 */
public final class Bmv2PreJsonGroups {
    public final L1Handle[] l1handles;
    public final L2Handle[] l2handles;
    public final Lag[] lags;
    public final Mgrp[] mgrps;

    @JsonCreator
    public Bmv2PreJsonGroups(@JsonProperty("l1_handles") L1Handle[] l1handles,
                             @JsonProperty("l2_handles") L2Handle[] l2handles,
                             @JsonProperty("lags") Lag[] lags,
                             @JsonProperty("mgrps") Mgrp[] mgrps) {
        this.l1handles = l1handles;
        this.l2handles = l2handles;
        this.lags = lags;
        this.mgrps = mgrps;
    }

    public static final class L1Handle {
        public final int handle;
        public final int l2handle;
        public final int rid;

        @JsonCreator
        public L1Handle(@JsonProperty("handle") int handle,
                        @JsonProperty("l2_handle") int l2handle,
                        @JsonProperty("rid") int rid) {
            this.handle = handle;
            this.l2handle = l2handle;
            this.rid = rid;
        }
    }

    public static final class L2Handle {
        public final int handle;
        public final int[] lags;
        public final int[] ports;

        @JsonCreator
        public L2Handle(@JsonProperty("handle") int handle,
                        @JsonProperty("lags") int[] lags,
                        @JsonProperty("ports") int[] ports) {
            this.handle = handle;
            this.lags = lags;
            this.ports = ports;
        }
    }

    public static final class Lag {
        //lag is not used for now
        @JsonCreator
        public Lag() {
        }
    }

    public static final class Mgrp {
        public final int id;
        public final int[] l1handles;

        @JsonCreator
        public Mgrp(@JsonProperty("id") int id, @JsonProperty("l1_handles") int[] l1handles) {
            this.id = id;
            this.l1handles = l1handles;
        }
    }
}

