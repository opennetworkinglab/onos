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

package org.onosproject.tetopology.management.api;

import java.net.URI;
import java.util.Objects;

/**
 * Representation of an key identifier in URI.
 */
public class KeyId {
    /**
     * Represents either no uri, or an unspecified uri.
     */
    public static final KeyId NONE = keyId("none:none");

    private final URI uri;
    private final String str;

    // Public construction is prohibited
    private KeyId(URI uri) {
        this.uri = uri;
        //this.str = uri.toString().toLowerCase();
        this.str = uri.toString();
    }


    /**
     * Default constructor for serialization of KeyId.
     */
    protected KeyId() {
        this.uri = null;
        this.str = null;
    }

    /**
     * Returns the backing URI.
     *
     * @return backing URI
     */
    public URI uri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof KeyId) {
            KeyId that = (KeyId) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.str, that.str);
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }

    /**
     * Creates a uri id using the supplied URI.
     *
     * @param uri URI
     * @return UriId
     */
    public static KeyId keyId(URI uri) {
        return new KeyId(uri);
    }

    /**
     * Creates a uri id using the supplied URI string.
     *
     * @param string URI string
     * @return UriId
     */
    public static KeyId keyId(String string) {
        return keyId(URI.create(string));
    }

}
