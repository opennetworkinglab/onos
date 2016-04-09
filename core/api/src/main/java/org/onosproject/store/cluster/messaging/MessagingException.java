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
package org.onosproject.store.cluster.messaging;

import java.io.IOException;

/**
 * Top level exception for MessagingService failures.
 */
@SuppressWarnings("serial")
public class MessagingException extends IOException {

    public MessagingException() {
    }

    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable t) {
        super(message, t);
    }

    public MessagingException(Throwable t) {
        super(t);
    }

    /**
     * Exception indicating no remote registered remote handler.
     */
    public static class NoRemoteHandler extends MessagingException {
    }

    /**
     * Exception indicating handler failure.
     */
    public static class RemoteHandlerFailure extends MessagingException {
    }

    /**
     * Exception indicating failure due to invalid message strucuture such as an incorrect preamble.
     */
    public static class ProcotolException extends MessagingException {
    }
}