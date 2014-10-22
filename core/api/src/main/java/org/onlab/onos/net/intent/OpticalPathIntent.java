package org.onlab.onos.net.intent;

import java.util.Collection;
import java.util.Objects;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;

public class OpticalPathIntent extends OpticalConnectivityIntent implements InstallableIntent {

    private final Path path;
    private final TrafficSelector opticalMatch;
    private final TrafficTreatment opticalAction;

    public OpticalPathIntent(IntentId id, TrafficSelector match, TrafficTreatment action,
                      ConnectPoint ingressPort, ConnectPoint egressPort,
                      Path path) {
        this.opticalMatch = match;
        this.opticalAction = action;
        this.path = path;
    }

    public OpticalPathIntent() {
        this.opticalMatch = null;
        this.opticalAction = null;
        this.path = null;
    }

    public Path path() {
        return path;
    }

    public TrafficSelector selector() {
        return opticalMatch;
    }

    public TrafficTreatment treatment() {
        return opticalAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        OpticalPathIntent that = (OpticalPathIntent) o;

        if (!path.equals(that.path)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("match", opticalMatch)
                .add("action", opticalAction)
                .add("ingressPort", this.getSrcConnectPoint())
                .add("egressPort", this.getDst())
                .add("path", path)
                .toString();
    }

    @Override
    public Collection<Link> requiredLinks() {
        return path.links();
    }
}
