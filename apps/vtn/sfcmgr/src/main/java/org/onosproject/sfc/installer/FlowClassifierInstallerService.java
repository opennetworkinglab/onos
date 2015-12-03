package org.onosproject.sfc.installer;

import org.onosproject.net.NshServicePathId;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;

/**
 * Abstraction of an entity which installs flow classification rules in ovs.
 */
public interface FlowClassifierInstallerService {

    /**
     * Install Flow-Classifier.
     *
     * @param portChain port-chain
     * @param nshSpiId nsh spi-id
     */
    void installFlowClassifier(PortChain portChain, NshServicePathId nshSpiId);

    /**
     * Uninstall Flow-Classifier.
     *
     * @param portChain port-chain
     * @param nshSpiId nsh spi-id
     */
    void unInstallFlowClassifier(PortChain portChain, NshServicePathId nshSpiId);

    /**
     * Prepare forwarding object for flow classifier.
     *
     * @param flowClassifier flow classifier
     * @param portPair port pair
     * @param nshSpiId nsh spi id
     * @param type forwarding objective operation type
     */
    void prepareFlowClassification(FlowClassifier flowClassifier, PortPair portPair, NshServicePathId nshSpiId,
                                   Operation type);
}
