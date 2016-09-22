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
 ONOS GUI -- Topology SubRegion Module.
 Module that holds the sub-regions for a region
 */

(function () {
    'use strict';

    var wss, is, sus;
    var Collection, Model;

    var remappedDeviceTypes = {
        virtual: 'cord'
    };

    // configuration
    var devIconDim = 36,
        halfDevIcon = devIconDim / 2;

    function createSubRegionCollection(data, region) {

        var SubRegionCollection = Collection.extend({
            model: Model
        });

        return new SubRegionCollection(data);
    }

    function mapDeviceTypeToGlyph(type) {
        return remappedDeviceTypes[type] || type || 'switch';
    }

    function iconBox(dim, labelWidth) {
        return {
            x: -dim / 2,
            y: -dim / 2,
            width: dim + labelWidth,
            height: dim
        };
    }

    angular.module('ovTopo2')
    .factory('Topo2SubRegionService',
        ['WebSocketService', 'Topo2Collection', 'Topo2NodeModel',
        'IconService', 'SvgUtilService', 'ThemeService', 'Topo2ViewService',

            function (_wss_, _c_, _NodeModel_, _is_, _sus_, _ts_, _t2vs_) {

                wss = _wss_;
                is = _is_;
                sus = _sus_;
                Collection = _c_;

                Model = _NodeModel_.extend({
                    initialize: function () {
                        this.set('weight', 0);
                        this.constructor.__super__.initialize.apply(this, arguments);
                    },
                    nodeType: 'sub-region',
                    mapDeviceTypeToGlyph: mapDeviceTypeToGlyph,
                    onClick: function () {

                        if (d3.event.defaultPrevented) return;

                        wss.sendEvent('topo2navRegion', {
                            dir: 'down',
                            rid: this.get('id')
                        });
                    },
                    onEnter: function (el) {

                        var node = d3.select(el),
                            glyphId = mapDeviceTypeToGlyph(this.get('type')),
                            label = this.trimLabel(this.label()),
                            glyph, labelWidth;

                        this.el = node;
                        this.el.on('click', this.onClick.bind(this));

                        // Label
                        var labelElements = this.addLabelElements(label);
                        labelWidth = label ? this.computeLabelWidth(node) : 0;
                        labelElements.rect.attr(iconBox(devIconDim, labelWidth));

                        // Icon
                        glyph = is.addDeviceIcon(node, glyphId, devIconDim);
                        glyph.attr(iconBox(devIconDim, 0));

                        node.attr('transform', sus.translate(-halfDevIcon, -halfDevIcon));
                        this.render();
                    },
                    onExit: function () {},
                    render: function () {}
                });

                return {
                    createSubRegionCollection: createSubRegionCollection
                };
            }
        ]);

})();
