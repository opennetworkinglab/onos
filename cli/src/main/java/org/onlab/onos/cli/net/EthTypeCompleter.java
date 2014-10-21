package org.onlab.onos.cli.net;

import java.util.List;
import java.util.SortedSet;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

/**
 * Ethernet type completer.
 */
public class EthTypeCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();
        strings.add(EthType.ARP.toString());
        strings.add(EthType.BSN.toString());
        strings.add(EthType.IPV4.toString());
        strings.add(EthType.LLDP.toString());
        strings.add(EthType.RARP.toString());

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
