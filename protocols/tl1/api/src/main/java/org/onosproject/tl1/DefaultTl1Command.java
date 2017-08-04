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
package org.onosproject.tl1;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a TL1 command.
 */
public final class DefaultTl1Command implements Tl1Command {
    private static final char HYPHEN = '-';
    private static final char COLON = ':';
    private static final char SEMICOLON = ';';

    private String verb;
    private String modifier;
    private Optional<String> tid;
    private String aid;
    private int ctag;
    private String parameters;

    private DefaultTl1Command(String verb, String modifier, String tid, String aid, int ctag, String parameters) {
        this.verb = verb;
        this.modifier = modifier;
        this.tid = Optional.ofNullable(tid);
        this.aid = aid;
        this.ctag = ctag;
        this.parameters = parameters;
    }

    @Override
    public String verb() {
        return verb;
    }

    @Override
    public String modifier() {
        return modifier;
    }

    @Override
    public Optional<String> tid() {
        return tid;
    }

    @Override
    public Optional<String> aid() {
        return Optional.ofNullable(aid);
    }

    @Override
    public int ctag() {
        return ctag;
    }

    @Override
    public Optional<String> parameters() {
        return Optional.ofNullable(parameters);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append(verb).append(HYPHEN)
                .append(modifier).append(COLON)
                .append(tid().orElse("")).append(COLON)
                .append(aid().orElse("")).append(COLON)
                .append(ctag);

        if (parameters().isPresent()) {
            sb.append(COLON).append(COLON).append(parameters);
        }

        return sb.append(SEMICOLON).toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements Tl1Command.Builder {
        private String verb;
        private String modifier;
        private String tid;
        private String aid;
        private int ctag;
        private String parameters;

        @Override
        public Tl1Command.Builder withVerb(String verb) {
            this.verb = verb;
            return this;
        }

        @Override
        public Tl1Command.Builder withModifier(String modifier) {
            this.modifier = modifier;
            return this;
        }

        @Override
        public Tl1Command.Builder forTid(String tid) {
            this.tid = tid;
            return this;
        }

        @Override
        public Tl1Command.Builder withAid(String aid) {
            this.aid = aid;
            return this;
        }

        @Override
        public Tl1Command.Builder withCtag(int ctag) {
            this.ctag = ctag;
            return this;
        }

        @Override
        public Tl1Command.Builder withParameters(String parameters) {
            this.parameters = parameters;
            return this;
        }

        @Override
        public Tl1Command build() {
            checkNotNull(verb, "Must supply a verb");
            checkNotNull(modifier, "Must supply a modifier");

            checkArgument(MIN_CTAG < ctag, "ctag cannot be less than " + MIN_CTAG);
            checkArgument(ctag <= MAX_CTAG, "ctag cannot be larger than " + MAX_CTAG);

            return new DefaultTl1Command(verb, modifier, tid, aid, ctag, parameters);
        }
    }
}
