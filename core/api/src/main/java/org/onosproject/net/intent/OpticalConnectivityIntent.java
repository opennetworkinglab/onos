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
import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OduSignalType;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An optical layer intent for connectivity between two OCh ports.
 * No traffic selector or traffic treatment are needed.
 */
@Beta
public final class OpticalConnectivityIntent extends Intent {
    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final OduSignalType signalType;
    private final boolean isBidirectional;

    /**
     * Creates an optical connectivity intent between the specified
     * connection points.
     *
     * @param appId application identification
     * @param key intent key
     * @param src the source transponder port
     * @param dst the destination transponder port
     * @param signalType signal type
     * @param isBidirectional indicates if intent is unidirectional
     * @param priority priority to use for flows from this intent
     */
    protected OpticalConnectivityIntent(ApplicationId appId,
                                        Key key,
                                        ConnectPoint src,
                                        ConnectPoint dst,
                                        OduSignalType signalType,
                                        boolean isBidirectional,
                                        int priority) {
        super(appId, key, Collections.emptyList(), priority);
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
        this.signalType = checkNotNull(signalType);
        this.isBidirectional = isBidirectional;
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
     * Builder for optical connectivity intents.
     */
    public static class Builder extends Intent.Builder {
        private ConnectPoint src;
        private ConnectPoint dst;
        private OduSignalType signalType;
        private boolean isBidirectional;

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
         * Sets the ODU signal type for the intent that will be built.
         *
         * @param signalType ODU signal type
         * @return this builder
         */
        public Builder signalType(OduSignalType signalType) {
            this.signalType = signalType;
            return this;
        }

        /**
         * Sets the directionality of the intent.
         *
         * @param isBidirectional true if bidirectional, false if unidirectional
         * @return this builder
         */
        public Builder bidirectional(boolean isBidirectional) {
            this.isBidirectional = isBidirectional;
            return this;
        }

        /**
         * Builds an optical connectivity intent from the accumulated parameters.
         *
         * @return point to point intent
         */
        public OpticalConnectivityIntent build() {

            return new OpticalConnectivityIntent(
                    appId,
                    key,
                    src,
                    dst,
                    signalType,
                    isBidirectional,
                    priority
            );
        }
    }

    /**
     * Constructor for serializer.
     */
    protected OpticalConnectivityIntent() {
        super();
        this.src = null;
        this.dst = null;
        this.signalType = null;
        this.isBidirectional = false;
    }

    /**
     * Returns the source transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getSrc() {
        return src;
    }

    /**
     * Returns the destination transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getDst() {
        return dst;
    }

    /**
     * Returns the ODU signal type.
     *
     * @return ODU signal type
     */
    public OduSignalType getSignalType() {
        return signalType;
    }

    /**
     * Returns the directionality of the intent.
     *
     * @return true if bidirectional, false if unidirectional
     */
    public boolean isBidirectional() {
        return isBidirectional;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("src", src)
                .add("dst", dst)
                .add("signalType", signalType)
                .add("isBidirectional", isBidirectional)
                .toString();
    }
}
