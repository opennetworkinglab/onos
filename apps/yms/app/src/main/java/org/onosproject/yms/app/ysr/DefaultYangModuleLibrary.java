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
import org.onosproject.yms.ysr.YangModuleInformation;
import org.onosproject.yms.ysr.YangModuleLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of default YANG module library.
 */
public class DefaultYangModuleLibrary implements YangModuleLibrary {

    private final String moduleSetId;
    private final List<YangModuleInformation> moduleInformation;

    /**
     * Creates an instance of YANG module library.
     *
     * @param moduleSetId module id
     */
    public DefaultYangModuleLibrary(String moduleSetId) {
        this.moduleSetId = moduleSetId;
        moduleInformation = new ArrayList<>();
    }

    @Override
    public String moduleSetId() {
        return moduleSetId;
    }

    @Override
    public List<YangModuleInformation> yangModuleList() {
        return ImmutableList.copyOf(moduleInformation);
    }

    /**
     * Adds module information.
     *
     * @param information module information
     */
    void addModuleInformation(YangModuleInformation information) {
        moduleInformation.add(information);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleInformation, moduleSetId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultYangModuleLibrary) {
            DefaultYangModuleLibrary that = (DefaultYangModuleLibrary) obj;
            return Objects.equals(moduleInformation, that.moduleInformation) &&
                    Objects.equals(moduleSetId, that.moduleSetId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("moduleInformation", moduleInformation)
                .add("moduleId", moduleSetId)
                .toString();
    }
}
