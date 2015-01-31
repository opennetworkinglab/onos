package org.onosproject.net.resource.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.resource.DefaultLabelResource;
import org.onosproject.net.resource.LabelResourceDelegate;
import org.onosproject.net.resource.LabelResourceEvent;
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
    public void create(DeviceId deviceId, long beginLabel, long endLabel) {
        // TODO Auto-generated method stub
        LabelResourceEvent event = store.create(deviceId, beginLabel, endLabel);
        post(event);
    }

    @Override
    public void create(LabelResourcePool labelResourcePool) {
        // TODO Auto-generated method stub
        LabelResourceEvent event = store.create(labelResourcePool);
        post(event);
    }

    @Override
    public void destroy(DeviceId deviceId) {
        LabelResourceEvent event = store.destroy(deviceId);
        post(event);
    }

    @Override
    public Collection<DefaultLabelResource> apply(DeviceId deviceId,
                                                  long applyNum) {
        // TODO Auto-generated method stub
        return store.apply(deviceId, applyNum);
    }

    @Override
    public boolean release(Multimap<DeviceId, DefaultLabelResource> release) {
        // TODO Auto-generated method stub
        return store.release(release);
    }

    @Override
    public boolean isFull(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.isFull(deviceId);
    }

    @Override
    public long getFreeNum(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.getFreeNum(deviceId);
    }

    @Override
    public LabelResourcePool getLabelResourcePool(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return store.getLabelResourcePool(deviceId);
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
