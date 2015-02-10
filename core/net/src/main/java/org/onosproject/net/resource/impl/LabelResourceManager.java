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
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.ApplyLabelNumber;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceDelegate;
import org.onosproject.net.resource.LabelResourceEvent;
import org.onosproject.net.resource.LabelResourceId;
import org.onosproject.net.resource.LabelResourceListener;
import org.onosproject.net.resource.LabelResourcePool;
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
public class LabelResourceManager implements LabelResourceService {
    private final Logger log = getLogger(getClass());
    private final LabelResourceDelegate delegate = new InternalLabelResourceDelegate();

    private final AbstractListenerRegistry<LabelResourceEvent, LabelResourceListener> listenerRegistry
                                                                                = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(LabelResourceEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(LabelResourceEvent.class);
        log.info("Stopped");
    }

    @Override
    public void createDevicePool(DeviceId deviceId, LabelResourceId beginLabel,
                       LabelResourceId endLabel) {
        // TODO Auto-generated method stub
        LabelResourceEvent event = store.createDevicePool(deviceId, beginLabel, endLabel);
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
    public boolean releaseToGlobalPool(Set<DefaultLabelResource> release) {
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
}
