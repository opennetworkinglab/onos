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

import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.cord.gui.model.BundleFactory.*;
import static org.onosproject.cord.gui.model.XosFunctionDescriptor.*;

/**
 * Unit tests for {@link BundleFactory}.
 */
public class BundleFactoryTest {

    @Test
    public void bundleCount() {
        assertEquals("wrong count", 2, availableBundles().size());
        assertTrue("missing basic", availableBundles().contains(BASIC_BUNDLE));
        assertTrue("missing family", availableBundles().contains(FAMILY_BUNDLE));
    }

    @Test
    public void basicBundle() {
        BundleDescriptor bundle = BundleFactory.BASIC_BUNDLE;
        assertEquals("wrong id", "basic", bundle.id());
        assertEquals("wrong id", "Basic Bundle", bundle.displayName());
        Set<XosFunctionDescriptor> funcs = bundle.functions();
        assertTrue("missing internet", funcs.contains(INTERNET));
        assertTrue("missing firewall", funcs.contains(FIREWALL));
        assertFalse("unexpected url-f", funcs.contains(URL_FILTER));
    }

    @Test
    public void familyBundle() {
        BundleDescriptor bundle = BundleFactory.FAMILY_BUNDLE;
        assertEquals("wrong id", "family", bundle.id());
        assertEquals("wrong id", "Family Bundle", bundle.displayName());
        Set<XosFunctionDescriptor> funcs = bundle.functions();
        assertTrue("missing internet", funcs.contains(INTERNET));
        assertTrue("missing firewall", funcs.contains(FIREWALL));
        assertTrue("missing url-f", funcs.contains(URL_FILTER));
    }

    @Test
    public void bundleFromIdBasic() {
        assertEquals("wrong bundle", BASIC_BUNDLE, bundleFromId("basic"));
    }

    @Test
    public void bundleFromIdFamily() {
        assertEquals("wrong bundle", FAMILY_BUNDLE, bundleFromId("family"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bundleFromIdUnknown() {
        bundleFromId("unknown");
    }
}

