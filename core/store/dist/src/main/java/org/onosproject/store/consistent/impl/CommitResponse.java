/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Result of a Transaction commit operation.
 */
public final class CommitResponse {

    private boolean success;
    private List<UpdateResult<String, byte[]>> updates;

    public static CommitResponse success(List<UpdateResult<String, byte[]>> updates) {
        return new CommitResponse(true, updates);
    }

    public static CommitResponse failure() {
        return new CommitResponse(false, Collections.emptyList());
    }

    private CommitResponse(boolean success, List<UpdateResult<String, byte[]>> updates) {
        this.success = success;
        this.updates = ImmutableList.copyOf(updates);
    }

    public boolean success() {
        return success;
    }

    public List<UpdateResult<String, byte[]>> updates() {
        return updates;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("success", success)
                .add("udpates", updates)
                .toString();
    }
}
