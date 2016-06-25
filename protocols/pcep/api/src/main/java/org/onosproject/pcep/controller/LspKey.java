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
package org.onosproject.pcep.controller;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of LSP info, it will be unique for each LSP.
 */
public class LspKey {
    private int plspId;
    private short localLspId;

    /**
     * Creates new instance of LspInfo.
     *
     * @param plspId LSP id assigned per tunnel per session
     * @param localLspId LSP id assigned per tunnel
     */
    public LspKey(int plspId, short localLspId) {
        this.plspId = plspId;
        this.localLspId = localLspId;
    }

    /**
     * Obtains PLSP id.
     *
     * @return LSP id assigned per tunnel per session
     */
    public int plspId() {
        return plspId;
    }

    /**
     * Obtains local LSP id.
     *
     * @return LSP id assigned per tunnel
     */
    public short localLspId() {
        return localLspId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(plspId, localLspId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LspKey) {
            LspKey other = (LspKey) obj;
            return Objects.equals(plspId, other.plspId)
                    && Objects.equals(localLspId, other.localLspId);
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("plspId", plspId)
                .add("localLspId", localLspId)
                .toString();
    }
}