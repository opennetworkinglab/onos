package org.onlab.onos.net.intent;

import java.util.List;

/**
 * Abstraction of a compiler which is capable of taking an intent
 * and translating it to other, potentially installable, intents.
 *
 * @param <T> the type of intent
 */
public interface IntentCompiler<T extends Intent> {
    /**
     * Compiles the specified intent into other intents.
     *
     * @param intent intent to be compiled
     * @return list of resulting intents
     * @throws IntentException if issues are encountered while compiling the intent
     */
    List<Intent> compile(T intent);
}
