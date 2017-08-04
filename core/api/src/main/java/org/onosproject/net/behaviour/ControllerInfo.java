/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.Annotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents information for a device to connect to a controller.
 */
public class ControllerInfo implements Annotated {

    private IpAddress ip = IpAddress.valueOf("0.0.0.0");
    private int port = 6653;
    private String type = "error";
    private final Annotations annotations;

    /**
     * Information for contacting the controller.
     *
     * @param ip   the ip address
     * @param port the tcp port
     * @param type the connection type
     */
    public ControllerInfo(IpAddress ip, int port, String type) {
        this(ip, port, type, DefaultAnnotations.EMPTY);
    }

    /**
     * Information for contacting the controller.
     *
     * @param ip   the ip address
     * @param port the tcp port
     * @param type the connection type
     * @param annotations optional key/value annotations
     */
    public ControllerInfo(IpAddress ip, int port, String type, Annotations annotations) {
        this.ip = checkNotNull(ip);
        this.port = port;
        this.type = checkNotNull(type);
        this.annotations = checkNotNull(annotations);
    }

    // TODO Factory method equivalent to this method
    //      should probably live in OVSDB, NETCONF package.
    /**
     * Information for contacting the controller, if some information
     * is not contained in the target string because it's optional
     * it's leaved as in the field declaration (default values).
     *
     * @param target column returned from ovsdb query
     *
     * @deprecated in Hummingbird (1.7.0)
     */
    @Deprecated
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
        this.annotations = DefaultAnnotations.EMPTY;
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

    @Override
    public Annotations annotations() {
        return annotations;
    }

    // TODO Method equivalent to this method
    //      should probably live in OVSDB, NETCONF package.
    // @deprecated in Hummingbird (1.7.0)
    @Deprecated
    public String target() {
        if (type.startsWith("p")) {
            return type + ":" + port + ":" + ip;
        } else {
            if (annotations.equals(DefaultAnnotations.EMPTY)) {
                return type + ":" + ip + ":" + port;
            } else {
                return type + ":" + ip + ":" + port + ":" + annotations.toString();
            }
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(ip, port, type);
    }

    @Override
    public boolean equals(Object toBeCompared) {
        if (toBeCompared instanceof ControllerInfo) {
            ControllerInfo that = (ControllerInfo) toBeCompared;
            return Objects.equals(this.type, that.type) &&
                    Objects.equals(this.ip, that.ip) &&
                    Objects.equals(this.port, that.port);
        }
        return false;
    }
}
