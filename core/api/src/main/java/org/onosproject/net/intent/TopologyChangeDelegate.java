/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent;

/**
 * Auxiliary delegate for integration of intent manager and flow trackerService.
 */
public interface TopologyChangeDelegate {

    /**
     * Notifies that topology has changed in such a way that the specified
     * intents should be recompiled. If the {@code compileAllFailed} parameter
     * is true, then all intents in {@link org.onosproject.net.intent.IntentState#FAILED}
     * state should be compiled as well.
     *
     * @param intentIds intents that should be recompiled
     * @param compileAllFailed true implies full compile of all failed intents
     *                         is required; false for selective recompile only
     */
    void triggerCompile(Iterable<Key> intentIds, boolean compileAllFailed);

}
