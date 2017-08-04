/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Partition event.
 */
//TODO change String into a proper object type
@Beta
public class WorkPartitionEvent extends AbstractEvent<WorkPartitionEvent.Type, String> {

    public enum Type {
        LEADER_CHANGED
    }

    public WorkPartitionEvent(Type type, String partition) {
        super(type, partition);
    }
}
