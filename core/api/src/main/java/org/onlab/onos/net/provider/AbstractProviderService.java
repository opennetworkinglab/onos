package org.onlab.onos.net.provider;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of a provider service, which tracks the provider to
 * which it is issued and can be invalidated.
 *
 * @param <P> type of the information provider
 */
public abstract class AbstractProviderService<P extends Provider> implements ProviderService<P> {

    private boolean isValid = true;
    private final P provider;

    /**
     * Creates a provider service on behalf of the specified provider.
     *
     * @param provider provider to which this service is being issued
     */
    protected AbstractProviderService(P provider) {
        this.provider = provider;
    }

    /**
     * Invalidates this provider service.
     */
    public void invalidate() {
        isValid = false;
    }

    /**
     * Checks the validity of this provider service.
     *
     * @throws java.lang.IllegalStateException if the service is no longer valid
     */
    public void checkValidity() {
        checkState(isValid, "Provider service is no longer valid");
    }

    @Override
    public P provider() {
        return provider;
    }

}
