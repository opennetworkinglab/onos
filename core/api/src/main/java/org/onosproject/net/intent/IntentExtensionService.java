/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Map;

/**
 * Service for extending the capability of intent framework by
 * adding additional compilers or/and installers.
 */
@Beta
public interface IntentExtensionService {
    /**
     * Registers the specified compiler for the given intent class.
     *
     * @param cls      intent class
     * @param compiler intent compiler
     * @param <T>      the type of intent
     */
    <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler);

    /**
     * Unregisters the compiler for the specified intent class.
     *
     * @param cls intent class
     * @param <T> the type of intent
     */
    <T extends Intent> void unregisterCompiler(Class<T> cls);

    /**
     * Returns immutable set of bindings of currently registered intent compilers.
     *
     * @return the set of compiler bindings
     */
    Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers();
}
