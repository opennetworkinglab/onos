/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents IP Device Identifiers.
 */
public class IpDeviceIdentifier {

    private final RouteDistinguisher routeDish;
    private final RouteInstance routeInstance;
    private final AsNumber asNum;
    private final DomainId domainIdentifier;
    private final AreaId areaId;
    private final RouteIdentifier routerIdentifier;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDish routing distinguisher instance
     * @param routeInstance routing protocol instance
     * @param asNum AS number
     * @param domainIdentifier BGP-LS domain
     * @param areaId Area ID
     * @param routerIdentifier IGP router ID
     */
    public IpDeviceIdentifier(RouteDistinguisher routeDish, RouteInstance routeInstance, AsNumber asNum,
                           DomainId domainIdentifier, AreaId areaId, RouteIdentifier routerIdentifier) {
        this.routeDish = routeDish;
        this.areaId = areaId;
        this.asNum = asNum;
        this.domainIdentifier = domainIdentifier;
        this.routeInstance = routeInstance;
        this.routerIdentifier = routerIdentifier;
    }

    /**
     * Obtains Route Distinguisher of Ip Device.
     *
     * @return Area ID
     */
    public RouteDistinguisher routeDish() {
        return routeDish;
    }

    /**
     * Obtains Area ID if Ip Device.
     *
     * @return Area ID
     */
    public AreaId areaId() {
        return areaId;
    }

    /**
     * Obtains AS number of Ip Device.
     *
     * @return AS number
     */
    public AsNumber asNum() {
        return asNum;
    }

    /**
     * Obtains domain identifier of Ip Device.
     *
     * @return domain identifier
     */
    public DomainId domainIdentifier() {
        return domainIdentifier;
    }

    /**
     * Obtains Router id of Ip Device.
     *
     * @return Router id
     */
    public RouteIdentifier routerIdentifier() {
        return routerIdentifier;
    }

    /**
     * Obtains routing protocol instance.
     *
     * @return routing protocol instance
     */
    public RouteInstance routeInstance() {
        return routeInstance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeDish, areaId, asNum, domainIdentifier, routerIdentifier, routeInstance);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IpDeviceIdentifier) {
            IpDeviceIdentifier other = (IpDeviceIdentifier) obj;
            return Objects.equals(areaId, other.areaId) && Objects.equals(asNum, other.asNum)
                    && Objects.equals(domainIdentifier, other.domainIdentifier)
                    && Objects.equals(routerIdentifier, other.routerIdentifier)
                    && Objects.equals(routeInstance, other.routeInstance)
                    && Objects.equals(routeDish, other.routeDish);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("areaId", areaId)
                .add("asNum", asNum)
                .add("domainIdentifier", domainIdentifier)
                .add("routerIdentifier", routerIdentifier)
                .add("routeInstance", routeInstance)
                .add("routeDish", routeDish)
                .toString();
    }
}