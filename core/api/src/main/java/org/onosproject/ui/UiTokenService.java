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

/**
 * Service for handling UI session tokens.
 */
public interface UiTokenService {

    /**
     * Issues a session token. The service will generate a new token,
     * publish it in the distributed map of valid UI session tokens, and
     * return it to the caller.
     *
     * @param username the username to be associated with the token.
     * @return the token
     */
    UiSessionToken issueToken(String username);

    /**
     * Revokes the specified session token. The service will remove the token
     * from the distributed map of valid UI session tokens.
     *
     * @param token the token to be revoked
     */
    void revokeToken(UiSessionToken token);

    /**
     * Returns true if the specified token is currently in the distributed
     * map of valid UI session tokens.
     *
     * @param token the token to check
     * @return true, if the token is currently valid; false otherwise
     */
    boolean isTokenValid(UiSessionToken token);
}
