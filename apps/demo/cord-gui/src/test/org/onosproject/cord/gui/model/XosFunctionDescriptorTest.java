/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.cord.gui.model.XosFunctionDescriptor.*;

/**
 * Sanity unit tests for {@link XosFunctionDescriptor}.
 */
public class XosFunctionDescriptorTest {

    @Test
    public void numberOfFunctions() {
        assertEquals("unexpected constant count", 3, values().length);
    }

    @Test
    public void internet() {
        assertEquals("wrong id", "internet", INTERNET.id());
        assertEquals("wrong display", "Internet", INTERNET.displayName());
        assertTrue("wrong desc", INTERNET.description().startsWith("Basic"));
    }

    @Test
    public void firewall() {
        assertEquals("wrong id", "firewall", FIREWALL.id());
        assertEquals("wrong display", "Firewall", FIREWALL.displayName());
        assertTrue("wrong desc", FIREWALL.description().startsWith("Normal"));
    }

    @Test
    public void urlFiltering() {
        assertEquals("wrong id", "url_filtering", URL_FILTER.id());
        assertEquals("wrong display", "Parental Control", URL_FILTER.displayName());
        assertTrue("wrong desc", URL_FILTER.description().startsWith("Variable"));
    }
}
