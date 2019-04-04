/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.ofoverlay.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for OVS Version.
 */
public final class OvsVersion {
    protected static final Logger log = LoggerFactory.getLogger(OvsVersion.class);

    private int[] versionElements = new int[] {0, 0, 0, Integer.MAX_VALUE};
    private int depth = 0;

    /**
     * Constructor for OvsVersion.
     * @param ovsVerStr OVS version string
     */
    private OvsVersion(String ovsVerStr) {

        // Supporting
        // 1.2.3 (depth = 3, public release)
        // 1.2.3.4 (depth = 4, beta release)
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(\\.(\\d+))?").matcher(ovsVerStr);

        if (!m.matches()) {
            throw new IllegalArgumentException("Malformed OVS version");
        }

        versionElements[0] = Integer.parseInt(m.group(1));
        versionElements[1] = Integer.parseInt(m.group(2));
        versionElements[2] = Integer.parseInt(m.group(3));

        if (m.group(4) == null) {
            depth = 3;
            return;
        }

        versionElements[3] = Integer.parseInt(m.group(5));
        depth = 4;
    }

    /**
     * Builder for OvsVersion.
     * @param ovsVersionStr OVS version string
     * @return ovs version
     */
    public static OvsVersion build(String ovsVersionStr) {
        try {
            return new OvsVersion(ovsVersionStr);
        } catch (IllegalArgumentException e) {
            log.error("Exception Occurred {}", e);
            return null;
        }
    }

    private int get(int level) {
        return versionElements[level];
    }

    private int compare(OvsVersion tgt) {
        //Comparison example
        // 2.7.0 < 2.7.2
        // 2.7.0 > 2.6.9.12
        // 2.7.0 > 2.7.0.0 (because 2.7.0 is public release)
        for (int i = 0; i < versionElements.length; i++) {
            if (versionElements[i] == tgt.get(i)) {
                continue;
            } else if (versionElements[i] < tgt.get(i)) {
                return (i + 1) * -1;
            } else {
                return (i + 1);
            }
        }
        return 0;
    }

    /**
     * Returns whether this OVS version is equal to the target OVS version.
     * @param tgt taret OVS version
     * @return whether this OVS version is equal to the target OVS version
     */
    public boolean isEqOf(OvsVersion tgt) {
        return (compare(tgt) == 0);
    }

    /**
     * Returns whether this OVS version is prior to the target OVS version.
     * @param tgt taret OVS version
     * @return whether this OVS version is prior to the target OVS version
     */
    public boolean isPriorOf(OvsVersion tgt) {
        return (compare(tgt) < 0);
    }

    /**
     * Returns whether this OVS version is prior or equal to the target OVS version.
     * @param tgt taret OVS version
     * @return whether this OVS version is prior or equal to the target OVS version
     */
    public boolean isPriorOrEqOf(OvsVersion tgt) {
        return (compare(tgt) <= 0);
    }

    /**
     * Returns whether this OVS version is later to the target OVS version.
     * @param tgt taret OVS version
     * @return whether this OVS version is later to the target OVS version
     */
    public boolean isLaterOf(OvsVersion tgt) {
        return (compare(tgt) > 0);
    }

    /**
     * Returns whether this OVS version is later or equal to the target OVS version.
     * @param tgt taret OVS version
     * @return whether this OVS version is later or equal to the target OVS version
     */
    public boolean isLaterOrEqOf(OvsVersion tgt) {
        return (compare(tgt) >= 0);
    }

    @Override
    public String toString() {

        StringBuilder strbuild = new StringBuilder();
        strbuild.append(versionElements[0]);

        for (int i = 1; i < depth; i++) {
            strbuild.append(".");
            strbuild.append(versionElements[i]);
        }
        return strbuild.toString();
    }

}