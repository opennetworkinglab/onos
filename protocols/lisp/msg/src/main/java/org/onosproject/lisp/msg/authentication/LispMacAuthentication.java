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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * LISP MAC authentication utility class.
 */
public class LispMacAuthentication {

    private static final Logger log = LoggerFactory.getLogger(LispMacAuthentication.class);

    private static final String NOT_SUPPORT_ALGORITHM_MSG =
                                "Not support provided algorithm {}";
    private static final String INVALID_KEY_MSG = "Provided key {} is invalid";

    private String algorithm;

    /**
     * Default constructor with given authentication key type.
     *
     * @param authType authentication key type
     */
    LispMacAuthentication(LispAuthenticationKeyEnum authType) {

        if (authType == LispAuthenticationKeyEnum.SHA1 || authType == LispAuthenticationKeyEnum.SHA256) {
            algorithm = authType.getName();
        } else {
            log.warn(NOT_SUPPORT_ALGORITHM_MSG, authType.getName());
        }
    }

    /**
     * Obtains dummy authentication data.
     *
     * @return dummy authentication data
     */
    byte[] getAuthenticationData() {
        return new byte[0];
    }

    /**
     * Obtains authentication data with given key and algorithm.
     *
     * @param key  authentication key
     * @param data array of byte buffer for place holder
     * @return authentication data
     */
    byte[] getAuthenticationData(String key, byte[] data) {
        try {
            SecretKeySpec signKey = new SecretKeySpec(key.getBytes(), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signKey);

            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            log.warn(NOT_SUPPORT_ALGORITHM_MSG, algorithm, e.getMessage());
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            log.warn(INVALID_KEY_MSG, key, e.getMessage());
            throw new IllegalArgumentException(e);
        }
    }
}
