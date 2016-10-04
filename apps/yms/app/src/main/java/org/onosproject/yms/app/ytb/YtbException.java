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

package org.onosproject.yms.app.ytb;

/**
 * Represents exception that needs to be handled by YTB.
 */
public class YtbException extends RuntimeException {

    /**
     * Creates YTB exception with an exception message.
     *
     * @param exceptionMessage message with which exception must be thrown
     */
    public YtbException(String exceptionMessage) {
        super(exceptionMessage);
    }

    /**
     * Creates YTB exception with the cause for it.
     *
     * @param cause cause of the exception
     */
    public YtbException(Throwable cause) {
        super(cause);
    }
}
