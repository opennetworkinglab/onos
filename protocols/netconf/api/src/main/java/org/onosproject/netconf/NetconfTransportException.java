/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.netconf;

/**
 * Exception triggered from NETCONF secure transport layer or below.
 */
public class NetconfTransportException extends RuntimeException {

    private static final long serialVersionUID = 5788096975954688094L;

    public NetconfTransportException() {
    }

    /**
     * @param message describing the error
     */
    public NetconfTransportException(String message) {
        super(message);
    }

    /**
     * @param cause of this exception
     */
    public NetconfTransportException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message describing the error
     * @param cause of this exception
     */
    public NetconfTransportException(String message, Throwable cause) {
        super(message, cause);
    }

}
