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
 * Represents YANG management system results. Protocols sends request to
 * YANG management system for execution. YMS returns response in form of
 * YANG data tree response
 */
public interface YdtResponse extends Ydt {

    /**
     * Returns YANG management system operation result. This status of the
     * operation execution is returned
     *
     * @return YMS operation result
     */
    YmsOperationExecutionStatus getYmsOperationResult();

    /*
     * TODO: Applications layer error reporting to protocols.
     */
}
