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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fake implementation of the intent service to assist in developing tests of
 * the interface contract.
 */
public class FakeIntentManager implements TestableIntentService {

    private final Map<Key, Intent> intents = new HashMap<>();
    private final Map<Key, IntentState> intentStates = new HashMap<>();
    private final Map<Key, List<Intent>> installables = new HashMap<>();
    private final Set<IntentListener> listeners = new HashSet<>();

    private final Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> compilers = new HashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<IntentException> exceptions = new ArrayList<>();

    @Override
    public List<IntentException> getExceptions() {
        return exceptions;
    }

    // Provides an out-of-thread simulation of intent submit life-cycle
    private void executeSubmit(final Intent intent) {
        registerSubclassCompilerIfNeeded(intent);
        executor.execute(() -> {
            try {
                executeCompilingPhase(intent);
            } catch (IntentException e) {
                exceptions.add(e);
            }
        });
    }

    // Provides an out-of-thread simulation of intent withdraw life-cycle
    private void executeWithdraw(final Intent intent) {
        executor.execute(() -> {
            try {
                List<Intent> installable = getInstallable(intent.key());
                executeWithdrawingPhase(intent, installable);
            } catch (IntentException e) {
                exceptions.add(e);
            }
        });
    }

    private <T extends Intent> IntentCompiler<T> getCompiler(T intent) {
        @SuppressWarnings("unchecked")
        IntentCompiler<T> compiler = (IntentCompiler<T>) compilers.get(intent.getClass());
        if (compiler == null) {
            throw new IntentException("no compiler for class " + intent.getClass());
        }
        return compiler;
    }

    private <T extends Intent> void executeCompilingPhase(T intent) {
        setState(intent, IntentState.COMPILING);
        try {
            // For the fake, we compile using a single level pass
            List<Intent> installable = new ArrayList<>();
            for (Intent compiled : getCompiler(intent).compile(intent, null)) {
                installable.add(compiled);
            }
            executeInstallingPhase(intent, installable);

        } catch (IntentException e) {
            setState(intent, IntentState.FAILED);
            dispatch(new IntentEvent(IntentEvent.Type.FAILED, intent));
        }
    }

    private void executeInstallingPhase(Intent intent,
                                        List<Intent> installable) {
        setState(intent, IntentState.INSTALLING);
        try {
            setState(intent, IntentState.INSTALLED);
            putInstallable(intent.key(), installable);
            dispatch(new IntentEvent(IntentEvent.Type.INSTALLED, intent));

        } catch (IntentException e) {
            setState(intent, IntentState.FAILED);
            dispatch(new IntentEvent(IntentEvent.Type.FAILED, intent));
        }
    }

    private void executeWithdrawingPhase(Intent intent,
                                         List<Intent> installable) {
        setState(intent, IntentState.WITHDRAWING);
        try {
            removeInstallable(intent.key());
            setState(intent, IntentState.WITHDRAWN);
            dispatch(new IntentEvent(IntentEvent.Type.WITHDRAWN, intent));
        } catch (IntentException e) {
            // FIXME: Rework this to always go from WITHDRAWING to WITHDRAWN!
            setState(intent, IntentState.FAILED);
            dispatch(new IntentEvent(IntentEvent.Type.FAILED, intent));
        }
    }

    // Sets the internal state for the given intent and dispatches an event
    private void setState(Intent intent, IntentState state) {
        intentStates.put(intent.key(), state);
    }

    private void putInstallable(Key key, List<Intent> installable) {
        installables.put(key, installable);
    }

    private void removeInstallable(Key key) {
        installables.remove(key);
    }

    private List<Intent> getInstallable(Key key) {
        List<Intent> installable = installables.get(key);
        if (installable != null) {
            return installable;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void submit(Intent intent) {
        intents.put(intent.key(), intent);
        setState(intent, IntentState.INSTALL_REQ);
        dispatch(new IntentEvent(IntentEvent.Type.INSTALL_REQ, intent));
        executeSubmit(intent);
    }

    @Override
    public void withdraw(Intent intent) {
        executeWithdraw(intent);
    }

    @Override
    public void purge(Intent intent) {
        IntentState currentState = intentStates.get(intent.key());
        if (currentState == IntentState.WITHDRAWN
                || currentState == IntentState.FAILED) {
            intents.remove(intent.key());
            installables.remove(intent.key());
            intentStates.remove(intent.key());
            dispatch(new IntentEvent(IntentEvent.Type.PURGED, intent));
        }
    }

    @Override
    public Set<Intent> getIntents() {
        return Collections.unmodifiableSet(new HashSet<>(intents.values()));
    }

    @Override
    public Iterable<IntentData> getIntentData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getIntentCount() {
        return intents.size();
    }

    @Override
    public Intent getIntent(Key intentKey) {
        return intents.get(intentKey);
    }

    @Override
    public IntentState getIntentState(Key intentKey) {
        return intentStates.get(intentKey);
    }

    @Override
    public List<Intent> getInstallableIntents(Key intentKey) {
        return installables.get(intentKey);
    }

    @Override
    public boolean isLocal(Key intentKey) {
        return true;
    }

    @Override
    public Iterable<Intent> getPending() {
        return Collections.emptyList();
    }

    @Override
    public void addListener(IntentListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IntentListener listener) {
        listeners.remove(listener);
    }

    private void dispatch(IntentEvent event) {
        for (IntentListener listener : listeners) {
            listener.event(event);
        }
    }

    @Override
    public <T extends Intent> void registerCompiler(Class<T> cls,
            IntentCompiler<T> compiler) {
        compilers.put(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilers.remove(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return Collections.unmodifiableMap(compilers);
    }

    private void registerSubclassCompilerIfNeeded(Intent intent) {
        if (!compilers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    IntentCompiler<?> compiler = compilers.get(cls);
                    if (compiler != null) {
                        compilers.put(intent.getClass(), compiler);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }
}
