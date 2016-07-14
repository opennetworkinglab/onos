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

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.net.PortNumber;

/**
 * Representation of policy group bucket identifier. Not exposed to
 * the application and only to be used internally.
 */
public class GroupBucketIdentifier {
    private int label;
    private BucketOutputType type;
    private PortNumber outPort;
    private PolicyGroupIdentifier outGroup;

    protected enum BucketOutputType {
        PORT,
        GROUP
    }

    protected GroupBucketIdentifier(int label,
                                    PortNumber outPort) {
        this.label = label;
        this.type = BucketOutputType.PORT;
        this.outPort = checkNotNull(outPort);
        this.outGroup = null;
    }

    protected GroupBucketIdentifier(int label,
                                    PolicyGroupIdentifier outGroup) {
        this.label = label;
        this.type = BucketOutputType.GROUP;
        this.outPort = null;
        this.outGroup = checkNotNull(outGroup);
    }

    protected int label() {
        return this.label;
    }

    protected BucketOutputType type() {
        return this.type;
    }

    protected PortNumber outPort() {
        return this.outPort;
    }

    protected PolicyGroupIdentifier outGroup() {
        return this.outGroup;
    }
}

