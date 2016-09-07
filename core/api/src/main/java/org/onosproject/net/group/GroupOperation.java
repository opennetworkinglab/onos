/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.group;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.core.GroupId;

/**
 * Group operation definition to be used between core and provider
 * layers of group subsystem.
 *
 */
public final class GroupOperation {
    private final Type opType;
    private final GroupId groupId;
    private final GroupDescription.Type groupType;
    private final GroupBuckets buckets;
    private final GroupMsgErrorCode failureCode;

    public enum Type {
        /**
         * Create a group in a device with the specified parameters.
         */
        ADD,
        /**
         * Modify a group in a device with the specified parameters.
         */
        MODIFY,
        /**
         * Delete a specified group.
         */
        DELETE
    }

    /**
     * Possible error codes for a failure of a group operation.
     *
     */
    public enum GroupMsgErrorCode {
        GROUP_EXISTS,
        INVALID_GROUP,
        WEIGHT_UNSUPPORTED,
        OUT_OF_GROUPS,
        OUT_OF_BUCKETS,
        CHAINING_UNSUPPORTED,
        WATCH_UNSUPPORTED,
        LOOP,
        UNKNOWN_GROUP,
        CHAINED_GROUP,
        BAD_TYPE,
        BAD_COMMAND,
        BAD_BUCKET,
        BAD_WATCH,
        EPERM
    }

    /**
     * Group operation constructor with the parameters.
     *
     * @param opType group operation type
     * @param groupId group Identifier
     * @param groupType type of the group
     * @param buckets immutable list of group buckets to be part of group
     */
    private GroupOperation(Type opType,
                           GroupId groupId,
                           GroupDescription.Type groupType,
                           GroupBuckets buckets) {
        this.opType = checkNotNull(opType);
        this.groupId = checkNotNull(groupId);
        this.groupType = checkNotNull(groupType);
        this.buckets = buckets;
        this.failureCode = null;
    }

    /**
     * Group operation copy-constructor with additional field to set failure code.
     * Typically used by provider to return information to the core about
     * the failure reason for a group operation.
     *
     * @param groupOp the original group operation
     * @param failureCode failure code for a failed group operation
     */
    private GroupOperation(GroupOperation groupOp, GroupMsgErrorCode failureCode) {
        this.opType = groupOp.opType;
        this.groupId = groupOp.groupId;
        this.groupType = groupOp.groupType;
        this.buckets = groupOp.buckets;
        this.failureCode = failureCode;
    }

    /**
     * Creates ADD group operation object.
     *
     * @param groupId group Identifier
     * @param groupType type of the group
     * @param buckets immutable list of group buckets to be part of group
     * @return add group operation object
     */
    public static GroupOperation createAddGroupOperation(GroupId groupId,
                                     GroupDescription.Type groupType,
                                     GroupBuckets buckets) {
        checkNotNull(buckets);
        return new GroupOperation(Type.ADD, groupId, groupType, buckets);
    }

    /**
     * Creates MODIFY group operation object.
     *
     * @param groupId group Identifier
     * @param groupType type of the group
     * @param buckets immutable list of group buckets to be part of group
     * @return modify group operation object
     */
    public static GroupOperation createModifyGroupOperation(GroupId groupId,
                               GroupDescription.Type groupType,
                               GroupBuckets buckets) {
        checkNotNull(buckets);
        return new GroupOperation(Type.MODIFY, groupId, groupType, buckets);

    }

    /**
     * Creates DELETE group operation object.
     *
     * @param groupId group Identifier
     * @param groupType type of the group
     * @return delete group operation object
     */
    public static GroupOperation createDeleteGroupOperation(GroupId groupId,
                                  GroupDescription.Type groupType) {
        return new GroupOperation(Type.DELETE, groupId, groupType, null);

    }

    /**
     * Creates failure group operation object by setting failure code
     * to inform the failure reason.
     *
     * @param groupOperation the original group operation
     * @param failureCode failure code for a failed group operation
     * @return failed group operation object
     */
    public static GroupOperation createFailedGroupOperation(GroupOperation groupOperation,
                                                            GroupMsgErrorCode failureCode) {
        checkNotNull(groupOperation);
        return new GroupOperation(groupOperation, failureCode);
    }

    /**
     * Returns group operation type.
     *
     * @return GroupOpType group operation type
     */
    public Type opType() {
        return this.opType;
    }

    /**
     * Returns group identifier attribute of the operation.
     *
     * @return GroupId group identifier
     */
    public GroupId groupId() {
        return this.groupId;
    }

    /**
     * Returns group type attribute of the operation.
     *
     * @return GroupType group type
     */
    public GroupDescription.Type groupType() {
        return this.groupType;
    }

    /**
     * Returns group buckets associated with the operation.
     *
     * @return GroupBuckets group buckets
     */
    public GroupBuckets buckets() {
        return this.buckets;
    }

    /**
     * Returns the failure code representing the failure of a group operation.
     *
     * @return error code for failure of group operation
     */
    public GroupMsgErrorCode failureCode() {
        return failureCode;
    }

    @Override
    /*
     * The deviceId, type and buckets are used for hash.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int hashCode() {
        return (buckets != null) ? Objects.hash(groupId, opType, buckets) :
            Objects.hash(groupId, opType);
    }

    @Override
    /*
     * The deviceId, type and buckets should be same.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
       if (obj instanceof GroupOperation) {
           GroupOperation that = (GroupOperation) obj;
           return Objects.equals(groupId, that.groupId) &&
                   Objects.equals(groupType, that.groupType) &&
                   Objects.equals(opType, that.opType) &&
                   Objects.equals(buckets, that.buckets);

        }
        return false;
    }
}
