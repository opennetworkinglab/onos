package org.onlab.onos.net.intent.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.intent.IntentState.FAILED;
import static org.onlab.onos.net.intent.IntentState.INSTALLED;
import static org.onlab.onos.net.intent.IntentState.WITHDRAWING;
import static org.onlab.onos.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final CopyOnWriteArrayList<IntentListener> listeners = new CopyOnWriteArrayList<>();

    private final AbstractListenerRegistry<IntentEvent, IntentListener>
        listenerRegistry = new AbstractListenerRegistry<>();

    private final IntentStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(IntentEvent.class, listenerRegistry);

//        this.intentEvents = new IntentMap<>("intentState", IntentEvent.class, collectionsService);
//        this.installableIntents =
//                new IntentMap<>("installableIntents", IntentCompilationResult.class, collectionsService);
//
//
//        this.intentEvents.addListener(new InternalEntryListener(new InternalIntentEventListener()));

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(IntentEvent.class);
        log.info("Stopped");
    }

    @Override
    public void submit(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        registerSubclassCompilerIfNeeded(intent);
        IntentEvent event = store.createIntent(intent);
        eventDispatcher.post(event);
        processStoreEvent(event);
    }

    @Override
    public void withdraw(Intent intent) {
        checkNotNull(intent, INTENT_NULL);
        IntentEvent event = store.setState(intent, WITHDRAWING);
        eventDispatcher.post(event);
        processStoreEvent(event);
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
     * Invokes all of registered intent event listener.
     *
     * @param event event supplied to a listener as an argument
     */
    private void invokeListeners(IntentEvent event) {
        for (IntentListener listener : listeners) {
            listener.event(event);
        }
    }

    /**
     * Returns the corresponding intent compiler to the specified intent.
     *
     * @param intent intent
     * @param <T> the type of intent
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
     * @param intent intent
     * @param <T> the type of installable intent
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
     * Compiles an intent.
     *
     * @param intent intent
     */
    private void compileIntent(Intent intent) {
        // FIXME: To make SDN-IP workable ASAP, only single level compilation is implemented
        // TODO: implement compilation traversing tree structure
        List<InstallableIntent> installable = new ArrayList<>();
        for (Intent compiled : getCompiler(intent).compile(intent)) {
            installable.add((InstallableIntent) compiled);
        }
        IntentEvent event = store.addInstallableIntents(intent.getId(), installable);
        eventDispatcher.post(event);
        processStoreEvent(event);
    }

    /**
     * Installs an intent.
     *
     * @param intent intent
     */
    private void installIntent(Intent intent) {
        for (InstallableIntent installable : store.getInstallableIntents(intent.getId())) {
            registerSubclassInstallerIfNeeded(installable);
            getInstaller(installable).install(installable);
        }

        IntentEvent event = store.setState(intent, INSTALLED);
        eventDispatcher.post(event);
        processStoreEvent(event);

    }

    /**
     * Uninstalls an intent.
     *
     * @param intent intent
     */
    private void uninstallIntent(Intent intent) {
        for (InstallableIntent installable : store.getInstallableIntents(intent.getId())) {
            getInstaller(installable).uninstall(installable);
        }

        store.removeInstalledIntents(intent.getId());
        store.setState(intent, WITHDRAWN);
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

    /**
     * Handles state transition of submitted intents.
     */
    private void processStoreEvent(IntentEvent event) {
            invokeListeners(event);
            Intent intent = event.getIntent();

            try {
                switch (event.getState()) {
                    case SUBMITTED:
                        compileIntent(intent);
                        break;
                    case COMPILED:
                        installIntent(intent);
                        break;
                    case INSTALLED:
                        break;
                    case WITHDRAWING:
                        uninstallIntent(intent);
                        break;
                    case WITHDRAWN:
                        break;
                    case FAILED:
                        break;
                    default:
                        throw new IllegalStateException(
                                "the state of IntentEvent is illegal: " + event.getState());
                }
            } catch (IntentException e) {
                store.setState(intent, FAILED);
            }

    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements IntentStoreDelegate {
        @Override
        public void notify(IntentEvent event) {
            eventDispatcher.post(event);
            processStoreEvent(event);
        }
    }

}
