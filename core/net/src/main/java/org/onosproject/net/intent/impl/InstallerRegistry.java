/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.intent.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentInstaller;

import java.util.Map;

/**
 * The local registry for Intent installer.
 */
public class InstallerRegistry {
    private final Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> installers;

    public InstallerRegistry() {
        installers = Maps.newConcurrentMap();
    }

    /**
     * Registers the specific installer for the given intent class.
     *
     * @param cls intent class
     * @param installer intent installer
     * @param <T> the type of intent
     */
    public <T extends Intent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installers.put(cls, installer);
    }

    /**
     * Unregisters the installer for the specific intent class.
     *
     * @param cls intent class
     * @param <T> the type of intent
     */
    public <T extends Intent> void unregisterInstaller(Class<T> cls) {
        installers.remove(cls);
    }

    /**
     * Returns immutable set of binding of currently registered intent installers.
     *
     * @return the set of installer bindings
     */
    public Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> getInstallers() {
        return ImmutableMap.copyOf(installers);
    }

    /**
     * Get an Intent installer by given Intent type.
     *
     * @param cls the Intent type
     * @param <T> the Intent type
     * @return the Intent installer of the Intent type if exists; null otherwise
     */
    public <T extends Intent> IntentInstaller<T> getInstaller(Class<T> cls) {
        return (IntentInstaller<T>) installers.get(cls);
    }
}
