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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Path;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ResourceGroup;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An optical layer intent with explicitly selected path.
 */
@Beta
public final class OpticalPathIntent extends Intent {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Path path;
    private final OchSignal lambda;
    private final OchSignalType signalType;
    private final boolean isBidirectional;

    private OpticalPathIntent(ApplicationId appId,
                              Key key,
                              ConnectPoint src,
                              ConnectPoint dst,
                              Path path,
                              OchSignal lambda,
                              OchSignalType signalType,
                              boolean isBidirectional,
                              int priority,
                              ResourceGroup resourceGroup) {
        super(appId, key, ImmutableSet.copyOf(path.links()), priority, resourceGroup);
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
        this.path = checkNotNull(path);
        this.lambda = checkNotNull(lambda);
        this.signalType = checkNotNull(signalType);
        this.isBidirectional = isBidirectional;
    }

    protected OpticalPathIntent() {
        this.src = null;
        this.dst = null;
        this.path = null;
        this.lambda = null;
        this.signalType = null;
        this.isBidirectional = true;
    }

    /**
     * Returns a new optical connectivity intent builder.
     *
     * @return host to host intent builder
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Builder for optical path intents.
     */
    public static class Builder extends Intent.Builder {
        private ConnectPoint src;
        private ConnectPoint dst;
        private Path path;
        private OchSignal lambda;
        private OchSignalType signalType;
        private boolean isBidirectional;
        private Key key;

        @Override
        public Builder appId(ApplicationId appId) {
            return (Builder) super.appId(appId);
        }

        @Override
        public Builder key(Key key) {
            return (Builder) super.key(key);
        }

        @Override
        public Builder priority(int priority) {
            return (Builder) super.priority(priority);
        }

        @Override
        public Builder resourceGroup(ResourceGroup resourceGroup) {
            return (Builder) super.resourceGroup(resourceGroup);
        }

        /**
         * Sets the source for the intent that will be built.
         *
         * @param src source to use for built intent
         * @return this builder
         */
        public Builder src(ConnectPoint src) {
            this.src = src;
            return this;
        }

        /**
         * Sets the destination for the intent that will be built.
         *
         * @param dst dest to use for built intent
         * @return this builder
         */
        public Builder dst(ConnectPoint dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Sets the path for the intent that will be built.
         *
         * @param path path to use for built intent
         * @return this builder
         */
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the optical channel (lambda) for the intent that will be built.
         *
         * @param lambda the optical channel
         * @return this builder
         */
        public Builder lambda(OchSignal lambda) {
            this.lambda = lambda;
            return this;
        }

        /**
         * Sets the optical signal type for the intent that will be built.
         *
         * @param signalType the optical signal type
         * @return this builder
         */
        public Builder signalType(OchSignalType signalType) {
            this.signalType = signalType;
            return this;
        }

        /**
         * Sets the intent's direction.
         *
         * @param isBidirectional indicates if intent is bidirectional
         * @return this builder
         */
        public Builder bidirectional(boolean isBidirectional) {
            this.isBidirectional = isBidirectional;
            return this;
        }

        /**
         * Builds an optical path intent from the accumulated parameters.
         *
         * @return optical path intent
         */
        public OpticalPathIntent build() {

            return new OpticalPathIntent(
                    appId,
                    key,
                    src,
                    dst,
                    path,
                    lambda,
                    signalType,
                    isBidirectional,
                    priority,
                    resourceGroup
            );
        }
    }


    public ConnectPoint src() {
        return src;
    }

    public ConnectPoint dst() {
        return dst;
    }

    public Path path() {
        return path;
    }

    public OchSignal lambda() {
        return lambda;
    }

    public OchSignalType signalType() {
        return signalType;
    }

    public boolean isBidirectional() {
        return isBidirectional;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("key", key())
                .add("resources", resources())
                .add("ingressPort", src)
                .add("egressPort", dst)
                .add("path", path)
                .add("lambda", lambda)
                .add("signalType", signalType)
                .add("isBidirectional", isBidirectional)
                .toString();
    }
}
