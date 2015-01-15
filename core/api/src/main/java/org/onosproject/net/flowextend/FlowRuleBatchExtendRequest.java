/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flowextend;

import java.util.Collection;
public class FlowRuleBatchExtendRequest {

    private final int batchId;
    private final Collection<FlowRuleExtendEntry> toAdd;

    public FlowRuleBatchExtendRequest(int batchId, Collection<FlowRuleExtendEntry> toAdd) {
        this.batchId = batchId;
        this.toAdd = toAdd;
    }

    public Collection<FlowRuleExtendEntry> getBatch(){
        return toAdd;
    }
    public int batchId() {
        return batchId;
    }
}
