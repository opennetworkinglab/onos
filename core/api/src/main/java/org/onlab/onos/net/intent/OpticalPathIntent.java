package org.onlab.onos.net.intent;

import java.util.Collection;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;

public class OpticalPathIntent extends OpticalConnectivityIntent {
    private final Path path;
    // private final TrafficSelector opticalMatch;
    // private final TrafficTreatment opticalAction;

    public OpticalPathIntent(ApplicationId appId,
            ConnectPoint src,
            ConnectPoint dst,
            TrafficSelector match,
            TrafficTreatment action,
            Path path) {
        super(appId, src, dst);
        // this.opticalMatch = match;
        // this.opticalAction = action;
        this.path = path;
    }

    protected OpticalPathIntent() {
        // this.opticalMatch = null;
        // this.opticalAction = null;
        this.path = null;
    }

    public Path path() {
        return path;
    }
/*
    public TrafficSelector selector() {
        // return opticalMatch;
    }

    public TrafficTreatment treatment() {
        // return opticalAction;
    }
*/
    @Override
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                //.add("match", opticalMatch)
                //.add("action", opticalAction)
                .add("ingressPort", this.getSrcConnectPoint())
                .add("egressPort", this.getDst())
                .add("path", path)
                .toString();
    }

    public Collection<Link> requiredLinks() {
        return path.links();
    }
}
