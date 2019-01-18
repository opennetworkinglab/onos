/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn;

import org.onosproject.net.ConnectPoint;
import java.util.Objects;

/**
 * Configuration related to a TAPI Connectivity.
 *
 * At this point, the class only conveys the
 * Connectivity service uuid, the ConnectPoints
 * obtained from the TAPI SIP and a boolean that
 * means setup or release. Additional attributes
 * may be needed when we further parse the TAPI
 * request, this class will grow accordingly.
 */
public final class TapiConnectivityConfig {

    private final String uuid;
    private final ConnectPoint left;
    private final ConnectPoint right;
    private final boolean setup;


    /**
     * Constructor with the Connection uuid and endpoints.
     *
     * @param uuid - TAPI uuid of the connection.
     * @param leftCp Left (ingress) ConnectPoint.
     * @param rightCp Right (egress) ConnectPoint.
     * @param setup True or false (setup or release).
     */
    public TapiConnectivityConfig(String uuid, ConnectPoint leftCp,
        ConnectPoint rightCp, Boolean setup) {
        this.uuid = uuid;
        this.left = leftCp;
        this.right = rightCp;
        this.setup = setup;
    }

    /**
     * Get the UUID associated to this TAPI connectivity rquest.
     *
     * @return the UUID.
     */
    public String uuid() {
        return uuid;
    }


    /**
     * Get the left (first) ConnectPoint of the request.
     * The left CP corresponds to the first endpoint in the TAPI
     * request.
     *
     * @return the connect point.
     */
    public ConnectPoint leftCp() {
        return left;
    }


    /**
     * Get the right (second) ConnectPoint of the request.
     * The right CP corresponds to the second endpoint in the TAPI
     * request.
     *
     * @return the connect point.
     */
    public ConnectPoint rightCp() {
        return right;
    }

    /**
     * Check if the request is for a setup or release.
     *
     * @return True for a setup request, False otherwise.
     */
    public Boolean isSetup() {
        return setup;
    }


    /**
     * Equals comparison.
     *
     * @return True if this:Object.equals(o)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TapiConnectivityConfig)) {
            return false;
        }
        TapiConnectivityConfig that = (TapiConnectivityConfig) o;
        return Objects.equals(uuid, that.uuid) &&
            Objects.equals(left, that.left) &&
            Objects.equals(right, that.right) &&
            Objects.equals(setup, that.setup);
    }


    /**
     * Get the hashcode for the TapiConnectivityConfig Object.
     *
     * @return the result of Objects.hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid, left, right, setup);
    }
}
