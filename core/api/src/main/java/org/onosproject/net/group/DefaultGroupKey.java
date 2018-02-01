/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.group;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

/**
 * Default implementation of group key interface.
 */
public class DefaultGroupKey implements GroupKey {
    private final byte[] key;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public DefaultGroupKey(byte[] key) {
        this.key = checkNotNull(key);
    }

    @Override
    public byte[] key() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultGroupKey)) {
            return false;
        }
        DefaultGroupKey that = (DefaultGroupKey) o;
        return (Arrays.equals(this.key, that.key));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.key);
    }

    /**
     * Returns a hex string representation of the byte array that is used
     * as a group key. This solution was adapted from
     * http://stackoverflow.com/questions/9655181/
     */
    @Override
    public String toString() {
        char[] hexChars = new char[key.length * 2];
        for (int j = 0; j < key.length; j++) {
            int v = key[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return "0x" + new String(hexChars);
    }
}