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
package org.onosproject.net.flowext;

import java.util.Collection;
public class FlowRuleBatchExtRequest {

    private final int batchId;
    /*
     * Concern all the entry as to add, because the bytes contains the information
     * of to-add or to-delete, 
     */   
    private final Collection<FlowRuleExtEntry> toAdd;

    public FlowRuleBatchExtRequest(int batchId, Collection<FlowRuleExtEntry> toAdd) {
        this.batchId = batchId;
        this.toAdd = toAdd;
    }

    public Collection<FlowRuleExtEntry> getBatch(){
        return toAdd;
    }
    public int batchId() {
        return batchId;
    }
}
