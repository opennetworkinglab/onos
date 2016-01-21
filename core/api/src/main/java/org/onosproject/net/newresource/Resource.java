/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An object that represent a resource in a network.
 * A Resource can represents path-like hierarchical structure with its ID. An ID of resource is
 * composed of a sequence of elementary resources that are not globally identifiable. A Resource
 * can be globally identifiable by its ID.
 *
 * Two types of resource are considered. One is discrete type and the other is continuous type.
 * Discrete type resource is a resource whose amount is measured as a discrete unit. VLAN ID and
 * MPLS label are examples of discrete type resource. Continuous type resource is a resource whose
 * amount is measured as a continuous value. Bandwidth is an example of continuous type resource.
 * A double value is associated with a continuous type value.
 *
 * Users of this class must keep the semantics of resources regarding the hierarchical structure.
 * For example, resource, Device:1/Port:1/VLAN ID:100, is valid, but resource,
 * VLAN ID:100/Device:1/Port:1 is not valid because a link is not a sub-component of a VLAN ID.
 */
@Beta
public abstract class Resource {

    private final Discrete parent;
    private final ResourceId id;

    public static final Discrete ROOT = new Discrete();

    public static Resource discrete(DeviceId device) {
        return new Discrete(ResourceId.of(device));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static Resource discrete(DeviceId device, Object... components) {
        return new Discrete(ResourceId.of(device, components));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param port port number which is the second component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static Resource discrete(DeviceId device, PortNumber port, Object... components) {
        return new Discrete(ResourceId.of(device, port, components));
    }

    /**
     * Creates an resource path which represents a continuous-type resource from the specified components.
     *
     * @param value amount of the resource
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static Resource continuous(double value, DeviceId device, Object... components) {
        checkArgument(components.length > 0,
                "Length of components must be greater thant 0, but " + components.length);

        return new Continuous(ResourceId.of(device, components), value);
    }

    /**
     * Creates an resource path which represents a continuous-type resource from the specified components.
     *
     * @param value amount of the resource
     * @param device device ID which is the first component of the path.
     * @param port port number which is the second component of the path.
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static Resource continuous(double value, DeviceId device, PortNumber port, Object... components) {
        return new Continuous(ResourceId.of(device, port, components), value);
    }

    /**
     * Creates an resource path from the specified id.
     *
     * @param id id of the path
     */
    protected Resource(ResourceId id) {
        checkNotNull(id);

        this.id = id;
        if (id.components.size() == 1) {
            this.parent = ROOT;
        } else {
            this.parent = new Discrete(id.parent());
        }
    }

    // for serialization
    private Resource() {
        this.parent = null;
        this.id = ResourceId.ROOT;
    }

    /**
     * Returns the components of this resource path.
     *
     * @return the components of this resource path
     */
    public List<Object> components() {
        return id.components;
    }

    /**
     * Returns the parent resource path of this instance.
     * E.g. if this path is Link:1/VLAN ID:100, the return value is the resource path for Link:1.
     *
     * @return the parent resource path of this instance.
     * If there is no parent, empty instance will be returned.
     */
    public Optional<Discrete> parent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Returns a child resource path of this instance with specifying the child object.
     * The child resource path is discrete-type.
     *
     * @param child child object
     * @return a child resource path
     */
    public Resource child(Object child) {
        checkState(this instanceof Discrete);

        return new Discrete(id().child(child));
    }

    /**
     * Returns a child resource path of this instance with specifying a child object and
     * value. The child resource path is continuous-type.
     *
     * @param child child object
     * @param value value
     * @return a child resource path
     */
    public Resource child(Object child, double value) {
        checkState(this instanceof Discrete);

        return new Continuous(id.child(child), value);
    }

    /**
     * Returns the last component of this instance.
     *
     * @return the last component of this instance.
     * The return value is equal to the last object of {@code components()}.
     */
    public Object last() {
        if (id.components.isEmpty()) {
            return null;
        }
        return id.components.get(id.components.size() - 1);
    }

    /**
     * Returns the ID of this resource path.
     *
     * @return the ID of this resource path
     */
    public ResourceId id() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Resource that = (Resource) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }

    /**
     * Represents a resource path which specifies a resource which can be measured
     * as a discrete unit. A VLAN ID and a MPLS label of a link are examples of the resource.
     * <p>
     * Note: This class is exposed to the public, but intended to be used in the resource API
     * implementation only. It is not for resource API user.
     * </p>
     */
    @Beta
    public static final class Discrete extends Resource {
        private Discrete() {
            super();
        }

        private Discrete(ResourceId id) {
            super(id);
        }
    }

    /**
     * Represents a resource path which specifies a resource which can be measured
     * as continuous value. Bandwidth of a link is an example of the resource.
     * <p>
     * Note: This class is exposed to the public, but intended to be used in the resource API
     * implementation only. It is not for resource API user.
     */
    @Beta
    public static final class Continuous extends Resource {
        private final double value;

        private Continuous(ResourceId id, double value) {
            super(id);
            this.value = value;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        // explicitly overriding to express that we intentionally ignore
        // `value` in equality comparison
        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        /**
         * Returns the value of the resource amount.
         *
         * @return the value of the resource amount
         */
        public double value() {
            return value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id())
                    .add("value", value)
                    .toString();
        }
    }

}
