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

package org.onosproject.xmpp.core.ctl.exception;

/**
 * Exception is thrown when the validation of XMPP packet fails.
 */
public class XmppValidationException extends Exception {

    private boolean streamValidation;

    public XmppValidationException(boolean streamValidation) {
        this.streamValidation = streamValidation;
    }

    public boolean isStreamValidationException() {
        return streamValidation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        XmppValidationException that = (XmppValidationException) o;

        return streamValidation == that.streamValidation;
    }

    @Override
    public int hashCode() {
        return (streamValidation ? 1 : 0);
    }
}
