/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

package org.onosproject.ui;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates a session token pertaining to a Web UI websocket connection.
 */
public class UiSessionToken {

    private final String token;

    /**
     * Creates a session token from the given string.
     *
     * @param token the token in string form
     * @throws NullPointerException if token is null
     */
    public UiSessionToken(String token) {
        this.token = checkNotNull(token);
    }

    @Override
    public String toString() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UiSessionToken that = (UiSessionToken) o;
        return token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }
}
