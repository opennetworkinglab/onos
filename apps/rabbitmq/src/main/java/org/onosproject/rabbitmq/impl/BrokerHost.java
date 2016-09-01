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
package org.onosproject.rabbitmq.impl;

/**
 * Represents the URL pointing to MQ Server. Used to connect to MQ Server.
 */
public class BrokerHost {

    private final String url;

    /**
     * Sets the MQ Server URL.
     *
     * @param url represents url of the MQ Server
     */
    public BrokerHost(String url) {
        this.url = url;
    }

    /**
     * Returns the MQ Server URL.
     *
     * @return url of the MQ Server
     */
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BrokerHost that = (BrokerHost) o;

        return url != null ? url.equals(that.url) : that.url == null;
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }

    @Override
    public String toString() {
        return url;
    }
}
