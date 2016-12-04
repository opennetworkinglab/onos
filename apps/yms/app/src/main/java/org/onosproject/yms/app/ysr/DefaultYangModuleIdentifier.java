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

import org.onosproject.yms.ysr.YangModuleIdentifier;

import java.util.Comparator;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of default YANG module identifier.
 */
public class DefaultYangModuleIdentifier implements YangModuleIdentifier,
        Comparator<YangModuleIdentifier> {

    private final String moduleName;
    private final String revision;

    /**
     * Creates an instance of YANG module identifier.
     *
     * @param moduleName module's name
     * @param revision   module's revision
     */
    DefaultYangModuleIdentifier(String moduleName, String revision) {
        this.moduleName = moduleName;
        this.revision = revision;
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public String revision() {
        return revision;
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, revision);
    }

    @Override
    public int compare(YangModuleIdentifier id1, YangModuleIdentifier id2) {
        int compare = id1.moduleName().compareTo(id2.moduleName());
        if (compare != 0) {
            return compare;
        }
        return id1.revision().compareTo(id2.revision());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultYangModuleIdentifier) {
            DefaultYangModuleIdentifier that = (DefaultYangModuleIdentifier) obj;
            return Objects.equals(moduleName, that.moduleName) &&
                    Objects.equals(revision, that.revision);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("moduleName", moduleName)
                .add("revision", revision)
                .toString();
    }
}
