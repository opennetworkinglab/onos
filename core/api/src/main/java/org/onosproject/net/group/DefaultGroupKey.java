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
package org.onosproject.net.group;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

/**
 * Default implementation of group key interface.
 */
public class DefaultGroupKey implements GroupKey {

    private final byte[] key;

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

}