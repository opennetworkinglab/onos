/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.workflow.api;


/**
 * Static convenience class that help a whether it was invoked correctly(whether its preconditions have been met).
 * Methods of this class generally accept a boolean expression which is expected to be true.
 * When false (or null) is passed instead, the Preconditions method throws an workflow exception.
 */
public final class CheckCondition {

    /**
     * Private class of check condition.
     */
    private CheckCondition() {
    }

    /**
     * Checks the condition, and if it is false, it raises workflow exception with exception message.
     * @param condition condition to check. A boolean expression is located on here.
     * @param exceptionMessage exception message for workflow exception
     * @throws WorkflowException workflow exception
     */
    public static void check(boolean condition, String exceptionMessage) throws WorkflowException {
        if (!condition) {
            throw new WorkflowException(exceptionMessage);
        }
    }

}
