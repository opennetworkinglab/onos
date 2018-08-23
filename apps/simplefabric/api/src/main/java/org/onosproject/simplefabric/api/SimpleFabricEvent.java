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

package org.onosproject.simplefabric.api;

import org.onosproject.event.AbstractEvent;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Describes an interface event.
 */
public class SimpleFabricEvent extends AbstractEvent<SimpleFabricEvent.Type, String> {

    public enum Type {
        SIMPLE_FABRIC_UPDATED,  /* Indicates topology info has been updated. */
        SIMPLE_FABRIC_FLUSH,    /* Indicates flush triggered. */
        SIMPLE_FABRIC_IDLE,     /* Indicates idle check. */
        SIMPLE_FABRIC_DUMP      /* Indicates to dump info on the subject to output stream. */
    }

    private PrintStream printStream;  // output stream for SIMPLE_FABRIC_DUMP

    /**
     * Creates an interface event with type and subject.
     *
     * @param type event type
     * @param subject subject for dump event or dummy
     */
    public SimpleFabricEvent(Type type, String subject) {
        super(type, subject);   /* subject is dummy */
    }

    /**
     * Creates an interface event with type, subject and output stream for dump.
     *
     * @param type event type
     * @param subject subject for dump event
     * @param out output stream to dump out
     */
    public SimpleFabricEvent(Type type, String subject, OutputStream out) {
        super(type, subject);   /* subject is dummy */
        printStream = new PrintStream(out, true);
    }

    public PrintStream out() {
        return printStream;
    }

}

