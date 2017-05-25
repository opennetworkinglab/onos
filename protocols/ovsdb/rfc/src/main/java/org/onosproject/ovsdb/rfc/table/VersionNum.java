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
package org.onosproject.ovsdb.rfc.table;

/**
 * The version number of tables and columns.
 */
public enum VersionNum {
    VERSION100("1.0.0"), VERSION102("1.0.2"), VERSION103("1.0.3"),
    VERSION104("1.0.4"), VERSION106("1.0.6"), VERSION110("1.1.0"),
    VERSION130("1.3.0"), VERSION200("2.0.0"), VERSION300("3.0.0"),
    VERSION330("3.3.0"), VERSION350("3.5.0"), VERSION400("4.0.0"),
    VERSION510("5.1.0"), VERSION520("5.2.0"), VERSION600("6.0.0"),
    VERSION610("6.1.0"), VERSION620("6.2.0"), VERSION630("6.3.0"),
    VERSION640("6.4.0"), VERSION650("6.5.0"), VERSION660("6.6.0"),
    VERSION670("6.7.0"), VERSION680("6.8.0"), VERSION690("6.9.0"),
    VERSION6100("6.10.0"), VERSION6111("6.11.1"), VERSION710("7.1.0"),
    VERSION720("7.2.0"), VERSION721("7.2.1"), VERSION730("7.3.0"),
    VERSION740("7.4.0"), VERSION750("7.5.0"), VERSION760("7.6.0"),
    VERSION770("7.7.0"), VERSION780("7.8.0"), VERSION790("7.9.0"),
    VERSION7100("7.10.0"), VERSION7110("7.11.0"), VERSION7120("7.12.0"),
    VERSION7130("7.13.0"), VERSION7140("7.14.0");

    private final String versionNum;

    private VersionNum(String versionNum) {
        this.versionNum = versionNum;
    }

    /**
     * Returns the version number for VersionNum.
     * @return the version number
     */
    public String versionNum() {
        return versionNum;
    }
}
