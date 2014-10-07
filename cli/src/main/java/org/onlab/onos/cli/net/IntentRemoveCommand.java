package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentService;

/**
 * Removes host-to-host connectivity intent.
 */
@Command(scope = "onos", name = "remove-intent",
         description = "Removes the specified intent")
public class IntentRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Intent ID",
              required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        int radix = id.startsWith("0x") ? 16 : 10;
        if (radix == 16) {
            id = id.replaceFirst("0x", "");
        }
        IntentId intentId = new IntentId(Long.parseLong(id, radix));


        Intent intent = service.getIntent(intentId);
        if (intent != null) {
            service.withdraw(intent);
        }
    }
}
