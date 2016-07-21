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

import io.netty.buffer.ByteBuf;

/**
 * Default LISP map reply message class.
 */
public class DefaultLispMapReply implements LispMapReply {
    @Override
    public LispType getType() {
        return null;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {

    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public boolean isProbe() {
        return false;
    }

    @Override
    public boolean isEtr() {
        return false;
    }

    @Override
    public boolean isSecurity() {
        return false;
    }

    @Override
    public byte getRecordCount() {
        return 0;
    }

    @Override
    public long getNonce() {
        return 0;
    }

    public static final class DefaultReplyBuilder implements ReplyBuilder {

        @Override
        public LispMessage build() {
            return null;
        }

        @Override
        public LispType getType() {
            return null;
        }

        @Override
        public ReplyBuilder withIsProbe(boolean isProbe) {
            return null;
        }

        @Override
        public ReplyBuilder withIsEtr(boolean isEtr) {
            return null;
        }

        @Override
        public ReplyBuilder withIsSecurity(boolean isSecurity) {
            return null;
        }

        @Override
        public ReplyBuilder withRecordCount(byte recordCount) {
            return null;
        }

        @Override
        public ReplyBuilder withNonce(long nonce) {
            return null;
        }
    }
}
