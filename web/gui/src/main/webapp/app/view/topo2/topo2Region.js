/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- Topology Region Module.
 Module that holds the current region in memory
 */

(function () {
    'use strict';

    // Injected Services
    var Model;

    // Internal
    var instance;

    // 'static' vars
    var ROOT = '(root)';

    angular.module('ovTopo2')
    .factory('Topo2RegionService', [
        '$log', 'Topo2Model', 'Topo2SubRegionService', 'Topo2DeviceService',
        'Topo2HostService', 'Topo2LinkService', 'Topo2ZoomService', 'Topo2DetailsPanelService',
        'Topo2BreadcrumbService', 'Topo2ViewController', 'Topo2SpriteLayerService', 'Topo2MapService',
        'Topo2MapConfigService', 'Topo2PeerRegionService', 'Topo2NoDevicesConnectedService',
        function ($log, _Model_, t2sr, t2ds, t2hs, t2ls, t2zs, t2dps, t2bcs, ViewController,
                  t2sls, t2ms, t2mcs, t2pr, t2ndcs) {

            Model = _Model_;

            var Region = ViewController.extend({
                initialize: function () {
                    instance = this;
                    this.model = null;

                    this.bgRendered = false;
                    this.regionData = null;
                    this.peers = null;

                    var RegionModel = Model.extend({
                        findNodeById: this.findNodeById,
                        nodes: this.regionNodes.bind(this),
                    });

                    this.model = new RegionModel();
                    this.createEmptyModel();

                },
                createEmptyModel: function () {
                    this.model.set({ subregions: t2sr.createSubRegionCollection([], this) });
                    this.model.set({ devices: t2ds.createDeviceCollection([], this) });
                    this.model.set({ hosts: t2hs.createHostCollection([], this) });
                    this.model.set({ peerRegions: t2pr.createCollection([], this) });
                    this.model.set({ links: t2ls.createLinkCollection([], this) });
                },
                isLoadComplete: function () {
                    return this.bgRendered && this.regionData && this.peers;
                },
                loaded: function (key, value) {
                    this[key] = value;
                    if (this.isLoadComplete()) {
                        this.startRegion();
                    }
                },
                startRegion: function () {

                    this.model.set({
                        id: this.regionData.id,
                        layerOrder: this.regionData.layerOrder,
                    });

                    this.sortMultiLinks();
                    this.assignPeerLocations();

                    // TODO: RegionLinks are dublicated in JSON Payload

                    this.model.set({ subregions: t2sr.createSubRegionCollection(this.regionData.subregions, this) });
                    this.model.set({ devices: t2ds.createDeviceCollection(this.regionData.devices, this) });
                    this.model.set({ hosts: t2hs.createHostCollection(this.regionData.hosts, this) });
                    this.model.set({ peerRegions: t2pr.createCollection(this.peers, this) });
                    this.model.set({ links: t2ls.createLinkCollection(this.regionData.links, this) });

                    // Hide Breadcrumbs if there are no subregions configured in the root region
                    if (this.isRootRegion() && !this.model.get('subregions').models.length) {
                        t2bcs.hide();
                    }

                    this.layout.createForceLayout();
                    this.displayNoDevs();
                },
                clear: function () {
                    this.regionData = null;
                    this.createEmptyModel();
                },
                removePort: function (key) {
                    var regex = new RegExp('^[^/]*');
                    return regex.exec(key)[0];
                },
                assignPeerLocations: function () {
                    var _this = this;
                    _.each(this.regionData.peerLocations, function (location, id) {
                        _.each(_this.peers, function (peer) {
                            if (peer.id === id) {
                                peer.location = location;
                            }
                        });
                    });
                },
                sortMultiLinks: function () {
                    var _this = this,
                        deviceConnections = {};

                    _.each(this.regionData.links, function (link) {

                        var epA = _this.removePort(link.epA),
                            epB = _this.removePort(link.epB),
                            key = epA + '~' + epB,
                            collection = deviceConnections[key] || [],
                            dup = _.find(collection, link);

                        // TODO: Investigate why region contains dup links?!?!
                        // FIXME: This shouldn't be needed - The backend is sending dups
                        //        and this is preventing the client thinking its a multilink
                        if (!dup) {
                            collection.push(link);
                        }

                        deviceConnections[key] = collection;
                    });

                    _.forIn(deviceConnections, function (collection) {
                        if (collection.length > 1) {
                            _.each(collection, function (link, index) {
                                link.multiline = {
                                    deviceLinks: collection.length,
                                    index: index,
                                };
                            });
                        }
                    });
                },
                isRootRegion: function () {
                    return this.model.get('id') === ROOT;
                },
                findNodeById: function (link, id) {
                    if (link.get('type') !== 'UiEdgeLink') {
                        id = this.removePort(id);
                    }
                    return this.model.get('devices').get(id) ||
                        this.model.get('hosts').get(id) ||
                        this.model.get('subregions').get(id);
                },
                regionNodes: function () {
                    if (this.model) {
                        return [].concat(
                            this.model.get('devices').models,
                            this.model.get('hosts').models,
                            this.model.get('subregions').models,
                            this.model.get('peerRegions').models
                        );
                    }
                    return [];
                },
                regionLinks: function () {
                    return this.model.get('links').models;
                },
                getLink: function (linkId) {
                    return this.model.get('links').get(linkId);
                },
                getDevice: function (deviceId) {
                    return this.model.get('devices').get(deviceId);
                },
                filterRegionNodes: function (predicate) {
                    var nodes = this.regionNodes();
                    return _.filter(nodes, predicate);
                },
                deselectAllNodes: function () {
                    var selected = this.filterRegionNodes(function (node) {
                        return node.get('selected', true);
                    });

                    if (selected.length) {

                        selected.forEach(function (node) {
                            node.deselect();
                        });

                        t2dps().el.hide();
                        return true;
                    }

                    return false;
                },
                deselectLink: function () {
                    var selected = _.filter(this.regionLinks(), function (link) {
                        return link.get('selected', true);
                    });

                    if (selected.length) {

                        selected.forEach(function (link) {
                            link.deselect();
                        });

                        t2dps().el.hide();
                        return true;
                    }

                    return false;
                },
                toggleHosts: function () {
                    var state = this.lookupPrefState('hosts');
                    this.updatePrefState('hosts', !state);

                    _.each(this.model.get('hosts').models, function (host) {
                        host.setVisibility();
                    });

                    _.each(this.model.get('links').models, function (link) {
                        link.setVisibility();
                    });

                    return !state;
                },
                toggleOfflineDevices: function () {
                    var state = this.lookupPrefState('offline_devices');
                    this.updatePrefState('offline_devices', !state);
                    _.each(this.regionNodes(), function (node) {
                        node.setOfflineVisibility();
                    });

                    return !state;
                },
                update: function (event) {

                    if (!this.isLoadComplete()) {
                        this.layout.createForceLayout();
                    }

                    if (this[event.type]) {
                        this[event.type](event);
                    } else {
                        $log.error('Unhanded topology update', event);
                    }

                    this.layout.update();
                    this.displayNoDevs();
                },
                displayNoDevs: function () {
                    if (this.regionNodes().length > 0) {
                        t2ndcs.hide();
                    } else {
                        t2ndcs.show();
                    }
                },

                // Topology update event handlers
                LINK_ADDED_OR_UPDATED: function (event) {

                    var regionLinks = this.model.get('links');

                    if (!regionLinks) {
                        this.model.set({ links: t2ls.createLinkCollection([], this) });
                    }

                    if (event.memo === 'added') {
                        var link = this.model.get('links').add(event.data);
                        link.createLink();
                        $log.debug('Added Link', link);
                    }
                },
                LINK_REMOVED: function (event) {
                    var link = this.getLink(event.subject);
                    link.remove();
                    this.model.get('links').remove(link);
                },
                DEVICE_ADDED_OR_UPDATED: function (event) {

                    var regionDevices = this.model.get('devices'),
                        device;

                    if (!regionDevices) {
                        this.model.set({ devices: t2ds.createDeviceCollection([], this) });
                    }

                    if (event.memo === 'added') {
                        device = this.model.get('devices').add(event.data);
                        $log.debug('Added device', device);
                    } else if (event.memo === 'updated') {
                        device = this.getDevice(event.subject);
                        device.set(event.data);
                    }
                },
                DEVICE_REMOVED: function (event) {
                    device.remove();
                },
                HOST_ADDED_OR_UPDATED: function (event) {
                    var regionHosts = this.model.get('hosts'),
                        host;

                    if (!regionHosts) {
                        this.model.set({ hosts: t2hs.createHostCollection([], this) });
                    }

                    if (event.memo === 'added') {
                        host = this.model.get('hosts').add(event.data);
                        $log.debug('Added host', host);
                    }
                },
                REGION_ADDED_OR_UPDATED: function (event) {
                    var regionSubRegions = this.model.get('subregions'),
                        region;

                    if (!regionSubRegions) {
                        this.model.set({ subregions: t2sr.createSubRegionCollection([], this) });
                    }

                    if (event.memo === 'added') {
                        region = this.model.get('subregions').add(event.data);
                        $log.debug('Added region', region);
                    }
                },
            });

            function getInstance() {
                return instance || new Region();
            }

            return getInstance();

        }]);

})();
