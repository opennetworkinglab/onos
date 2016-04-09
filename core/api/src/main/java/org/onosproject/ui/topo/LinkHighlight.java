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

package org.onosproject.ui.topo;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Denotes the highlighting to be applied to a link.
 * {@link Flavor} is a closed set of NO-, PRIMARY-, or SECONDARY- highlighting.
 * {@link Mod} is an open ended set of additional modifications (CSS classes)
 * that may also be applied.
 * Note that {@link #MOD_OPTICAL} and {@link #MOD_ANIMATED} are pre-defined mods.
 * Label text may be set, which will also be displayed on the link.
 */
public class LinkHighlight extends AbstractHighlight {

    private static final String PLAIN = "plain";
    private static final String PRIMARY = "primary";
    private static final String SECONDARY = "secondary";
    private static final String EMPTY = "";
    private static final String SPACE = " ";

    private final Flavor flavor;
    private final Set<Mod> mods = new TreeSet<>();
    private String label = EMPTY;

    /**
     * Constructs a link highlight entity.
     *
     * @param linkId the link identifier
     * @param flavor the highlight flavor
     */
    public LinkHighlight(String linkId, Flavor flavor) {
        super(TopoElementType.LINK, linkId);
        this.flavor = checkNotNull(flavor);
    }

    /**
     * Adds a highlighting modification to this link highlight.
     *
     * @param mod mod to be added
     * @return self, for chaining
     */
    public LinkHighlight addMod(Mod mod) {
        mods.add(checkNotNull(mod));
        return this;
    }

    /**
     * Adds a label to be displayed on the link.
     *
     * @param label the label text
     * @return self, for chaining
     */
    public LinkHighlight setLabel(String label) {
        this.label = label == null ? EMPTY : label;
        return this;
    }

    /**
     * Returns the highlight flavor.
     *
     * @return highlight flavor
     */
    public Flavor flavor() {
        return flavor;
    }

    /**
     * Returns the highlight modifications.
     *
     * @return highlight modifications
     */
    public Set<Mod> mods() {
        return Collections.unmodifiableSet(mods);
    }

    /**
     * Generates the CSS classes string from the {@link #flavor} and
     * any optional {@link #mods}.
     *
     * @return CSS classes string
     */
    public String cssClasses() {
        StringBuilder sb = new StringBuilder(flavor.toString());
        mods.forEach(m -> sb.append(SPACE).append(m));
        return sb.toString();
    }

    /**
     * Returns the label text.
     *
     * @return label text
     */
    public String label() {
        return label;
    }

    /**
     * Link highlighting flavor.
     */
    public enum Flavor {
        NO_HIGHLIGHT(PLAIN),
        PRIMARY_HIGHLIGHT(PRIMARY),
        SECONDARY_HIGHLIGHT(SECONDARY);

        private String cssName;

        Flavor(String s) {
            cssName = s;
        }

        @Override
        public String toString() {
            return cssName;
        }
    }

    /**
     * Denotes a link to be tagged as an optical link.
     */
    public static final Mod MOD_OPTICAL = new Mod("optical");

    /**
     * Denotes a link to be tagged with animated traffic ("marching ants").
     */
    public static final Mod MOD_ANIMATED = new Mod("animated");
}
