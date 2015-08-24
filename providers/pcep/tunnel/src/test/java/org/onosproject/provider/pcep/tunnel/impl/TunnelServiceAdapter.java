package org.onosproject.provider.pcep.tunnel.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelSubscription;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Collections;

public class TunnelServiceAdapter implements TunnelService {
    @Override
    public Tunnel borrowTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelName tunnelName,
                                           Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                           Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                           Tunnel.Type type, Annotations... annotations) {
        return null;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelName tunnelName, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                Tunnel.Type type, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                Annotations... annotations) {
        return false;
    }

    @Override
    public Tunnel queryTunnel(TunnelId tunnelId) {
        return null;
    }

    @Override
    public Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId consumerId) {
        return null;
    }

    @Override
    public Collection<Tunnel> queryTunnel(Tunnel.Type type) {
        return null;
    }

    @Override
    public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
        return null;
    }

    @Override
    public Collection<Tunnel> queryAllTunnels() {
        return Collections.emptyList();
    }

    @Override
    public int tunnelCount() {
        return 0;
    }

    @Override
    public Iterable<Tunnel> getTunnels(DeviceId deviceId) {
        return null;
    }

    @Override
    public void addListener(TunnelListener listener) {

    }

    @Override
    public void removeListener(TunnelListener listener) {

    }
}
