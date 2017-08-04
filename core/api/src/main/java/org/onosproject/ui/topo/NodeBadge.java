/*
 *  Copyright 2015-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.topo;

/**
 * Designates a badge to be applied to a node in the topology view.
 */
public final class NodeBadge {

    private static final String EMPTY = "";

    /** Designates the badge status. */
    public enum Status {
        INFO("i"),
        WARN("w"),
        ERROR("e");

        private String code;

        Status(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "{" + code + "}";
        }

        /* Returns the status code in string form. */
        public String code() {
            return code;
        }
    }

    private final Status status;
    private final boolean isGlyph;
    private final String text;
    private final String message;

    // only instantiated through static methods.
    private NodeBadge(Status status, boolean isGlyph, String text, String message) {
        this.status = status == null ? Status.INFO : status;
        this.isGlyph = isGlyph;
        this.text = text;
        this.message = message;
    }

    @Override
    public String toString() {
        return "{Badge " + status +
                " (" + text + ")" +
                (isGlyph ? "*G " : " ") +
                "\"" + message + "\"}";
    }

    /**
     * Returns the badge status.
     *
     * @return badge status
     */
    public Status status() {
        return status;
    }

    /**
     * Returns true if the text for this badge designates a glyph ID.
     *
     * @return true if badge uses glyph
     */
    public boolean isGlyph() {
        return isGlyph;
    }

    /**
     * Returns the text for the badge.
     * Note that if {@link #isGlyph} is true, the text is a glyph ID, otherwise
     * the text is displayed verbatim in the badge.
     *
     * @return text for badge
     */
    public String text() {
        return text;
    }

    /**
     * Returns the message associated with the badge.
     *
     * @return associated message
     */
    public String message() {
        return message;
    }

    private static String nonNull(String s) {
        return s == null ? EMPTY : s;
    }

    /**
     * Returns an arbitrary text badge, with default status.
     *
     * @param txt the text
     * @return node badge to display text
     */
    public static NodeBadge text(String txt) {
        // TODO: consider length constraint on txt (3 chars?)
        return new NodeBadge(Status.INFO, false, nonNull(txt), null);
    }

    /**
     * Returns a glyph badge, with default status.
     *
     * @param gid the glyph ID
     * @return node badge to display glyph
     */
    public static NodeBadge glyph(String gid) {
        return new NodeBadge(Status.INFO, true, nonNull(gid), null);
    }

    /**
     * Returns a numeric badge, with default status.
     *
     * @param n the number
     * @return node badge to display a number
     */
    public static NodeBadge number(int n) {
        // TODO: consider constraints, e.g. 1 <= n <= 999
        return new NodeBadge(Status.INFO, false, Integer.toString(n), null);
    }

    /**
     * Returns an arbitrary text badge, with the given status.
     *
     * @param s the status
     * @param txt the text
     * @return node badge to display text
     */
    public static NodeBadge text(Status s, String txt) {
        // TODO: consider length constraint on txt (3 chars?)
        return new NodeBadge(s, false, nonNull(txt), null);
    }

    /**
     * Returns a glyph badge, with the given status.
     *
     * @param s the status
     * @param gid the glyph ID
     * @return node badge to display glyph
     */
    public static NodeBadge glyph(Status s, String gid) {
        return new NodeBadge(s, true, nonNull(gid), null);
    }


    /**
     * Returns a numeric badge, with the given status and optional message.
     *
     * @param s the status
     * @param n the number
     * @return node badge to display a number
     */
    public static NodeBadge number(Status s, int n) {
        // TODO: consider constraints, e.g. 1 <= n <= 999
        return new NodeBadge(s, false, Integer.toString(n), null);
    }

    /**
     * Returns an arbitrary text badge, with the given status and optional
     * message.
     *
     * @param s the status
     * @param txt the text
     * @param msg the optional message
     * @return node badge to display text
     */
    public static NodeBadge text(Status s, String txt, String msg) {
        // TODO: consider length constraint on txt (3 chars?)
        return new NodeBadge(s, false, nonNull(txt), msg);
    }

    /**
     * Returns a glyph badge, with the given status and optional message.
     *
     * @param s the status
     * @param gid the glyph ID
     * @param msg the optional message
     * @return node badge to display glyph
     */
    public static NodeBadge glyph(Status s, String gid, String msg) {
        return new NodeBadge(s, true, nonNull(gid), msg);
    }


    /**
     * Returns a numeric badge, with the given status and optional message.
     *
     * @param s the status
     * @param n the number
     * @param msg the optional message
     * @return node badge to display a number
     */
    public static NodeBadge number(Status s, int n, String msg) {
        // TODO: consider constraints, e.g. 1 <= n <= 999
        return new NodeBadge(s, false, Integer.toString(n), msg);
    }

}
