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
package org.onosproject.ovsdb.rfc.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Version utility class tests.
 */
public class VersionUtilTest {

    @Test
    public void testVersionCompare() {
        assertThat(VersionUtil.versionCompare("1.2.3", null), lessThan(0));
        assertThat(VersionUtil.versionCompare(null, "1.2.3"), lessThan(0));

        assertThat(VersionUtil.versionCompare("1.2.x", "1.2.3"), lessThan(0));
        assertThat(VersionUtil.versionCompare("1.2.3", "1.2.y"), lessThan(0));

        assertThat(VersionUtil.versionCompare("1", "1.2.3"), lessThan(0));
        assertThat(VersionUtil.versionCompare("1.2", "1.2.3"), lessThan(0));
        assertThat(VersionUtil.versionCompare("1.2.3.4", "1.2.3"), lessThan(0));

        assertThat(VersionUtil.versionCompare("1.2.3", "1.2.3"), is(0));
        assertThat(VersionUtil.versionCompare("2.2.3", "1.2.3"), greaterThan(0));
        assertThat(VersionUtil.versionCompare("1.2.3", "2.2.3"), lessThan(0));
    }
}