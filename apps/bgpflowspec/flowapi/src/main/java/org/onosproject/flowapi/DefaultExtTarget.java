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
package org.onosproject.flowapi;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Target local and remote speaker implementation for wide community.
 */
public final class DefaultExtTarget implements ExtTarget {

    private ExtPrefix localSpeaker;
    private ExtPrefix remoteSpeaker;
    private ExtType type;

    /**
     * Creates an object of type DefaultExtTarget which contains local and remote speakers.
     *
     * @param localSpeaker local speaker prefix list
     * @param remoteSpeaker remoteSpeaker speaker prefix list
     * @param type ExtType type
     */
    DefaultExtTarget(ExtPrefix localSpeaker, ExtPrefix remoteSpeaker, ExtType type) {
        this.localSpeaker = localSpeaker;
        this.remoteSpeaker = remoteSpeaker;
        this.type = type;
    }

    @Override
    public ExtType type() {
        return type;
    }

    /**
     * Returns the local speaker prefix list.
     *
     * @return the ExtPrefix
     */
    @Override
    public ExtPrefix localSpeaker() {
        return localSpeaker;
    }

    /**
     * Returns the remote speaker prefix list.
     *
     * @return the ExtPrefix
     */
    @Override
    public ExtPrefix remoteSpeaker() {
        return remoteSpeaker;
    }

    /**
     * Returns whether this target is an exact match to the target given
     * in the argument.
     *
     * @param target other target to match
     * @return true if the target are an exact match, otherwise false
     */
    @Override
    public boolean exactMatch(ExtTarget target) {
        return this.equals(target) &&
                Objects.equals(this.localSpeaker, target.localSpeaker())
                && Objects.equals(this.remoteSpeaker, target.remoteSpeaker())
                && Objects.equals(this.type, target.type());
    }

    @Override
    public int hashCode() {
        return Objects.hash(localSpeaker, remoteSpeaker, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultExtTarget) {
            DefaultExtTarget that = (DefaultExtTarget) obj;
            return Objects.equals(localSpeaker, that.localSpeaker())
                    && Objects.equals(remoteSpeaker, that.remoteSpeaker())
                    && Objects.equals(this.type, that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("localSpeaker", localSpeaker)
                .add("remoteSpeaker", remoteSpeaker)
                .add("type", type)
                .toString();
    }

    /**
     * Builder class for wide community target.
     */
    public static class Builder implements ExtTarget.Builder {
        private ExtPrefix localSpeaker;
        private ExtPrefix remoteSpeaker;
        private ExtType type;

        @Override
        public Builder setLocalSpeaker(ExtPrefix localSpeaker) {
            this.localSpeaker = localSpeaker;
            return this;
        }

        @Override
        public Builder setRemoteSpeaker(ExtPrefix remoteSpeaker) {
            this.remoteSpeaker = remoteSpeaker;
            return this;
        }

        @Override
        public Builder setType(ExtType type) {
            this.type = type;
            return this;
        }

        @Override
        public ExtTarget build() {
            checkNotNull(localSpeaker, "localSpeaker cannot be null");
            checkNotNull(remoteSpeaker, "remoteSpeaker cannot be null");
            return new DefaultExtTarget(localSpeaker, remoteSpeaker, type);
        }
    }
}