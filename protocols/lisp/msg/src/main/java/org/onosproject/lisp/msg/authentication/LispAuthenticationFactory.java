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
package org.onosproject.lisp.msg.authentication;

/**
 * A factory class that returns LISP authentication instance.
 */
public final class LispAuthenticationFactory {

    /**
     * Prevents object instantiation from external.
     */
    private LispAuthenticationFactory() {
    }

    /**
     * Obtains a factory singleton instance.
     *
     * @return factory singleton instance
     */
    public static LispAuthenticationFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Generates a new authentication data with given authentication key and
     * authentication type.
     *
     * @param authType authentication key type
     * @param authKey  authentication key string
     * @param data     authentication data
     * @return authentication data
     */
    public byte[] createAuthenticationData(LispAuthenticationKeyEnum authType,
                                           String authKey, byte[] data) {
        LispMacAuthentication macAuth = new LispMacAuthentication(authType);
        byte[] authData;
        switch (authType) {
            case SHA1:
            case SHA256:
                authData = macAuth.getAuthenticationData(authKey, data);
                break;
            case NONE:
            case UNKNOWN:
            default:
                authData = macAuth.getAuthenticationData();
                break;
        }
        return authData;
    }

    /**
     * A private singleton helper class.
     */
    private static final class SingletonHelper {
        private static final LispAuthenticationFactory INSTANCE =
                                                new LispAuthenticationFactory();
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
