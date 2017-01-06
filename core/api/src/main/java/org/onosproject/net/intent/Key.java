/*
 * Copyright 2015-present Open Networking Laboratory
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
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceConsumerId;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Key class for Intents.
 */
// TODO maybe pull this up to utils
@Beta
public abstract class Key implements Comparable<Key>, ResourceConsumer {

    //TODO consider making this a HashCode object (worry about performance)
    private final long hash;
    private static final HashFunction HASH_FN = Hashing.md5();

    protected Key(long hash) {
        this.hash = hash;
    }

    public long hash() {
        return hash;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(hash);
    }

    @Override
    public abstract boolean equals(Object obj);

    /**
     * Creates a key based on the provided string.
     * <p>
     * Note: Two keys with equal value, but different appId, are not equal.
     * Warning: it is caller responsibility to make sure the hashed value of
     * {@code value} is unique.
     * </p>
     *
     * @param key the provided string
     * @param appId application id to associate with this key
     * @return the key for the string
     */
    public static Key of(String key, ApplicationId appId) {
        return new StringKey(key, appId);
    }

    /**
     * Creates a key based on the provided long.
     * <p>
     * Note: Two keys with equal value, but different appId, are not equal.
     * Also, "10" and 10L are different.
     * </p>
     *
     * @param key the provided long
     * @param appId application id to associate with this key
     * @return the key for the long
     */
    public static Key of(long key, ApplicationId appId) {
        return new LongKey(key, appId);
    }

    @Override
    public ResourceConsumerId consumerId() {
        return ResourceConsumerId.of(hash(), getClass());
    }

    private static final class StringKey extends Key {

        private final ApplicationId appId;
        private final String key;

        private StringKey(String key, ApplicationId appId) {
            super(HASH_FN.newHasher()
                          .putShort(appId.id())
                          .putString(key, StandardCharsets.UTF_8)
                          .hash().asLong());
            this.key = key;
            this.appId = appId;
        }

        @Override
        public String toString() {
            return key;
        }

        // checkstyle requires this
        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final StringKey other = (StringKey) obj;
            return this.hash() == other.hash() &&
                    Objects.equals(this.appId, other.appId) &&
                    Objects.equals(this.key, other.key);
        }

        @Override
        public int compareTo(Key o) {
            StringKey sk = (StringKey) o;
            return this.key.compareTo(sk.key);
        }
    }

    private static final class LongKey extends Key {

        private final ApplicationId appId;
        private final long key;

        private LongKey(long key, ApplicationId appId) {
            super(HASH_FN.newHasher()
                          .putShort(appId.id())
                          .putLong(key)
                          .hash().asLong());
            this.key = key;
            this.appId = appId;
        }

        @Override
        public String toString() {
            return "0x" + Long.toHexString(key);
        }

        // checkstyle requires this
        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final LongKey other = (LongKey) obj;
            return this.hash() == other.hash() &&
                    this.key == other.key &&
                    Objects.equals(this.appId, other.appId);
        }

        @Override
        public int compareTo(Key o) {
            Long myKey = key;
            Long otherKey = ((LongKey) o).key;
            return myKey.compareTo(otherKey);
        }
    }
}


