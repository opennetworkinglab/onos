package org.onosproject.net.resource.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.resource.ApplyLabelNumber;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceDelegate;
import org.onosproject.net.resource.LabelResourceEvent;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceListener;
import org.onosproject.net.resource.LabelResourcePool;
import org.onosproject.net.resource.LabelResourceProvider;
import org.onosproject.net.resource.LabelResourceProviderRegistry;
import org.onosproject.net.resource.LabelResourceProviderService;
import org.onosproject.net.resource.LabelResourceService;
import org.onosproject.net.resource.LabelResourceStore;
import org.slf4j.Logger;

import com.google.common.collect.Multimap;

/**
 * provides implementation of the label resource NB &amp; SB APIs.
 *
 */
@Component(immediate = true)
@Service
public class LabelResourceManager
        extends
        AbstractProviderRegistry<LabelResourceProvider, LabelResourceProviderService>
        implements LabelResourceService, LabelResourceProviderRegistry {
    private final Logger log = getLogger(getClass());
    private final LabelResourceDelegate delegate = new InternalLabelResourceDelegate();

    private final AbstractListenerRegistry<LabelResourceEvent,
                                    LabelResourceListener> listenerRegistry
                                    = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private DeviceListener deviceListener = new InternalDeviceListener();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(LabelResourceEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        log.info("Started");

    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LabelResourceEvent.class);
        log.info("Stopped");
    }

    @Override
    public void createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                                 LabelResourceId endLabel) {
        // TODO Auto-generated method stub
        LabelResourceEvent event = store.createDevicePool(deviceId, beginLabel,
                                                          endLabel);
        post(event);
    }

    @Override
    public void createGlobalPool(LabelResourceId beginLabel,
                                 LabelResourceId endLabel) {
        // TODO Auto-generated method stub
        LabelResourceEvent event = store.createGlobalPool(beginLabel, endLabel);
        post(event);
    }

    @Override
    public void destroyDevicePool(DeviceId deviceId) {
        LabelResourceEvent event = store.destroyDevicePool(deviceId);
        post(event);
    }

    @Override
    public void destroyGlobalPool() {
        LabelResourceEvent event = store.destroyGlobalPool();
        post(event);
    }

    @Override
    public Collection<DefaultLabelResource> applyFromDevicePool(DeviceId deviceId,
                                                                ApplyLabelNumber applyNum) {
        // TODO Auto-generated method stub
        return store.applyFromDevicePool(deviceId, applyNum);
    }

    @Override
    public Collection<DefaultLabelResource> applyFromGlobalPool(ApplyLabelNumber applyNum) {
        // TODO Auto-generated method stub
        return store.applyFromGlobalPool(applyNum);
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, DefaultLabelResource> release) {
        // TODO Auto-generated method stub
        return store.releaseToDevicePool(release);
    }

    @Override
    public boolean releaseToGlobalPool(Set<LabelResourceId> release) {
        // TODO Auto-generated method stub
        return store.releaseToGlobalPool(release);
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.isDevicePoolFull(deviceId);
    }

    @Override
    public boolean isGlobalPoolFull() {
        // TODO Auto-generated method stub
        return store.isGlobalPoolFull();
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.getFreeNumOfDevicePool(deviceId);
    }

    @Override
    public long getFreeNumOfGlobalPool() {
        // TODO Auto-generated method stub
        return store.getFreeNumOfGlobalPool();
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.getDeviceLabelResourcePool(deviceId);
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
        // TODO Auto-generated method stub
        return store.getGlobalLabelResourcePool();
    }

    @Override
    public void addListener(LabelResourceListener listener) {
        // TODO Auto-generated method stub
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(LabelResourceListener listener) {
        listenerRegistry.removeListener(listener);

    }

    private void post(LabelResourceEvent event) {
        if (event != null) {
            eventDispatcher.post(event);
        }
    }

    private class InternalLabelResourceDelegate
            implements LabelResourceDelegate {

        @Override
        public void notify(LabelResourceEvent event) {
            post(event);
        }

    }

    @Override
	public Set<LabelResourcePool> getAllLabelResourcePool() {
		// TODO Auto-generated method stub
		return store.getAllLabelResourcePool();
	}

	private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (Type.DEVICE_REMOVED.equals(event.type())) {
                destroyDevicePool(device.id());
            }
        }
    }

    private class InternalLabelResourceProviderService
            extends AbstractProviderService<LabelResourceProvider>
            implements LabelResourceProviderService {

        protected InternalLabelResourceProviderService(LabelResourceProvider provider) {
            super(provider);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void deviceLabelResourcePoolDetected(DeviceId deviceId,
                                                    LabelResourceId beginLabel,
                                                    LabelResourceId endLabel) {
            // TODO Auto-generated method stub
            createDevicePool(deviceId, beginLabel, endLabel);
        }

        @Override
        public void deviceLabelResourcePoolDestroyed(DeviceId deviceId) {
            // TODO Auto-generated method stub
            destroyDevicePool(deviceId);
        }

    }

    @Override
    protected LabelResourceProviderService createProviderService(LabelResourceProvider provider) {
        // TODO Auto-generated method stub
        return new InternalLabelResourceProviderService(provider);
    }
}