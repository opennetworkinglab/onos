package org.onosproject.provider.te.topology;

import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangResourceIdentifierType;

/**
 * Represents implementation of YangCompositeEncoding interfaces.
 */
public class YangCompositeEncodingImpl implements YangCompositeEncoding {

    /**
     * Resource identifier for composite encoding.
     */
    private String resourceIdentifier;

    /**
     * Resource information for composite encoding.
     */
    private String resourceInformation;

    /**
     * Resource identifier type.
     */
    public YangResourceIdentifierType resourceIdentifierType;

    /**
     * Creates an instance of YangCompositeEncodingImpl.
     *
     * @param resourceIdentifierType is URI
     * @param resourceIdentifier is the URI string
     * @param resourceInformation is the JSON body string
     */
    public YangCompositeEncodingImpl(YangResourceIdentifierType resourceIdentifierType,
                                     String resourceIdentifier,
                                     String resourceInformation) {
        this.resourceIdentifierType = resourceIdentifierType;
        this.resourceIdentifier = resourceIdentifier;
        this.resourceInformation = resourceInformation;
    }

    @Override
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public YangResourceIdentifierType getResourceIdentifierType() {
        return resourceIdentifierType;
    }

    @Override
    public String getResourceInformation() {
        return resourceInformation;
    }

    @Override
    public void setResourceIdentifier(String resourceId) {
        resourceIdentifier = resourceId;
    }

    @Override
    public void setResourceInformation(String resourceInfo) {
        resourceInformation = resourceInfo;
    }

    @Override
    public void setResourceIdentifierType(YangResourceIdentifierType idType) {
        resourceIdentifierType = idType;
    }
}

