package org.onlab.onos.metrics.topology.cli;

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
import org.onlab.onos.metrics.topology.TopologyMetricsService;

/**
 * Command to show the topology events metrics.
 */
@Command(scope = "onos", name = "topology-events-metrics",
         description = "Lists topology events metrics")
public class TopologyEventsMetricsCommand extends AbstractShellCommand {

    private static final String FORMAT_GAUGE =
        "Last Topology Event Timestamp (ms from epoch)=%d";
    private static final String FORMAT_METER =
        "Topology Events count=%d rate(events/sec) mean=%f m1=%f m5=%f m15=%f";

    @Override
    protected void execute() {
        TopologyMetricsService service = get(TopologyMetricsService.class);
        Gauge<Long> gauge = service.lastEventTimestampEpochMsGauge();
        Meter meter = service.eventRateMeter();

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new MetricsModule(TimeUnit.SECONDS,
                                                  TimeUnit.MILLISECONDS,
                                                  false));
            ObjectNode result = mapper.createObjectNode();
            try {
                //
                // NOTE: The API for custom serializers is incomplete,
                // hence we have to parse the JSON string to create JsonNode.
                //
                final String gaugeJson = mapper.writeValueAsString(gauge);
                final String meterJson = mapper.writeValueAsString(meter);
                JsonNode gaugeNode = mapper.readTree(gaugeJson);
                JsonNode meterNode = mapper.readTree(meterJson);
                result.put("lastTopologyEventTimestamp", gaugeNode);
                result.put("listenerEventRate", meterNode);
            } catch (JsonProcessingException e) {
                log.error("Error writing value as JSON string", e);
            } catch (IOException e) {
                log.error("Error writing value as JSON string", e);
            }
            print("%s", result);
        } else {
            TimeUnit rateUnit = TimeUnit.SECONDS;
            double rateFactor = rateUnit.toSeconds(1);
            print(FORMAT_GAUGE, gauge.getValue());
            print(FORMAT_METER, meter.getCount(),
                  meter.getMeanRate() * rateFactor,
                  meter.getOneMinuteRate() * rateFactor,
                  meter.getFiveMinuteRate() * rateFactor,
                  meter.getFifteenMinuteRate() * rateFactor);
        }
    }
}
