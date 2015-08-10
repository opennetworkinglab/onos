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
