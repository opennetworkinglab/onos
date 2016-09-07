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
 * Abstraction of an entity which contains context specific error info.
 */
public interface YdtErrorInfo {
    /**
     * Retrieves the application specific error tag corresponding to the
     * error context in operation.
     *
     * @return application specific error tag corresponding to the error
     * context in operation
     */
    String getErrorAppTag();

    /**
     * Retrieves the error message corresponding to the error context in
     * operation.
     *
     * @return the error message corresponding to the error context in operation
     */
    String getErrorMessage();
}
