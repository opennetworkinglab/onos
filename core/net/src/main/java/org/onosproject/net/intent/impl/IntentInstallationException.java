/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent.impl;

import org.onosproject.net.intent.IntentException;

/**
 * An exception thrown when intent installation fails.
 */
public class IntentInstallationException extends IntentException {
    private static final long serialVersionUID = 3720268258616014168L;

    public IntentInstallationException() {
        super();
    }

    public IntentInstallationException(String message) {
        super(message);
    }

    public IntentInstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
