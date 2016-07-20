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
package org.onosproject.pcep.api;

/**
 * A interface defined operator type, and provide a method to get the operator
 * type.
 */
public interface PcepOperator {

    enum OperationType {

        ADD, UPDATE, DELETE,
    }

    /**
     * Get operate type of a event,such as device add ,device update.
     *
     * @return operation type.
     */
    OperationType getOperationType();
}
