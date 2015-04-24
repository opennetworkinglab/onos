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
 */
package org.onosproject.ui;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.junit.Assert.*;
import static org.onosproject.ui.UiView.Category.OTHER;

/**
 * Tests the default user interface extension descriptor.
 */
public class UiExtensionTest {

    @Test
    public void basics() throws IOException {
        UiExtension ext = new UiExtension(ImmutableList.of(new UiView(OTHER, "foo", "Foo View")),
                                          null,
                                          getClass().getClassLoader());
        String css = new String(toByteArray(ext.css()));
        assertTrue("incorrect css stream", css.contains("foo-css"));
        String js = new String(toByteArray(ext.js()));
        assertTrue("incorrect js stream", js.contains("foo-js"));
        assertEquals("incorrect view id", "foo", ext.views().get(0).id());
        assertEquals("incorrect view category", OTHER, ext.views().get(0).category());
        assertNull("incorrect handler factory", ext.messageHandlerFactory());
    }

    @Test
    public void withPath() throws IOException {
        UiExtension ext = new UiExtension(ImmutableList.of(new UiView(OTHER, "foo", "Foo View")),
                                          null, "custom", getClass().getClassLoader());
        String css = new String(toByteArray(ext.css()));
        assertTrue("incorrect css stream", css.contains("custom-css"));
        String js = new String(toByteArray(ext.js()));
        assertTrue("incorrect js stream", js.contains("custom-js"));
        assertEquals("incorrect view id", "foo", ext.views().get(0).id());
        assertNull("incorrect handler factory", ext.messageHandlerFactory());
    }
}