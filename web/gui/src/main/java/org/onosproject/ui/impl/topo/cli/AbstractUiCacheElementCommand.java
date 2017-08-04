/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.ui.impl.topo.cli;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ui.model.topo.UiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for model cache CLI commands.
 */
abstract class AbstractUiCacheElementCommand extends AbstractShellCommand {

    /**
     * Built in comparator for elements.
     */
    private static final Comparator<UiElement> ELEMENT_COMPARATOR =
            (o1, o2) -> o1.idAsString().compareTo(o2.idAsString());

    /**
     * Returns the given elements in a list, sorted by string representation
     * of the identifiers.
     *
     * @param elements the elements to sort
     * @return the sorted elements
     */
    protected List<UiElement> sorted(Set<? extends UiElement> elements) {
        List<UiElement> list = new ArrayList<>(elements);
        Collections.sort(list, ELEMENT_COMPARATOR);
        return list;
    }
}
