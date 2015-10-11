/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.base.Preconditions;
import org.onlab.packet.IpAddress;

import java.util.Objects;

/**
 * Represents information for a device to connect to a controller.
 */
public class ControllerInfo {

    private IpAddress ip = IpAddress.valueOf("0.0.0.0");
    private int port = 6653;
    private String type = "error";

    /**
     * Information for contacting the controller.
     *
     * @param ip   the ip address
     * @param port the tcp port
     * @param type the connection type
     */
    public ControllerInfo(IpAddress ip, int port, String type) {
        this.ip = ip;
        this.port = port;
        this.type = type;
    }

    /**
     * Information for contacting the controller, if some information
     * is not contained in the target string because it's optional
     * it's leaved as in the field declaration (default values).
     *
     * @param target column returned from ovsdb query
     */
    public ControllerInfo(String target) {
        String[] data = target.split(":");
        this.type = data[0];
        Preconditions.checkArgument(!data[0].contains("unix"),
                                    "Unable to create controller info " +
                                            "from {} because it's based " +
                                            "on unix sockets", target);
        if (data[0].startsWith("p")) {
            if (data.length >= 2) {
                this.port = Integer.parseInt(data[1]);
            }
            if (data.length == 3) {
                this.ip = IpAddress.valueOf(data[2]);
            }
        } else {
            this.ip = IpAddress.valueOf(data[1]);
            if (data.length == 3) {
                this.port = Integer.parseInt(data[2]);
            }
        }
    }

    /**
     * Exposes the ip address of the controller.
     *
     * @return IpAddress ip address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Exposes the tcp port of the controller.
     *
     * @return int tcp port
     */
    public int port() {
        return port;
    }

    /**
     * Exposes the type of the controller connection.
     *
     * @return String type
     */
    public String type() {
        return type;
    }

    public String target() {
        if (type.startsWith("p")) {
            return type + ":" + port + ":" + ip;
        } else {
            return type + ":" + ip + ":" + port;
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(ip, port, type);
    }

    @Override
    public boolean equals(Object toBeCompared) {
        if (toBeCompared instanceof ControllerInfo) {
            ControllerInfo controllerInfo = (ControllerInfo) toBeCompared;
            if (controllerInfo.type().equals(this.type)
                    && controllerInfo.ip().equals(this.ip())
                    && controllerInfo.port() == this.port) {
                return true;
            }
        }
        return false;
    }
}
