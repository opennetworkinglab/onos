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
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An object that is used to locate a resource in a network.
 * A ResourcePath represents a path that is hierarchical and composed of a sequence
 * of elementary resources that are not globally identifiable. A ResourcePath can be a globally
 * unique resource identifier.
 *
 * Two types of resource are considered. One is discrete type and the other is continuous type.
 * Discrete type resource is a resource whose amount is measured as a discrete unit. VLAN ID and
 * MPLS label are examples of discrete type resource. Continuous type resource is a resource whose
 * amount is measured as a continuous value. Bandwidth is an example of continuous type resource.
 * A double value is associated with a continuous type value.
 *
 * Users of this class must keep the semantics of resources regarding the hierarchical structure.
 * For example, resource path, Device:1/Port:1/VLAN ID:100, is valid, but resource path,
 * VLAN ID:100/Device:1/Port:1 is not valid because a link is not a sub-component of a VLAN ID.
 */
@Beta
public abstract class ResourcePath {

    private final Discrete parent;
    private final Key key;

    public static final Discrete ROOT = new Discrete();

    public static ResourcePath discrete(DeviceId device) {
        return new Discrete(Key.of(device));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static ResourcePath discrete(DeviceId device, Object... components) {
        return new Discrete(Key.of(device, components));
    }

    /**
     * Creates an resource path which represents a discrete-type resource from the specified components.
     *
     * @param device device ID which is the first component of the path
     * @param port port number which is the second component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static ResourcePath discrete(DeviceId device, PortNumber port, Object... components) {
        return new Discrete(Key.of(device, port, components));
    }

    /**
     * Creates an resource path which represents a continuous-type resource from the specified components.
     *
     * @param value amount of the resource
     * @param device device ID which is the first component of the path
     * @param components following components of the path. The order represents hierarchical structure of the resource.
     * @return resource path instance
     */
    public static ResourcePath continuous(double value, DeviceId device, Object... components) {
        checkArgument(components.length > 0,
                "Length of components must be greater thant 0, but " + components.length);

        return new Continuous(Key.of(device, components), value);
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
    public static ResourcePath continuous(double value, DeviceId device, PortNumber port, Object... components) {
        return new Continuous(Key.of(device, port, components), value);
    }

    /**
     * Creates an resource path from the specified key.
     *
     * @param key key of the path
     */
    protected ResourcePath(Key key) {
        checkNotNull(key);

        this.key = key;
        if (key.components.size() == 1) {
            this.parent = ROOT;
        } else {
            this.parent = new Discrete(key.parent());
        }
    }

    // for serialization
    private ResourcePath() {
        this.parent = null;
        this.key = Key.ROOT;
    }

    /**
     * Returns the components of this resource path.
     *
     * @return the components of this resource path
     */
    public List<Object> components() {
        return key.components;
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
    public ResourcePath child(Object child) {
        checkState(this instanceof Discrete);

        return new Discrete(key().child(child));
    }

    /**
     * Returns a child resource path of this instance with specifying a child object and
     * value. The child resource path is continuous-type.
     *
     * @param child child object
     * @param value value
     * @return a child resource path
     */
    public ResourcePath child(Object child, double value) {
        checkState(this instanceof Discrete);

        return new Continuous(key.child(child), value);
    }

    /**
     * Returns the last component of this instance.
     *
     * @return the last component of this instance.
     * The return value is equal to the last object of {@code components()}.
     */
    public Object last() {
        if (key.components.isEmpty()) {
            return null;
        }
        return key.components.get(key.components.size() - 1);
    }

    /**
     * Returns the key of this resource path.
     *
     * @return the key of this resource path
     */
    public Key key() {
        return key;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResourcePath)) {
            return false;
        }
        final ResourcePath that = (ResourcePath) obj;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
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
    public static final class Discrete extends ResourcePath {
        private Discrete() {
            super();
        }

        private Discrete(Key key) {
            super(key);
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
    public static final class Continuous extends ResourcePath {
        private final double value;

        private Continuous(Key key, double value) {
            super(key);
            this.value = value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.key(), this.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Continuous)) {
                return false;
            }

            if (!super.equals(obj)) {
                return false;
            }

            final Continuous other = (Continuous) obj;
            return Objects.equals(this.key(), other.key());
        }

        /**
         * Returns the value of the resource amount.
         *
         * @return the value of the resource amount
         */
        public double value() {
            return value;
        }
    }

    /**
     * Represents key of resource path used as a key in ResourceStore.
     * This class is exposed to public, but intended to use only in ResourceStore implementations.
     */
    @Beta
    public static final class Key {
        private static final Key ROOT = new Key();

        private final ImmutableList<Object> components;

        private static Key of(DeviceId device, Object... components) {
            return new Key(ImmutableList.builder()
                    .add(device)
                    .add(components)
                    .build());
        }

        private static Key of(DeviceId device, PortNumber port, Object... components) {
            return new Key(ImmutableList.builder()
                    .add(device)
                    .add(port)
                    .add(components)
                    .build());
        }

        private Key(ImmutableList<Object> components) {
            this.components = checkNotNull(components);
        }

        // for serializer
        private Key() {
            this.components = ImmutableList.of();
        }

        // IndexOutOfBoundsException is raised when the instance is equal to ROOT
        private Key parent() {
            if (components.size() == 1) {
                return ROOT;
            } else {
                return new Key(components.subList(0, components.size() - 1));
            }
        }

        private Key child(Object child) {
            return new Key(ImmutableList.builder()
                    .add(components)
                    .add(child)
                    .build());
        }

        @Override
        public int hashCode() {
            return components.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }

            Key other = (Key) obj;
            return Objects.equals(this.components, other.components);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("components", components)
                    .toString();
        }
    }
}
