package org.onlab.onos.net.intent.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.onos.net.intent.IntentState.COMPILING;
import static org.onlab.onos.net.intent.IntentState.FAILED;
import static org.onlab.onos.net.intent.IntentState.INSTALLED;
import static org.onlab.onos.net.intent.IntentState.INSTALLING;
import static org.onlab.onos.net.intent.IntentState.RECOMPILING;
import static org.onlab.onos.net.intent.IntentState.WITHDRAWING;
import static org.onlab.onos.net.intent.IntentState.WITHDRAWN;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.intent.InstallableIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentException;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentOperations;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentStore;
import org.onlab.onos.net.intent.IntentStoreDelegate;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

/**
 * An implementation of Intent Manager.
 */
@Component(immediate = true)
@Service
public class IntentManager
        implements IntentService, IntentExtensionService {
    private final Logger log = getLogger(getClass());

    public static final String INTENT_NULL = "Intent cannot be null";
    public static final String INTENT_ID_NULL = "Intent ID cannot be null";

    // Collections for compiler, installer, and listener are ONOS instance local
    private final ConcurrentMap<Class<? extends Intent>,
            IntentCompiler<? extends Intent>> compilers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends InstallableIntent>,
            IntentInstaller<? extends InstallableIntent>> installers = new ConcurrentHashMap<>();

    private final AbstractListenerRegistry<IntentEvent, IntentListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final ExecutorService executor = newSingleThreadExecutor(namedThreads("onos-intents"));

    private final IntentStoreDelegate delegate = new InternalStoreDelegate();
    private final TopologyChangeDelegate topoDelegate = new InternalTopoChangeDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        trackerService.setDelegate(topoDelegate);
        eventDispatcher.addSink(IntentEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        trackerService.unsetDelegate(topoDelegate);
        eventDispatcher.removeSink(IntentEvent.class);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        registerSubclassCompilerIfNeeded(intent);
        IntentEvent event = store.createIntent(intent);
        if (event != null) {
            eventDispatcher.post(event);
            executor.execute(new IntentTask(COMPILING, intent));
        }
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        executor.execute(new IntentTask(WITHDRAWING, intent));
    }

    // FIXME: implement this method
    @Override
    public void execute(IntentOperations operations) {
        throw new UnsupportedOperationException("execute() is not implemented yet");
    }

    @Override
    public Iterable<Intent> getIntents() {
        return store.getIntents();
    }

    @Override
    public long getIntentCount() {
        return store.getIntentCount();
    }

    @Override
    public Intent getIntent(IntentId id) {
        checkNotNull(id, INTENT_ID_NULL);
        return store.getIntent(id);
    }

    @Override
    public IntentState getIntentState(IntentId id) {
        checkNotNull(id, INTENT_ID_NULL);
        return store.getIntentState(id);
    }

    @Override
    public void addListener(IntentListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(IntentListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public <T extends Intent> void registerCompiler(Class<T> cls, IntentCompiler<T> compiler) {
        compilers.put(cls, compiler);
    }

    @Override
    public <T extends Intent> void unregisterCompiler(Class<T> cls) {
        compilers.remove(cls);
    }

    @Override
    public Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> getCompilers() {
        return ImmutableMap.copyOf(compilers);
    }

    @Override
    public <T extends InstallableIntent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installers.put(cls, installer);
    }

    @Override
    public <T extends InstallableIntent> void unregisterInstaller(Class<T> cls) {
        installers.remove(cls);
    }

    @Override
    public Map<Class<? extends InstallableIntent>, IntentInstaller<? extends InstallableIntent>> getInstallers() {
        return ImmutableMap.copyOf(installers);
    }

    /**
     * Returns the corresponding intent compiler to the specified intent.
     *
     * @param intent intent
     * @param <T>    the type of intent
     * @return intent compiler corresponding to the specified intent
     */
    private <T extends Intent> IntentCompiler<T> getCompiler(T intent) {
        @SuppressWarnings("unchecked")
        IntentCompiler<T> compiler = (IntentCompiler<T>) compilers.get(intent.getClass());
        if (compiler == null) {
            throw new IntentException("no compiler for class " + intent.getClass());
        }
        return compiler;
    }

    /**
     * Returns the corresponding intent installer to the specified installable intent.
     *
     * @param intent intent
     * @param <T>    the type of installable intent
     * @return intent installer corresponding to the specified installable intent
     */
    private <T extends InstallableIntent> IntentInstaller<T> getInstaller(T intent) {
        @SuppressWarnings("unchecked")
        IntentInstaller<T> installer = (IntentInstaller<T>) installers.get(intent.getClass());
        if (installer == null) {
            throw new IntentException("no installer for class " + intent.getClass());
        }
        return installer;
    }

    /**
     * Compiles the specified intent.
     *
     * @param intent intent to be compiled
     */
    private void executeCompilingPhase(Intent intent) {
        // Indicate that the intent is entering the compiling phase.
        store.setState(intent, COMPILING);

        try {
            // Compile the intent into installable derivatives.
            List<InstallableIntent> installable = compileIntent(intent);

            // If all went well, associate the resulting list of installable
            // intents with the top-level intent and proceed to install.
            store.addInstallableIntents(intent.id(), installable);
            executeInstallingPhase(intent);

        } catch (Exception e) {
            log.warn("Unable to compile intent {} due to: {}", intent.id(), e);

            // If compilation failed, mark the intent as failed.
            store.setState(intent, FAILED);
        }
    }

    // FIXME: To make SDN-IP workable ASAP, only single level compilation is implemented
    // TODO: implement compilation traversing tree structure
    private List<InstallableIntent> compileIntent(Intent intent) {
        List<InstallableIntent> installable = new ArrayList<>();
        for (Intent compiled : getCompiler(intent).compile(intent)) {
            InstallableIntent installableIntent = (InstallableIntent) compiled;
            installable.add(installableIntent);
        }
        return installable;
    }

    /**
     * Installs all installable intents associated with the specified top-level
     * intent.
     *
     * @param intent intent to be installed
     */
    private void executeInstallingPhase(Intent intent) {
        // Indicate that the intent is entering the installing phase.
        store.setState(intent, INSTALLING);

        try {
            List<InstallableIntent> installables = store.getInstallableIntents(intent.id());
            if (installables != null) {
                for (InstallableIntent installable : installables) {
                    registerSubclassInstallerIfNeeded(installable);
                    trackerService.addTrackedResources(intent.id(),
                                                       installable.requiredLinks());
                    getInstaller(installable).install(installable);
                }
            }
            eventDispatcher.post(store.setState(intent, INSTALLED));

        } catch (Exception e) {
            log.warn("Unable to install intent {} due to: {}", intent.id(), e);
            uninstallIntent(intent);

            // If compilation failed, kick off the recompiling phase.
            executeRecompilingPhase(intent);
        }
    }

    /**
     * Recompiles the specified intent.
     *
     * @param intent intent to be recompiled
     */
    private void executeRecompilingPhase(Intent intent) {
        // Indicate that the intent is entering the recompiling phase.
        store.setState(intent, RECOMPILING);

        try {
            // Compile the intent into installable derivatives.
            List<InstallableIntent> installable = compileIntent(intent);

            // If all went well, compare the existing list of installable
            // intents with the newly compiled list. If they are the same,
            // bail, out since the previous approach was determined not to
            // be viable.
            List<InstallableIntent> originalInstallable =
                    store.getInstallableIntents(intent.id());

            if (Objects.equals(originalInstallable, installable)) {
                eventDispatcher.post(store.setState(intent, FAILED));
            } else {
                // Otherwise, re-associate the newly compiled installable intents
                // with the top-level intent and kick off installing phase.
                store.addInstallableIntents(intent.id(), installable);
                executeInstallingPhase(intent);
            }
        } catch (Exception e) {
            log.warn("Unable to recompile intent {} due to: {}", intent.id(), e);

            // If compilation failed, mark the intent as failed.
            eventDispatcher.post(store.setState(intent, FAILED));
        }
    }

    /**
     * Uninstalls the specified intent by uninstalling all of its associated
     * installable derivatives.
     *
     * @param intent intent to be installed
     */
    private void executeWithdrawingPhase(Intent intent) {
        // Indicate that the intent is being withdrawn.
        store.setState(intent, WITHDRAWING);
        uninstallIntent(intent);

        // If all went well, disassociate the top-level intent with its
        // installable derivatives and mark it as withdrawn.
        store.removeInstalledIntents(intent.id());
        eventDispatcher.post(store.setState(intent, WITHDRAWN));
    }

    /**
     * Uninstalls all installable intents associated with the given intent.
     *
     * @param intent intent to be uninstalled
     */
    private void uninstallIntent(Intent intent) {
        try {
            List<InstallableIntent> installables = store.getInstallableIntents(intent.id());
            if (installables != null) {
                for (InstallableIntent installable : installables) {
                    getInstaller(installable).uninstall(installable);
                }
            }
        } catch (IntentException e) {
            log.warn("Unable to uninstall intent {} due to: {}", intent.id(), e);
        }
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

    /**
     * Registers an intent installer of the specified intent if an intent installer
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent installer for a parent type is found, this method
     * registers the found intent installer.
     *
     * @param intent intent
     */
    private void registerSubclassInstallerIfNeeded(InstallableIntent intent) {
        if (!installers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the InstallableIntent class descendants
                if (InstallableIntent.class.isAssignableFrom(cls)) {
                    IntentInstaller<?> installer = installers.get(cls);
                    if (installer != null) {
                        installers.put(intent.getClass(), installer);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements IntentStoreDelegate {
        @Override
        public void notify(IntentEvent event) {
            eventDispatcher.post(event);
            if (event.type() == IntentEvent.Type.SUBMITTED) {
                executor.execute(new IntentTask(COMPILING, event.subject()));
            }
        }
    }

    // Topology change delegate
    private class InternalTopoChangeDelegate implements TopologyChangeDelegate {
        @Override
        public void triggerCompile(Iterable<IntentId> intentIds,
                                   boolean compileAllFailed) {
            // Attempt recompilation of the specified intents first.
            for (IntentId intentId : intentIds) {
                Intent intent = getIntent(intentId);
                uninstallIntent(intent);

                executeRecompilingPhase(intent);
            }

            if (compileAllFailed) {
                // If required, compile all currently failed intents.
                for (Intent intent : getIntents()) {
                    if (getIntentState(intent.id()) == FAILED) {
                        executeCompilingPhase(intent);
                    }
                }
            }
        }
    }

    // Auxiliary runnable to perform asynchronous tasks.
    private class IntentTask implements Runnable {
        private final IntentState state;
        private final Intent intent;

        public IntentTask(IntentState state, Intent intent) {
            this.state = state;
            this.intent = intent;
        }

        @Override
        public void run() {
            if (state == COMPILING) {
                executeCompilingPhase(intent);
            } else if (state == RECOMPILING) {
                executeRecompilingPhase(intent);
            } else if (state == WITHDRAWING) {
                executeWithdrawingPhase(intent);
            }
        }
    }

}
