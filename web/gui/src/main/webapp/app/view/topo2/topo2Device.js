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

    var Collection, Model, is, sus, ts;

    var remappedDeviceTypes = {
        virtual: 'cord'
    };

    // configuration
    var devIconDim = 36,
        halfDevIcon = devIconDim / 2;

    function createDeviceCollection(data, region) {

        var DeviceCollection = Collection.extend({
            model: Model,
            comparator: function (a, b) {
                var order = region.get('layerOrder'),
                    aLayer = a.get('layer'),
                    bLayer = b.get('layer');
                return order.indexOf(aLayer - order.indexOf(bLayer));
            }
        });

        var devices = [];
        data.forEach(function (deviceLayer) {
            deviceLayer.forEach(function (device) {
                devices.push(device);
            });
        });

        var deviceCollection = new DeviceCollection(devices);
        deviceCollection.sort();

        return deviceCollection;
    }

    function mapDeviceTypeToGlyph(type) {
        return remappedDeviceTypes[type] || type || 'unknown';
    }

    // note: these are the device icon colors without affinity (no master)
    var dColTheme = {
        light: {
            online: '#444444',
            offline: '#cccccc'
        },
        dark: {
            // TODO: theme
            online: '#444444',
            offline: '#cccccc'
        }
    };

    function deviceGlyphColor(d) {
        var o = this.node.online,
            id = this.node.master, // TODO: This should be from node.master
            otag = o ? 'online' : 'offline';

        return o ? sus.cat7().getColor(id, 0, ts.theme()) :
            dColTheme[ts.theme()][otag];
    }

    function setDeviceColor() {
        this.el.select('use')
            .style('fill', this.deviceGlyphColor());
    }

    angular.module('ovTopo2')
    .factory('Topo2DeviceService',
        ['Topo2Collection', 'Topo2NodeModel', 'IconService',
        'SvgUtilService', 'ThemeService',

            function (_c_, _nm_, _is_, _sus_, _ts_, classnames) {

                is = _is_;
                sus = _sus_;
                ts = _ts_;
                Collection = _c_;

                Model = _nm_.extend({
                    initialize: function () {
                        this.set('weight', 0);
                        this.constructor.__super__.initialize.apply(this, arguments);
                    },
                    nodeType: 'device',
                    deviceGlyphColor: deviceGlyphColor,
                    mapDeviceTypeToGlyph: mapDeviceTypeToGlyph,
                    setDeviceColor: setDeviceColor,
                    onEnter: function (el) {

                        var node = d3.select(el),
                            glyphId = mapDeviceTypeToGlyph(this.get('type')),
                            label = this.trimLabel(this.label()),
                            glyph, labelWidth;

                        this.el = node;

                        // Label
                        var labelElements = this.addLabelElements(label);
                        labelWidth = label ? this.computeLabelWidth(node) : 0;
                        labelElements.rect.attr(this.iconBox(devIconDim, labelWidth));

                        // Icon
                        glyph = is.addDeviceIcon(node, glyphId, devIconDim);
                        glyph.attr(this.iconBox(devIconDim, 0));

                        node.attr('transform', sus.translate(-halfDevIcon, -halfDevIcon));
                        this.render();
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
                    },
                    render: function () {
                        this.setDeviceColor();
                    }
                });

                return {
                    createDeviceCollection: createDeviceCollection
                };
            }
        ]);

})();
