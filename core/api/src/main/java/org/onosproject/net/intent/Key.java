/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.onosproject.core.ApplicationId;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Key class for Intents.
 */
// TODO maybe pull this up to utils
// TODO need to make this classes kryo serializable
public class Key {

    private final long hash;
    private static final HashFunction HASH_FN = Hashing.md5();

    private Key(long hash) {
        this.hash = hash;
    }

    public long hash() {
        return hash;
    }

    public static Key of(String key, ApplicationId appId) {
        return new StringKey(key, appId);
    }

    public static Key of(long key, ApplicationId appId) {
        return new LongKey(key, appId);
    }

    private final static class StringKey extends Key {

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

        @Override
        public int hashCode() {
            return Objects.hash(key);
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
    }

    private final static class LongKey extends Key {

        private final ApplicationId appId;
        private static long key;

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
            return Long.toString(key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
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
            return Objects.equals(this.appId, other.appId) &&
                    this.key == other.key;
        }

    }
}


