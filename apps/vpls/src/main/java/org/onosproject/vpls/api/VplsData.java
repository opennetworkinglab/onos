/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.vpls.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.EncapsulationType;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.*;

/**
 * Class stores a VPLS information.
 */
public final class VplsData {
    /**
     * States of a VPLS.
     */
    public enum VplsState {
        UPDATING,
        ADDING,
        REMOVING,
        ADDED,
        REMOVED,
        FAILED
    }

    private String name;
    private Set<Interface> interfaces;
    private EncapsulationType encapsulationType;
    private VplsState state;

    /**
     * Constructs a VPLS data by given name and encapsulation type.
     *
     * @param name the given name
     * @param encapType the encapsulation type
     */
    private VplsData(String name, EncapsulationType encapType) {
        this.name = name;
        this.encapsulationType = encapType;
        this.interfaces = Sets.newHashSet();
        this.state = VplsState.ADDING;
    }

    /**
     * Creates a VPLS data by given name.
     * The encapsulation type of the VPLS will be NONE.
     *
     * @param name the given name
     * @return the VPLS data
     */
    public static VplsData of(String name) {
        requireNonNull(name);
        return new VplsData(name, EncapsulationType.NONE);
    }

    /**
     * Creates a VPLS data by given name and encapsulation type.
     *
     * @param name the given name
     * @param encapType the encapsulation type
     * @return the VPLS data
     */
    public static VplsData of(String name, EncapsulationType encapType) {
        requireNonNull(name);
        if (encapType == null) {
            return new VplsData(name, EncapsulationType.NONE);
        } else {
            return new VplsData(name, encapType);
        }
    }

    /**
     * Creates a copy of VPLS data.
     *
     * @param vplsData the VPLS data
     * @return the copy of the VPLS data
     */
    public static VplsData of(VplsData vplsData) {
        requireNonNull(vplsData);
        VplsData vplsDataCopy = new VplsData(vplsData.name(), vplsData.encapsulationType());
        vplsDataCopy.state(vplsData.state());
        vplsDataCopy.addInterfaces(vplsData.interfaces());
        return vplsData;
    }

    /**
     * Gets name of the VPLS.
     *
     * @return the name of the VPLS
     */
    public String name() {
        return name;
    }

    public Set<Interface> interfaces() {
        return ImmutableSet.copyOf(interfaces);
    }

    public EncapsulationType encapsulationType() {
        return encapsulationType;
    }

    public void addInterfaces(Collection<Interface> interfaces) {
        requireNonNull(interfaces);
        this.interfaces.addAll(interfaces);
    }

    public void addInterface(Interface iface) {
        requireNonNull(iface);
        this.interfaces.add(iface);
    }

    public void removeInterfaces(Collection<Interface> interfaces) {
        requireNonNull(interfaces);
        this.interfaces.removeAll(interfaces);
    }

    public void removeInterface(Interface iface) {
        requireNonNull(iface);
        this.interfaces.remove(iface);
    }

    public void encapsulationType(EncapsulationType encapType) {
        this.encapsulationType = encapType;
    }

    public VplsState state() {
        return state;
    }

    public void state(VplsState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("interfaces", interfaces)
                .add("encap type", encapsulationType)
                .add("state", state)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VplsData)) {
            return false;
        }
        VplsData other = (VplsData) obj;
        return Objects.equals(other.name, this.name) &&
                Objects.equals(other.interfaces, this.interfaces) &&
                Objects.equals(other.encapsulationType, this.encapsulationType);
    }

    @Override
    public int hashCode() {
        return hash(name, interfaces, encapsulationType);
    }
}
