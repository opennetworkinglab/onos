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

package org.onosproject.net.config;

/**
 * Indicates an invalid configuration was supplied by the user.
 */
public class InvalidConfigException extends RuntimeException {

    private final String subjectKey;
    private final String subject;
    private final String configKey;

    /**
     * Creates a new invalid config exception about a specific config.
     *
     * @param subjectKey config's subject key
     * @param subject config's subject
     * @param configKey config's config key
     */
    public InvalidConfigException(String subjectKey, String subject, String configKey) {
        this(subjectKey, subject, configKey, null);
    }

    /**
     * Creates a new invalid config exception about a specific config with an
     * exception regarding the cause of the invalidity.
     *
     * @param subjectKey config's subject key
     * @param subject config's subject
     * @param configKey config's config key
     * @param cause cause of the invalidity
     */
    public InvalidConfigException(String subjectKey, String subject, String configKey, Throwable cause) {
        super(message(subjectKey, subject, configKey, cause), cause);
        this.subjectKey = subjectKey;
        this.subject = subject;
        this.configKey = configKey;
    }

    /**
     * Returns the subject key of the config.
     *
     * @return subject key
     */
    public String subjectKey() {
        return subjectKey;
    }

    /**
     * Returns the string representation of the subject of the config.
     *
     * @return subject
     */
    public String subject() {
        return subject;
    }

    /**
     * Returns the config key of the config.
     *
     * @return config key
     */
    public String configKey() {
        return configKey;
    }

    private static String message(String subjectKey, String subject, String configKey, Throwable cause) {
        String error = "Error parsing config " + subjectKey + "/" + subject + "/" + configKey;
        if (cause != null) {
            error = error + ": " + cause.getMessage();
        }
        return error;
    }
}
