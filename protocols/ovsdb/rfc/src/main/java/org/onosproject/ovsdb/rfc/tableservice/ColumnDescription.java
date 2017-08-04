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
package org.onosproject.ovsdb.rfc.tableservice;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.ovsdb.rfc.table.VersionNum;
import org.onosproject.ovsdb.rfc.utils.VersionUtil;

/**
 * Column description.
 */
public class ColumnDescription {

    // The column name
    private final String name;
    // The method name
    private final String method;
    // The initial version
    private final String fromVersion;
    // The end of the version
    private final String untilVersion;

    /**
     * Constructs a MonitorRequest object.
     * @param name column name
     * @param method method name
     */
    public ColumnDescription(String name, String method) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(method, "method cannot be null");
        this.name = name;
        this.method = method;
        this.fromVersion = VersionUtil.DEFAULT_VERSION_STRING;
        this.untilVersion = VersionUtil.DEFAULT_VERSION_STRING;
    }

    /**
     * Constructs a MonitorRequest object.
     * @param name column name
     * @param method method name
     * @param fromVersion the initial version
     */
    public ColumnDescription(String name, String method, VersionNum fromVersion) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(method, "method cannot be null");
        checkNotNull(fromVersion, "the initial version cannot be null");
        this.name = name;
        this.method = method;
        this.fromVersion = fromVersion.versionNum();
        this.untilVersion = VersionUtil.DEFAULT_VERSION_STRING;
    }

    /**
     * Constructs a MonitorRequest object.
     * @param name column name
     * @param method method name
     * @param fromVersion the initial version
     * @param untilVersion the end of the version
     */
    public ColumnDescription(String name, String method, VersionNum fromVersion,
                             VersionNum untilVersion) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(method, "method cannot be null");
        checkNotNull(fromVersion, "the initial version cannot be null");
        checkNotNull(untilVersion, "the end of the version cannot be null");
        this.name = name;
        this.method = method;
        this.fromVersion = fromVersion.versionNum();
        this.untilVersion = untilVersion.versionNum();
    }

    /**
     * Returns the column name.
     * @return the column name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the method name.
     * @return the method name
     */
    public String method() {
        return method;
    }

    /**
     * Returns the initial version.
     * @return the initial version
     */
    public String fromVersion() {
        return fromVersion;
    }

    /**
     * Returns the end of the version.
     * @return the end of the version
     */
    public String untilVersion() {
        return untilVersion;
    }
}
