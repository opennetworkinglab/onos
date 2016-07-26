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
package org.onosproject.lisp.msg.protocols;

import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * Default implementation of LispMapRecord.
 */
public class DefaultLispMapRecord implements LispMapRecord {

    private int recordTtl;
    private int locatorCount;
    private byte maskLength;
    private LispMapReplyAction action;
    private boolean authoritative;
    private short mapVersionNumber;
    private LispAfiAddress eidPrefixAfi;

    public int getRecordTtl() {
        return recordTtl;
    }

    public int getLocatorCount() {
        return locatorCount;
    }

    public byte getMaskLength() {
        return maskLength;
    }

    public LispMapReplyAction getAction() {
        return action;
    }

    public boolean isAuthoritative() {
        return authoritative;
    }

    public short getMapVersionNumber() {
        return mapVersionNumber;
    }

    public LispAfiAddress getEidPrefixAfi() {
        return eidPrefixAfi;
    }

    public static final class DefaultMapRecordBuilder implements MapRecordBuilder {

        @Override
        public MapRecordBuilder withRecordTtl(int recordTtl) {
            return null;
        }

        @Override
        public MapRecordBuilder withLocatorCount(int locatorCount) {
            return null;
        }

        @Override
        public MapRecordBuilder withMaskLength(byte maskLength) {
            return null;
        }

        @Override
        public MapRecordBuilder withAction(LispMapReplyAction action) {
            return null;
        }

        @Override
        public MapRecordBuilder withAuthoritative(boolean authoritative) {
            return null;
        }

        @Override
        public MapRecordBuilder withMapVersionNumber(short mapVersionNumber) {
            return null;
        }

        @Override
        public MapRecordBuilder withEidPrefixAfi(LispAfiAddress prefix) {
            return null;
        }
    }
}
