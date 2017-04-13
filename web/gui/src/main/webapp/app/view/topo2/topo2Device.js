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
 ONOS GUI -- Topology Devices Module.
 Module that holds the devices for a region
 */

(function () {
    'use strict';

    var Collection, Model;

    var remappedDeviceTypes = {
        switch: 'm_switch',
        virtual: 'cord'
    };

    function createDeviceCollection(data) {

        var DeviceCollection = Collection.extend({
            model: Model
        });

        var devices = [];
        data.forEach(function (deviceLayer) {
            deviceLayer.forEach(function (device) {
                devices.push(device);
            });
        });

        return new DeviceCollection(devices);
    }

    angular.module('ovTopo2')
    .factory('Topo2DeviceService', [
            'Topo2Collection', 'Topo2NodeModel', 'Topo2DeviceDetailsPanel',
            'PrefsService',
            function (_c_, _nm_, detailsPanel, ps) {

                Collection = _c_;

                Model = _nm_.extend({

                    nodeType: 'device',
                    multiSelectEnabled: true,
                    events: {
                        'click': 'onClick'
                    },

                    initialize: function () {
                        this.super = this.constructor.__super__;
                        this.super.initialize.apply(this, arguments);
                    },
                    onChange: function () {
                        if (this.el) {
                            this.el.attr('class', this.svgClassName());
                            var rect = this.el.select('.icon-rect');
                            rect.style('fill', this.devGlyphColor());

                            this.setOfflineVisibility();
                        }
                    },
                    icon: function () {
                        var type = this.get('type');
                        return remappedDeviceTypes[type] || type || 'unknown';
                    },
                    showDetails: function () {
                        var id = this.get('id'),
                            nodeType = this.get('nodeType');
                        detailsPanel.updateDetails(id, nodeType);
                        detailsPanel.show();
                    },
                    displayMastership: function () {
                        var id = this.mastershipService.mastership(),
                            suppress = id ? this.get('master') !== id : false;

                        this.set({mastership: suppress});
                    },
                    setOfflineVisibility: function () {
                        var showOffline = ps.getPrefs('topo2_prefs')['offline_devices'],
                            display = this.get('online') || showOffline;
                        this.el.style('visibility', display ? 'visible' : 'hidden');
                    },
                    onExit: function () {
                        var node = this.el;
                        node.select('use')
                            .style('opacity', 0.5)
                            .transition()
                            .duration(800)
                            .style('opacity', 0);

                        node.selectAll('rect')
                            .style('stroke-fill', '#555')
                            .style('fill', '#888')
                            .style('opacity', 0.5);
                    }
                });

                return {
                    createDeviceCollection: createDeviceCollection
                };
            }
        ]);

})();
