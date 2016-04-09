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
package org.onosproject.segmentrouting.grouphandler;

import java.util.List;

/**
 * Representation of policy based group identifiers.
 * Opaque to group handler applications and only the outermost
 * policy group identifier in a chain is visible to the applications.
 */
public class PolicyGroupIdentifier {
    private String id;
    private List<PolicyGroupParams> inputParams;
    private List<GroupBucketIdentifier> bucketIds;

    /**
     * Constructs policy group identifier.
     *
     * @param id unique identifier associated with the policy group
     * @param input policy group params associated with this group
     * @param bucketIds buckets associated with this group
     */
    protected PolicyGroupIdentifier(String id,
                                 List<PolicyGroupParams> input,
                                 List<GroupBucketIdentifier> bucketIds) {
        this.id = id;
        this.inputParams = input;
        this.bucketIds = bucketIds;
    }

    /**
     * Returns the bucket identifier list associated with the policy
     * group identifier.
     *
     * @return list of bucket identifier
     */
    protected List<GroupBucketIdentifier> bucketIds() {
        return this.bucketIds;
    }

    @Override
    public int hashCode() {
        int result = 17;
        int combinedHash = 0;
        for (PolicyGroupParams input:inputParams) {
            combinedHash = combinedHash + input.hashCode();
        }
        for (GroupBucketIdentifier bucketId:bucketIds) {
            combinedHash = combinedHash + bucketId.hashCode();
        }
        result = 31 * result + combinedHash;

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PolicyGroupIdentifier) {
            PolicyGroupIdentifier that = (PolicyGroupIdentifier) obj;
            boolean result = this.id.equals(that.id);
            result = result &&
                    this.inputParams.containsAll(that.inputParams) &&
                    that.inputParams.containsAll(this.inputParams);
            result = result &&
                    this.bucketIds.containsAll(that.bucketIds) &&
                    that.bucketIds.containsAll(this.bucketIds);
            return result;
        }

        return false;
    }
}
