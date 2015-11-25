package org.onosproject.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.net.EncapsulationType;

import java.util.List;
import java.util.SortedSet;

/**
 * Encapsulation type completer.
 */
public class EncapTypeCompleter implements Completer {

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();

        for (EncapsulationType encapType : EncapsulationType.values()) {
            strings.add(encapType.toString());
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }
}
