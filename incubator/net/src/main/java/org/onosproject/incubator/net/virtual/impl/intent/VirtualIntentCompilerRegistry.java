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

package org.onosproject.incubator.net.virtual.impl.intent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.intent.VirtualIntentCompiler;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class VirtualIntentCompilerRegistry {
    private final ConcurrentMap<Class<? extends Intent>,
                VirtualIntentCompiler<? extends Intent>> compilers = new ConcurrentHashMap<>();

    // non-instantiable (except for our Singleton)
    private VirtualIntentCompilerRegistry() {

    }

    public static VirtualIntentCompilerRegistry getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Registers the specified compiler for the given intent class.
     *
     * @param cls      intent class
     * @param compiler intent compiler
     * @param <T>      the type of intent
     */
    public <T extends Intent> void registerCompiler(Class<T> cls,
                                                    VirtualIntentCompiler<T> compiler) {
        compilers.put(cls, compiler);
    }

    /**
     * Unregisters the compiler for the specified intent class.
     *
     * @param cls intent class
     * @param <T> the type of intent
     */
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilers.remove(cls);
    }

    /**
     * Returns immutable set of bindings of currently registered intent compilers.
     *
     * @return the set of compiler bindings
     */
    public Map<Class<? extends Intent>, VirtualIntentCompiler<? extends Intent>> getCompilers() {
        return ImmutableMap.copyOf(compilers);
    }

    /**
     * Compiles an intent recursively.
     *
     * @param networkId network identifier
     * @param intent intent
     * @param previousInstallables previous intent installables
     * @return result of compilation
     */
    public List<Intent> compile(NetworkId networkId,
                         Intent intent, List<Intent> previousInstallables) {
        if (intent.isInstallable()) {
            return ImmutableList.of(intent);
        }

        // FIXME: get previous resources
        List<Intent> installables = new ArrayList<>();
        Queue<Intent> compileQueue = new LinkedList<>();
        compileQueue.add(intent);

        Intent compiling;
        while ((compiling = compileQueue.poll()) != null) {
            registerSubclassCompilerIfNeeded(compiling);

            List<Intent> compiled = getCompiler(compiling)
                    .compile(networkId, compiling, previousInstallables);

            compiled.forEach(i -> {
                if (i.isInstallable()) {
                    installables.add(i);
                } else {
                    compileQueue.add(i);
                }
            });
        }
        return installables;
    }

    /**
     * Returns the corresponding intent compiler to the specified intent.
     *
     * @param intent intent
     * @param <T>    the type of intent
     * @return intent compiler corresponding to the specified intent
     */
    private <T extends Intent> VirtualIntentCompiler<T> getCompiler(T intent) {
        @SuppressWarnings("unchecked")
        VirtualIntentCompiler<T> compiler =
                (VirtualIntentCompiler<T>) compilers.get(intent.getClass());
        if (compiler == null) {
            throw new IntentException("no compiler for class " + intent.getClass());
        }
        return compiler;
    }

    /**
     * Registers an intent compiler of the specified intent if an intent compiler
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent compiler for a parent type is found, this method
     * registers the found intent compiler.
     *
     * @param intent intent
     */
    private void registerSubclassCompilerIfNeeded(Intent intent) {
        if (!compilers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    VirtualIntentCompiler<?> compiler = compilers.get(cls);
                    if (compiler != null) {
                        compilers.put(intent.getClass(), compiler);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG =
                "Should not instantiate this class.";
        private static final VirtualIntentCompilerRegistry INSTANCE =
                new VirtualIntentCompilerRegistry();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
