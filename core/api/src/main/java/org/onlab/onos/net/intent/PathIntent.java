package org.onlab.onos.net.intent;

import com.google.common.base.MoreObjects;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

/**
 * Abstraction of explicitly path specified connectivity intent.
 */
public class PathIntent extends ConnectivityIntent {

    private final Path path;

    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and using the specified explicit path.
     *
     * @param appId     application identifier
     * @param selector  traffic selector
     * @param treatment treatment
     * @param path      traversed links
     * @throws NullPointerException {@code path} is null
     */
    public PathIntent(ApplicationId appId, TrafficSelector selector,
                      TrafficTreatment treatment, Path path) {
        super(id(PathIntent.class, selector, treatment, path), appId,
              resources(path.links()), selector, treatment);
        this.path = path;
    }

    /**
     * Constructor for serializer.
     */
    protected PathIntent() {
        super();
        this.path = null;
    }

    /**
     * Returns the links which the traffic goes along.
     *
     * @return traversed links
     */
    public Path path() {
        return path;
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("appId", appId())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("path", path)
                .toString();
    }

}
