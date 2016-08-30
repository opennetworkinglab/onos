/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yms.ydt;

/**
 * Abstraction of an entity which represents YANG management system operation
 * result. Once the protocol translates the request information into a abstract
 * YANG data tree, it uses YANG management system as a broker to get the
 * operation executed in ONOS. Protocols uses a request Yang management
 * system to delegate the operation request.
 *
 * YANG management system is responsible to split the protocol operation
 * across application(s) which needs to participate, and collate the
 * response(s) from application(s) and return an effective result of the
 * operation request. The status of the operation execution is returned.
 */
public enum YmsOperationExecutionStatus {

    /**
     * Successful execution of the operation.
     */
    EXECUTION_SUCCESS,

    /**
     * Exception in execution of the operation.
     */
    EXECUTION_EXCEPTION,

    /**
     * Error in execution of the operation.
     */
    ERROR_EXCEPTION
}
