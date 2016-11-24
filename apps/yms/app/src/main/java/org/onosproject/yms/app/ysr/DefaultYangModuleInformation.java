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

package org.onosproject.yms.app.ysr;

import com.google.common.collect.ImmutableList;
import org.onosproject.yangutils.datamodel.YangNamespace;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of default YANG module information.
 */
class DefaultYangModuleInformation implements YangModuleInformation {

    private final YangModuleIdentifier moduleIdentifier;
    private final YangNamespace nameSpace;
    private final List<String> features;
    private final List<YangModuleIdentifier> subModuleIdentifiers;

    /**
     * Creates an instance of YANG module information.
     *
     * @param moduleIdentifier module identifier
     * @param nameSpace        name space of module
     */
    DefaultYangModuleInformation(YangModuleIdentifier moduleIdentifier,
                                 YangNamespace nameSpace) {
        this.moduleIdentifier = moduleIdentifier;
        this.nameSpace = nameSpace;
        subModuleIdentifiers = new ArrayList<>();
        features = new ArrayList<>();
    }

    @Override
    public YangModuleIdentifier moduleIdentifier() {
        return moduleIdentifier;
    }

    public YangNamespace namespace() {
        return nameSpace;
    }

    @Override
    public List<String> featureList() {
        return ImmutableList.copyOf(features);
    }

    @Override
    public List<YangModuleIdentifier> subModuleIdentifiers() {
        return ImmutableList.copyOf(subModuleIdentifiers);
    }

    /**
     * Adds to YANG sub module identifier list.
     *
     * @param subModuleIdentifier YANG sub module identifier
     */
    void addSubModuleIdentifiers(YangModuleIdentifier subModuleIdentifier) {
        subModuleIdentifiers.add(subModuleIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleIdentifier, subModuleIdentifiers, nameSpace, features);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultYangModuleInformation) {
            DefaultYangModuleInformation that = (DefaultYangModuleInformation) obj;
            return Objects.equals(moduleIdentifier, that.moduleIdentifier) &&
                    Objects.equals(nameSpace, that.nameSpace) &&
                    Objects.equals(features, that.features) &&
                    Objects.equals(subModuleIdentifiers, that.subModuleIdentifiers);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("yangModuleIdentifier", moduleIdentifier)
                .add("nameSpace", nameSpace)
                .add("features", features)
                .add("yangModuleIdentifiers", subModuleIdentifiers)
                .toString();
    }
}
