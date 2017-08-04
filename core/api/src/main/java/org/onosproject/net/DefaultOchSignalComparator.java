/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net;

import java.util.Comparator;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Comparator implementation for OchSignal. Assumes identical grid type and channel spacing.
 */
public class DefaultOchSignalComparator implements Comparator<OchSignal> {

    private static final DefaultOchSignalComparator INSTANCE = new DefaultOchSignalComparator();

    /**
     * Creates a new instance of {@link TreeSet} using this Comparator.
     * @return {@link TreeSet}
     */
    public static TreeSet<OchSignal> newOchSignalTreeSet() {
        return new TreeSet<>(INSTANCE);
    }

    @Override
    public int compare(OchSignal o1, OchSignal o2) {
        checkNotNull(o1.gridType());
        checkNotNull(o1.channelSpacing());

        checkArgument(o1.gridType().equals(o2.gridType()));
        checkArgument(o1.channelSpacing().equals(o2.channelSpacing()));

        return o1.spacingMultiplier() * o1.slotGranularity() - o2.spacingMultiplier() * o2.slotGranularity();
    }
}
