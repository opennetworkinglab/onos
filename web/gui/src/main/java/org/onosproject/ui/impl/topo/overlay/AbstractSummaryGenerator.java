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

package org.onosproject.ui.impl.topo.overlay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of a {@link SummaryGenerator}. Provides convenience
 * methods for compiling a list of properties to be displayed in the summary
 * panel on the UI.
 */
public abstract class AbstractSummaryGenerator implements SummaryGenerator {
    private static final String NUMBER_FORMAT = "#,###";
    private static final DecimalFormat DF = new DecimalFormat(NUMBER_FORMAT);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Prop> props = new ArrayList<>();
    private String iconId;
    private String title;

    /**
     * Constructs a summary generator without specifying the icon ID or title.
     * It is expected that the title (and optionally the icon ID) will be set
     * later via {@link #title(String)} (and {@link #iconId(String)}), before
     * {@link #buildObjectNode()} is invoked to generate the message payload.
     */
    public AbstractSummaryGenerator() {
    }

    /**
     * Constructs a summary generator that uses the specified iconId ID and
     * title in its generated output.
     *
     * @param iconId iconId ID
     * @param title title
     */
    public AbstractSummaryGenerator(String iconId, String title) {
        this.iconId = iconId;
        this.title = title;
    }

    /**
     * Subclasses need to provide an implementation.
     *
     * @return the summary payload
     */
    @Override
    public abstract ObjectNode generateSummary();

    /**
     * Formats the given number into a string, using comma separator.
     *
     * @param number the number
     * @return formatted as a string
     */
    protected String format(Number number) {
        return DF.format(number);
    }

    /**
     * Sets the iconId ID to use.
     *
     * @param iconId iconId ID
     */
    protected void iconId(String iconId) {
        this.iconId = iconId;
    }

    /**
     * Sets the summary panel title.
     *
     * @param title the title
     */
    protected void title(String title) {
        this.title = title;
    }

    /**
     * Clears out the cache of properties.
     */
    protected void clearProps() {
        props.clear();
    }

    /**
     * Adds a property to the summary. Note that the value is converted to
     * a string by invoking the <code>toString()</code> method on it.
     *
     * @param label the label
     * @param value the value
     */
    protected void prop(String label, Object value) {
        props.add(new Prop(label, value));
    }

    /**
     * Adds a separator to the summary; when rendered on the client, a visible
     * break between properties.
     */
    protected void separator() {
        props.add(new Prop("-", ""));
    }

    /**
     * Builds an object node from the current state of the summary generator.
     *
     * @return summary payload as JSON object node
     */
    protected ObjectNode buildObjectNode() {
        ObjectNode result = MAPPER.createObjectNode();
        // NOTE: "id" and "type" are currently used for title and iconID
        //  so that this structure can be "re-used" with detail panel payloads
        result.put("id", title).put("type", iconId);

        ObjectNode pnode = MAPPER.createObjectNode();
        ArrayNode porder = MAPPER.createArrayNode();

        for (Prop p : props) {
            porder.add(p.label);
            pnode.put(p.label, p.value);
        }
        result.set("propOrder", porder);
        result.set("props", pnode);

        return result;
    }

    // ===================================================================

    /**
     * Abstraction of a property, that is, a label-value pair.
     */
    private static class Prop {
        private final String label;
        private final String value;

        public Prop(String label, Object value) {
            this.label = label;
            this.value = value.toString();
        }
    }
}
