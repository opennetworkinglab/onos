/*
 * Copyright 2016-present Open Networking Foundation
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
 * A singleton class that Stores LISP authentication information.
 */
public final class LispAuthenticationConfig {

    private String lispAuthKey;
    private short lispAuthKeyId;

    /**
     * Obtains an authentication info singleton instance.
     *
     * @return authentication info singleton instance
     */
    public static LispAuthenticationConfig getInstance() {
        return SingletonHelper.INSTANCE;
    }

    // non-instantiable (except for our Singleton)
    private LispAuthenticationConfig() {
    }

    /**
     * Updates LISP authentication key.
     *
     * @param lispAuthKey LISP authentication key
     */
    public void updateLispAuthKey(String lispAuthKey) {
        this.lispAuthKey = lispAuthKey;
    }

    /**
     * Updates LISP authentication key identifier.
     *
     * @param lispAuthKeyId LISP authentication key identifier
     */
    public void updateLispAuthKeyId(int lispAuthKeyId) {
        this.lispAuthKeyId = (short) lispAuthKeyId;
    }

    /**
     * Obtains LISP authentication key.
     *
     * @return LISP authentication key
     */
    public String lispAuthKey() {
        return lispAuthKey;
    }

    /**
     * Obtains LISP authentication key identifier.
     *
     * @return LISP authentication key identifier
     */
    public short lispAuthKeyId() {
        return lispAuthKeyId;
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispAuthenticationConfig INSTANCE =
                                                new LispAuthenticationConfig();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
