/*
 *  Copyright 2015 Open Networking Laboratory
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

    /** Designates the type of badge. */
    public enum Type {
        INFO("i"),
        WARN("w"),
        ERROR("e"),
        CHECK_MARK("/"),
        X_MARK("X"),
        NUMBER("n");

        private String code;

        Type(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "{" + code + "}";
        }

        /** Returns the type's code in string form. */
        public String code() {
            return code;
        }
    }

    private final Type type;
    private final String message;

    // only instantiated through static methods.
    private NodeBadge(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public String toString() {
        return "{Badge " + type + " \"" + message + "\"}";
    }

    /**
     * Returns the badge type.
     *
     * @return badge type
     */
    public Type type() {
        return type;
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
     * Returns an informational badge, with associated message.
     *
     * @param message the message
     * @return INFO type node badge
     */
    public static NodeBadge info(String message) {
        return new NodeBadge(Type.INFO, nonNull(message));
    }

    /**
     * Returns a warning badge, with associated message.
     *
     * @param message the message
     * @return WARN type node badge
     */
    public static NodeBadge warn(String message) {
        return new NodeBadge(Type.WARN, nonNull(message));
    }

    /**
     * Returns an error badge, with associated message.
     *
     * @param message the message
     * @return ERROR type node badge
     */
    public static NodeBadge error(String message) {
        return new NodeBadge(Type.ERROR, nonNull(message));
    }

    /**
     * Returns a check-mark badge, with associated message.
     *
     * @param message the message
     * @return CHECK_MARK type node badge
     */
    public static NodeBadge checkMark(String message) {
        return new NodeBadge(Type.CHECK_MARK, nonNull(message));
    }

    /**
     * Returns an X-mark badge, with associated message.
     *
     * @param message the message
     * @return X_MARK type node badge
     */
    public static NodeBadge xMark(String message) {
        return new NodeBadge(Type.X_MARK, nonNull(message));
    }

    /**
     * Returns a numeric badge.
     *
     * @param n the number
     * @return NUMBER type node badge
     */
    public static NodeBadge number(int n) {
        // TODO: consider constraints, e.g. 1 <= n <= 99
        return new NodeBadge(Type.NUMBER, Integer.toString(n));
    }

}
