package org.onlab.onos.metrics.intent.cli;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.metrics.intent.IntentMetricsService;

/**
 * Command to show the intent events metrics.
 */
@Command(scope = "onos", name = "intents-events-metrics",
         description = "Lists intent events metrics")
public class IntentEventsMetricsCommand extends AbstractShellCommand {

    private static final String FORMAT_GAUGE =
        "Intent %s Event Timestamp (ms from epoch)=%d";
    private static final String FORMAT_METER =
        "Intent %s Events count=%d rate(events/sec) mean=%f m1=%f m5=%f m15=%f";

    @Override
    protected void execute() {
        IntentMetricsService service = get(IntentMetricsService.class);
        Gauge<Long> gauge;
        Meter meter;

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new MetricsModule(TimeUnit.SECONDS,
                                                  TimeUnit.MILLISECONDS,
                                                  false));
            ObjectNode result = mapper.createObjectNode();
            //
            gauge = service.intentSubmittedTimestampEpochMsGauge();
            result.put("intentSubmittedTimestamp", json(mapper, gauge));
            gauge = service.intentInstalledTimestampEpochMsGauge();
            result.put("intentInstalledTimestamp", json(mapper, gauge));
            gauge = service.intentWithdrawRequestedTimestampEpochMsGauge();
            result.put("intentWithdrawRequestedTimestamp",
                       json(mapper, gauge));
            gauge = service.intentWithdrawnTimestampEpochMsGauge();
            result.put("intentWithdrawnTimestamp", json(mapper, gauge));
            //
            meter = service.intentSubmittedRateMeter();
            result.put("intentSubmittedRate", json(mapper, meter));
            meter = service.intentInstalledRateMeter();
            result.put("intentInstalledRate", json(mapper, meter));
            meter = service.intentWithdrawRequestedRateMeter();
            result.put("intentWithdrawRequestedRate", json(mapper, meter));
            meter = service.intentWithdrawnRateMeter();
            result.put("intentWithdrawnRate", json(mapper, meter));
            //
            print("%s", result);
        } else {
            gauge = service.intentSubmittedTimestampEpochMsGauge();
            printGauge("Submitted", gauge);
            gauge = service.intentInstalledTimestampEpochMsGauge();
            printGauge("Installed", gauge);
            gauge = service.intentWithdrawRequestedTimestampEpochMsGauge();
            printGauge("Withdraw Requested", gauge);
            gauge = service.intentWithdrawnTimestampEpochMsGauge();
            printGauge("Withdrawn", gauge);
            //
            meter = service.intentSubmittedRateMeter();
            printMeter("Submitted", meter);
            meter = service.intentInstalledRateMeter();
            printMeter("Installed", meter);
            meter = service.intentWithdrawRequestedRateMeter();
            printMeter("Withdraw Requested", meter);
            meter = service.intentWithdrawnRateMeter();
            printMeter("Withdrawn", meter);
        }
    }

    /**
     * Produces JSON node for an Object.
     *
     * @param mapper the JSON object mapper to use
     * @param object the Object with the data
     * @return JSON node for the Object
     */
    private JsonNode json(ObjectMapper mapper, Object object) {
        //
        // NOTE: The API for custom serializers is incomplete,
        // hence we have to parse the JSON string to create JsonNode.
        //
        try {
            final String objectJson = mapper.writeValueAsString(object);
            JsonNode objectNode = mapper.readTree(objectJson);
            return objectNode;
        } catch (JsonProcessingException e) {
            log.error("Error writing value as JSON string", e);
        } catch (IOException e) {
            log.error("Error writing value as JSON string", e);
        }
        return null;
    }

    /**
     * Prints a Gauge.
     *
     * @param operationStr the string with the intent operation to print
     * @param gauge the Gauge to print
     */
    private void printGauge(String operationStr, Gauge<Long> gauge) {
        print(FORMAT_GAUGE, operationStr, gauge.getValue());
    }

    /**
     * Prints a Meter.
     *
     * @param operationStr the string with the intent operation to print
     * @param meter the Meter to print
     */
    private void printMeter(String operationStr, Meter meter) {
            TimeUnit rateUnit = TimeUnit.SECONDS;
            double rateFactor = rateUnit.toSeconds(1);
            print(FORMAT_METER, operationStr, meter.getCount(),
                  meter.getMeanRate() * rateFactor,
                  meter.getOneMinuteRate() * rateFactor,
                  meter.getFiveMinuteRate() * rateFactor,
                  meter.getFifteenMinuteRate() * rateFactor);
    }
}
