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
package org.onosproject.ui;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.junit.Assert.*;
import static org.onosproject.ui.UiView.Category.OTHER;

/**
 * Tests the default user interface extension descriptor.
 */
public class UiExtensionTest {

    private static final String FOO_ID = "foo";
    private static final String FOO_LABEL = "Foo View";
    private static final String BAR_ID = "bar";
    private static final String BAR_LABEL = "Bar View";
    private static final String CUSTOM = "custom";


    private static final UiView FOO_VIEW = new UiView(OTHER, FOO_ID, FOO_LABEL);
    private static final UiView BAR_VIEW = new UiView(OTHER, BAR_ID, BAR_LABEL);
    private static final UiView HIDDEN_VIEW = new UiViewHidden(FOO_ID);

    private static final UiMessageHandlerFactory MH_FACTORY = () -> null;
    private static final UiTopoOverlayFactory TO_FACTORY = () -> null;

    private final ClassLoader cl = getClass().getClassLoader();

    private List<UiView> viewList;
    private UiExtension ext;
    private String css;
    private String js;
    private UiView view;



    @Test
    public void basics() throws IOException {
        viewList = ImmutableList.of(FOO_VIEW);
        ext = new UiExtension.Builder(cl, viewList).build();

        css = new String(toByteArray(ext.css()));
        assertTrue("incorrect css stream", css.contains("foo-css"));
        js = new String(toByteArray(ext.js()));
        assertTrue("incorrect js stream", js.contains("foo-js"));

        assertEquals("expected 1 view", 1, ext.views().size());
        view = ext.views().get(0);
        assertEquals("wrong view category", OTHER, view.category());
        assertEquals("wrong view id", FOO_ID, view.id());
        assertEquals("wrong view label", FOO_LABEL, view.label());

        assertNull("unexpected message handler factory", ext.messageHandlerFactory());
        assertNull("unexpected topo overlay factory", ext.topoOverlayFactory());
    }

    @Test
    public void withPath() throws IOException {
        viewList = ImmutableList.of(FOO_VIEW);
        ext = new UiExtension.Builder(cl, viewList)
                .resourcePath(CUSTOM)
                .build();

        css = new String(toByteArray(ext.css()));
        assertTrue("incorrect css stream", css.contains("custom-css"));
        js = new String(toByteArray(ext.js()));
        assertTrue("incorrect js stream", js.contains("custom-js"));

        assertEquals("expected 1 view", 1, ext.views().size());
        view = ext.views().get(0);
        assertEquals("wrong view category", OTHER, view.category());
        assertEquals("wrong view id", FOO_ID, view.id());
        assertEquals("wrong view label", FOO_LABEL, view.label());

        assertNull("unexpected message handler factory", ext.messageHandlerFactory());
        assertNull("unexpected topo overlay factory", ext.topoOverlayFactory());
    }

    @Test
    public void messageHandlerFactory() {
        viewList = ImmutableList.of(FOO_VIEW);
        ext = new UiExtension.Builder(cl, viewList)
                .messageHandlerFactory(MH_FACTORY)
                .build();

        assertEquals("wrong message handler factory", MH_FACTORY,
                     ext.messageHandlerFactory());
        assertNull("unexpected topo overlay factory", ext.topoOverlayFactory());
    }

    @Test
    public void topoOverlayFactory() {
        viewList = ImmutableList.of(HIDDEN_VIEW);
        ext = new UiExtension.Builder(cl, viewList)
                .topoOverlayFactory(TO_FACTORY)
                .build();

        assertNull("unexpected message handler factory", ext.messageHandlerFactory());
        assertEquals("wrong topo overlay factory", TO_FACTORY,
                     ext.topoOverlayFactory());
    }

    @Test
    public void twoViews() {
        viewList = ImmutableList.of(FOO_VIEW, BAR_VIEW);
        ext = new UiExtension.Builder(cl, viewList).build();

        assertEquals("expected 2 views", 2, ext.views().size());

        view = ext.views().get(0);
        assertEquals("wrong view category", OTHER, view.category());
        assertEquals("wrong view id", FOO_ID, view.id());
        assertEquals("wrong view label", FOO_LABEL, view.label());

        view = ext.views().get(1);
        assertEquals("wrong view category", OTHER, view.category());
        assertEquals("wrong view id", BAR_ID, view.id());
        assertEquals("wrong view label", BAR_LABEL, view.label());
    }
}