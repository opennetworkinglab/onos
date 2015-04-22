/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An optical layer intent for connectivity from one transponder port to another
 * transponder port. No traffic selector or traffic treatment are needed.
 */
public final class OpticalConnectivityIntent extends Intent {
    private final ConnectPoint src;
    private final ConnectPoint dst;

    /**
     * Creates an optical connectivity intent between the specified
     * connection points.
     *
     * @param appId application identification
     * @param key intent key
     * @param src the source transponder port
     * @param dst the destination transponder port
     * @param priority priority to use for flows from this intent
     */
    protected OpticalConnectivityIntent(ApplicationId appId,
                                     Key key,
                                     ConnectPoint src,
                                     ConnectPoint dst,
                                     int priority) {
        super(appId, key, Collections.emptyList(), priority);
        this.src = checkNotNull(src);
        this.dst = checkNotNull(dst);
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
                .toString();
    }
}
