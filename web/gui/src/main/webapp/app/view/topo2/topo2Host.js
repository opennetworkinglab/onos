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
 ONOS GUI -- Topology Hosts Module.
 Module that holds the hosts for a region
 */

(function () {
    'use strict';

    var Collection, Model;

    var hostIconDim = 15,
        hostIconDimMin = 8,
        hostIconDimMax = 15,
        remappedDeviceTypes = {};

    function createHostCollection(data, region) {

        var HostCollection = Collection.extend({
            model: Model
        });

        var hosts = [];
        data.forEach(function (hostsLayer) {
            hostsLayer.forEach(function (host) {
                hosts.push(host);
            });
        });

        return new HostCollection(hosts);
    }

    angular.module('ovTopo2')
    .factory('Topo2HostService', [
        'Topo2Collection', 'Topo2NodeModel', 'Topo2ViewService',
        'IconService', 'Topo2ZoomService', 'Topo2HostsPanelService', 'PrefsService',
        function (_c_, NodeModel, _t2vs_, is, zs, t2hds, ps) {

            Collection = _c_;

            Model = NodeModel.extend({

                nodeType: 'host',
                events: {
                    'click': 'onClick'
                },
                initialize: function () {
                    this.super = this.constructor.__super__;
                    this.super.initialize.apply(this, arguments);
                },
                onChange: function () {
                    // Update class names when the model changes
                    if (this.el) {
                        this.el.attr('class', this.svgClassName());
                    }
                },
                showDetails: function() {
                    t2hds.displayPanel(this);
                },
                icon: function () {
                    var type = this.get('type');
                    return remappedDeviceTypes[type] || type || 'm_endstation';
                },
                label: function () {
                    return this.get('ips')[0] || 'unknown';
                },
                setScale: function () {

                    if (!this.el) return;

                    var dim = hostIconDim,
                        multipler = 1;

                    if (dim * zs.scale() < hostIconDimMin) {
                        multipler = hostIconDimMin / (dim * zs.scale());
                    } else if (dim * zs.scale() > hostIconDimMax) {
                        multipler = hostIconDimMax / (dim * zs.scale());
                    }

                    this.el.select('g').selectAll('*')
                        .style('transform', 'scale(' + multipler + ')');
                },
                setVisibility: function () {
                    var visible = ps.getPrefs('topo2_prefs')['hosts'];
                    this.el.style('visibility', visible ? 'visible' : 'hidden');
                },
                onEnter: function (el) {
                    var node = d3.select(el),
                        icon = this.icon(),
                        textDy = 5,
                        textDx = (hostIconDim * 2) + 20;

                    this.el = node;

                    var g = node.append('g')
                        .attr('class', 'svgIcon hostIcon');

                    g.append('circle').attr('r', hostIconDim);

                    var glyphSize = hostIconDim * 1.5;
                    g.append('use').attr({
                        'xlink:href': '#' + icon,
                        width: glyphSize,
                        height: glyphSize,
                        x: -glyphSize / 2,
                        y: -glyphSize / 2
                    });

                    var labelText = this.label();

                    g.append('text')
                        .text(labelText)
                        .attr('dy', textDy)
                        .attr('dx', textDx)
                        .attr('text-anchor', 'middle');

                    this.setScale();
                    this.setUpEvents();
                    this.setVisibility();
                }
            });

            return {
                createHostCollection: createHostCollection
            };
        }
    ]);

})();
