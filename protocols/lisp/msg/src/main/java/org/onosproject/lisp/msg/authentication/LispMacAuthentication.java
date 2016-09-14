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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.SHA1;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.SHA256;

/**
 * LISP MAC authentication utility class.
 */
public class LispMacAuthentication {

    private static final Logger log = LoggerFactory.getLogger(LispMacAuthentication.class);

    private String algorithm;
    private int authenticationLength;

    public LispMacAuthentication(LispAuthenticationKeyEnum authType) {

        if (authType == SHA1 || authType == SHA256) {
            algorithm = authType.getName();
        } else {
            log.warn("Not support provided algorithm {}", authType.getName());
            return;
        }

        try {
            authenticationLength = Mac.getInstance(algorithm).getMacLength();
        } catch (NoSuchAlgorithmException e) {
            log.warn("Not support provided algorithm {}", algorithm);
        }
    }

    /**
     * Obtains dummy authentication data.
     *
     * @return dummy authentication data
     */
    public byte[] getAuthenticationData() {
        return new byte[0];
    }

    /**
     * Obtains authentication data with given key and algorithm.
     *
     * @param key  authentication key (e.g., EID)
     * @param data array of byte buffer for place holder
     * @return authentication data
     */
    public byte[] getAuthenticationData(String key, byte[] data) {
        try {
            SecretKeySpec signKey = new SecretKeySpec(key.getBytes(), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signKey);

            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Not support provided algorithm {}", algorithm);
        } catch (InvalidKeyException e) {
            log.warn("Provided key {} is invalid", key);
        }
        return null;
    }

    /**
     * Obtains authentication data length.
     *
     * @return authentication data length
     */
    public int getAuthenticationLength() {
        return authenticationLength;
    }

    /**
     * Obtains authentication algorithm.
     *
     * @return authentication algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }
}
