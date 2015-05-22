package org.onosproject.incubator.net.resource.label.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceDelegate;
import org.onosproject.incubator.net.resource.label.LabelResourceEvent;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceListener;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceProvider;
import org.onosproject.incubator.net.resource.label.LabelResourceProviderRegistry;
import org.onosproject.incubator.net.resource.label.LabelResourceProviderService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.resource.label.LabelResourceStore;
import org.slf4j.Logger;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

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
        implements LabelResourceService, LabelResourceAdminService,
        LabelResourceProviderRegistry {
    private final Logger log = getLogger(getClass());
    private final LabelResourceDelegate delegate = new InternalLabelResourceDelegate();

    private final ListenerRegistry<LabelResourceEvent, LabelResourceListener> listenerRegistry
                            = new ListenerRegistry<>();

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
    public boolean createDevicePool(DeviceId deviceId,
                                    LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        checkNotNull(deviceId, "deviceId is not null");
        checkNotNull(beginLabel, "beginLabel is not null");
        checkNotNull(endLabel, "beginLabel is not null");
        checkArgument(beginLabel.labelId() >= 0 || endLabel.labelId() >= 0,
                      "The value of beginLabel and the value of endLabel must be both positive number.");
        checkArgument(beginLabel.labelId() <= endLabel.labelId(),
                      "The value of endLabel must be greater than the value of endLabel.");
        return store.createDevicePool(deviceId, beginLabel, endLabel);
    }

    @Override
    public boolean createGlobalPool(LabelResourceId beginLabel,
                                    LabelResourceId endLabel) {
        checkNotNull(beginLabel, "beginLabel is not null");
        checkNotNull(endLabel, "beginLabel is not null");
        checkArgument(beginLabel.labelId() >= 0 && endLabel.labelId() >= 0,
                "The value of beginLabel and the value of endLabel must be both positive number.");
        checkArgument(beginLabel.labelId() <= endLabel.labelId(),
                "The value of endLabel must be greater than the value of endLabel.");
        return store.createGlobalPool(beginLabel, endLabel);
    }

    @Override
    public boolean destroyDevicePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.destroyDevicePool(deviceId);
    }

    @Override
    public boolean destroyGlobalPool() {
        return store.destroyGlobalPool();
    }

    @Override
    public Collection<LabelResource> applyFromDevicePool(DeviceId deviceId,
                                                         long applyNum) {
        checkNotNull(deviceId, "deviceId is not null");
        checkNotNull(applyNum, "applyNum is not null");
        return store.applyFromDevicePool(deviceId, applyNum);
    }

    @Override
    public Collection<LabelResource> applyFromGlobalPool(long applyNum) {
        checkNotNull(applyNum, "applyNum is not null");
        return store.applyFromGlobalPool(applyNum);
    }

    @Override
    public boolean releaseToDevicePool(Multimap<DeviceId, LabelResource> release) {
        checkNotNull(release, "release is not null");
        return store.releaseToDevicePool(release);
    }

    @Override
    public boolean releaseToGlobalPool(Set<LabelResourceId> release) {
        checkNotNull(release, "release is not null");
        return store.releaseToGlobalPool(release);
    }

    @Override
    public boolean isDevicePoolFull(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.isDevicePoolFull(deviceId);
    }

    @Override
    public boolean isGlobalPoolFull() {
        return store.isGlobalPoolFull();
    }

    @Override
    public long getFreeNumOfDevicePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.getFreeNumOfDevicePool(deviceId);
    }

    @Override
    public long getFreeNumOfGlobalPool() {
        return store.getFreeNumOfGlobalPool();
    }

    @Override
    public LabelResourcePool getDeviceLabelResourcePool(DeviceId deviceId) {
        checkNotNull(deviceId, "deviceId is not null");
        return store.getDeviceLabelResourcePool(deviceId);
    }

    @Override
    public LabelResourcePool getGlobalLabelResourcePool() {
        return store.getGlobalLabelResourcePool();
    }

    @Override
    public void addListener(LabelResourceListener listener) {
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
        }

        @Override
        public void deviceLabelResourcePoolDetected(DeviceId deviceId,
                                                    LabelResourceId beginLabel,
                                                    LabelResourceId endLabel) {
            checkNotNull(deviceId, "deviceId is not null");
            checkNotNull(beginLabel, "beginLabel is not null");
            checkNotNull(endLabel, "endLabel is not null");
            createDevicePool(deviceId, beginLabel, endLabel);
        }

        @Override
        public void deviceLabelResourcePoolDestroyed(DeviceId deviceId) {
            checkNotNull(deviceId, "deviceId is not null");
            destroyDevicePool(deviceId);
        }

    }

    @Override
    protected LabelResourceProviderService createProviderService(LabelResourceProvider provider) {
        return new InternalLabelResourceProviderService(provider);
    }
}
