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
package org.onosproject.openflow.controller.driver;

/**
 * Base class for exception thrown by switch driver sub-handshake processing.
 *
 */
public class SwitchDriverSubHandshakeException extends RuntimeException {
    private static final long serialVersionUID = -6257836781419604438L;

    protected SwitchDriverSubHandshakeException() {
        super();
    }

    protected SwitchDriverSubHandshakeException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    protected SwitchDriverSubHandshakeException(String arg0) {
        super(arg0);
    }

    protected SwitchDriverSubHandshakeException(Throwable arg0) {
        super(arg0);
    }

}
