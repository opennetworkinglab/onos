package org.onlab.onos.net.intent;

import java.util.Map;

/**
 * Service for extending the capability of intent framework by
 * adding additional compilers or/and installers.
 */
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

    /**
     * Registers the specified installer for the given installable intent class.
     *
     * @param cls       installable intent class
     * @param installer intent installer
     * @param <T>       the type of installable intent
     */
    <T extends Intent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer);

    /**
     * Unregisters the installer for the given installable intent class.
     *
     * @param cls installable intent class
     * @param <T> the type of installable intent
     */
    <T extends Intent> void unregisterInstaller(Class<T> cls);

    /**
     * Returns immutable set of bindings of currently registered intent installers.
     *
     * @return the set of installer bindings
     */
    Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> getInstallers();
}
