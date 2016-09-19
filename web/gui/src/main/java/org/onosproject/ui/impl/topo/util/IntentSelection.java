/*
 *  Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.impl.topo.util;

import org.onosproject.net.intent.Intent;
import org.onosproject.ui.topo.NodeSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a selection of intents (paths) inferred from a selection
 * of devices and/or hosts from the topology view.
 */
public class IntentSelection {

    private static final int ALL = -1;

    protected static final Logger log =
            LoggerFactory.getLogger(IntentSelection.class);

    private final NodeSelection nodes;

    private final List<Intent> intents;
    private int index = ALL;

    /**
     * Creates an intent selection group, based on selected nodes.
     *
     * @param nodes node selection
     * @param filter intent filter
     */
    public IntentSelection(NodeSelection nodes, TopoIntentFilter filter) {
        this.nodes = nodes;
        intents = filter.findPathIntents(
                nodes.hostsWithHover(),
                nodes.devicesWithHover(),
                nodes.linksWithHover());
        if (intents.size() == 1) {
            index = 0;  // pre-select a single intent
        }
    }

    /**
     * Creates an intent selection group, for a single intent.
     *
     * @param intent the intent
     */
    public IntentSelection(Intent intent) {
        nodes = null;
        intents = new ArrayList<>(1);
        intents.add(intent);
        index = 0;
    }

    /**
     * Returns true if no intents are selected.
     *
     * @return true if nothing selected
     */
    public boolean none() {
        return intents.isEmpty();
    }

    /**
     * Returns true if all intents in this select group are currently selected.
     * This is the initial state, so that all intents are shown on the
     * topology view with primary highlighting.
     *
     * @return true if all selected
     */
    public boolean all() {
        return index == ALL;
    }

    /**
     * Returns true if there is a single intent in this select group, or if
     * a specific intent has been marked (index != ALL).
     *
     * @return true if single intent marked
     */
    public boolean single() {
        return !all();
    }

    /**
     * Returns the number of intents in this selection group.
     *
     * @return number of intents
     */
    public int size() {
        return intents.size();
    }

    /**
     * Returns the index of the currently selected intent.
     *
     * @return the current index
     */
    public int index() {
        return index;
    }

    /**
     * The list of intents in this selection group.
     *
     * @return list of intents
     */
    public List<Intent> intents() {
        return Collections.unmodifiableList(intents);
    }

    /**
     * Marks and returns the next intent in this group. Note that the
     * selection wraps around to the beginning again, if necessary.
     *
     * @return the next intent in the group
     */
    public Intent next() {
        index += 1;
        if (index >= intents.size()) {
            index = 0;
        }
        return intents.get(index);
    }

    /**
     * Marks and returns the previous intent in this group. Note that the
     * selection wraps around to the end again, if necessary.
     *
     * @return the previous intent in the group
     */
    public Intent prev() {
        index -= 1;
        if (index < 0) {
            index = intents.size() - 1;
        }
        return intents.get(index);
    }

    /**
     * Returns the currently marked intent, or null if "all" intents
     * are marked.
     *
     * @return the currently marked intent
     */
    public Intent current() {
        return all() ? null : intents.get(index);
    }

    @Override
    public String toString() {
        return "IntentSelection{" +
                "nodes=" + nodes +
                ", #intents=" + intents.size() +
                ", index=" + index +
                '}';
    }

}
