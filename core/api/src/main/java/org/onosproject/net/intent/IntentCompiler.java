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

import com.google.common.annotations.Beta;

import java.util.List;

/**
 * Abstraction of a compiler which is capable of taking an intent
 * and translating it to other, potentially installable, intents.
 *
 * @param <T> the type of intent
 */
@Beta
public interface IntentCompiler<T extends Intent> {
    /**
     * Compiles the specified intent into other intents.
     *
     * @param intent      intent to be compiled
     * @param installable previous compilation result; optional
     * @return list of resulting intents
     * @throws IntentException if issues are encountered while compiling the intent
     */
    List<Intent> compile(T intent, List<Intent> installable);

}
