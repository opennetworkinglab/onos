/*
 * Copyright 2016-present Open Networking Laboratory
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
        'Topo2MapConfigService', 'Topo2PeerRegionService',
        function ($log, _Model_, t2sr, t2ds, t2hs, t2ls, t2zs, t2dps, t2bcs, ViewController,
                  t2sls, t2ms, t2mcs, t2pr) {

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
                        nodes: this.regionNodes.bind(this)
                    });

                    this.model = new RegionModel();
                },
                loaded: function (key, value) {
                    this[key] = value;
                    if (this.bgRendered && this.regionData && this.peers) {
                        this.startRegion();
                    }
                },
                startRegion: function () {

                    var _this = this;

                    this.model.set({
                        id: this.regionData.id,
                        layerOrder: this.regionData.layerOrder
                    });

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
                },
                clear: function () {

                    this.regionData = null;

                    if (!this.model)
                        return;
                },
                isRootRegion: function () {
                    return this.model.get('id') === ROOT;
                },
                findNodeById: function (link, id) {
                    if (link.get('type') !== 'UiEdgeLink') {
                        // Remove /{port} from id if needed
                        var regex = new RegExp('^[^/]*');
                        id = regex.exec(id)[0];
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
                    console.log('remove link')
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

                update: function (event) {

                    if (this[event.type]) {
                        this[event.type](event);
                    } else {
                        $log.error("Unhanded topology update", event);
                    }

                    this.layout.update()
                },

                // Topology update event handlers
                LINK_ADDED_OR_UPDATED: function (event) {
                    if (event.memo === 'added') {
                        var link = this.model.get('links').add(event.data);
                        link.createLink();
                    }
                },
                LINK_REMOVED: function (event) {
                    var link = this.getLink(event.subject);
                    link.remove();
                    this.model.get('links').remove(link);
                },
                DEVICE_ADDED_OR_UPDATED: function (event) {

                    var device;

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
                }
            });

            function getInstance() {
                return instance || new Region();
            }

            return getInstance();

        }]);

})();
