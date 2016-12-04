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

    var hostIconDim = 20,
        hostIconDimMin = 15,
        hostIconDimMax = 20,
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
        'IconService', 'Topo2ZoomService', 'Topo2HostsPanelService', 
        function (_Collection_, _NodeModel_, _t2vs_, is, zs, t2hds) {

            Collection = _Collection_;

            Model = _NodeModel_.extend({
                initialize: function () {
                    this.super = this.constructor.__super__;
                    this.super.initialize.apply(this, arguments);
                },
                events: {
                    'click': 'onClick'
                },
                onChange: function () {
                    // Update class names when the model changes
                    if (this.el) {
                        this.el.attr('class', this.svgClassName());
                    }
                },
                onClick: function () {
                    var selected = this.select(d3.select);

                    if (selected.length > 0) {
                        t2hds.displayPanel(this);
                    } else {
                        t2hds.hide();
                    }
                },
                nodeType: 'host',
                icon: function () {
                    var type = this.get('type');
                    return remappedDeviceTypes[type] || type || 'endstation';
                },
                label: function () {
                    var labelText = this.get('id'),
                        ips = this.get('ips');

                    if (this.labelIndex() === 0) {
                        return '';
                    }

                    if (ips && ips.length > 0) {
                        labelText = ips[0];
                    }

                    return labelText;
                },
                setScale: function () {

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
                onEnter: function (el) {
                    var node = d3.select(el),
                        icon = this.icon(),
                        iconDim = hostIconDim,
                        textDy = 5,
                        textDx = (hostIconDim * 2) + 20;

                    this.el = node;

                    var g = node.append('g')
                        .attr('class', 'svgIcon hostIcon');

                    g.append('circle').attr('r', hostIconDim);
                    g.append('use').attr({
                        'xlink:href': '#' + icon,
                        width: iconDim,
                        height: iconDim,
                        x: -iconDim / 2,
                        y: -iconDim / 2
                    });

                    var labelText = this.label();

                    g.append('text')
                        .text(labelText)
                        .attr('dy', textDy)
                        .attr('dx', textDx)
                        .attr('text-anchor', 'middle');

                    this.setScale();
                    this.setUpEvents();
                }
            });

            return {
                createHostCollection: createHostCollection
            };
        }
    ]);

})();
