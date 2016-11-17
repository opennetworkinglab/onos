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

    var wss;
    var Collection, Model;

    var remappedDeviceTypes = {
        virtual: 'cord'
    };

    function createSubRegionCollection(data, region) {

        var SubRegionCollection = Collection.extend({
            model: Model
        });

        return new SubRegionCollection(data);
    }

    angular.module('ovTopo2')
    .factory('Topo2SubRegionService',
        ['WebSocketService', 'Topo2Collection', 'Topo2NodeModel',
        'ThemeService', 'Topo2ViewService', 'Topo2SubRegionPanelService',

            function (_wss_, _c_, _NodeModel_, _ts_, _t2vs_m, _t2srp_) {

                wss = _wss_;
                Collection = _c_;

                Model = _NodeModel_.extend({
                    initialize: function () {
                        this.super = this.constructor.__super__;
                        this.super.initialize.apply(this, arguments);
                    },
                    events: {
                        'dblclick': 'navigateToRegion',
                        'click': 'onClick'
                    },
                    onChange: function () {
                        // Update class names when the model changes
                        if (this.el) {
                            this.el.attr('class', this.svgClassName());
                        }
                    },
                    nodeType: 'sub-region',
                    icon: function () {
                        var type = this.get('type');
                        return remappedDeviceTypes[type] || type || 'm_cloud';
                    },
                    onClick: function () {
                        var selected = this.select(d3.event);

                        if (selected.length > 0) {
                            _t2srp_.displayPanel(this);
                        } else {
                            _t2srp_.hide();
                        }
                    },
                    navigateToRegion: function () {

                        if (d3.event.defaultPrevented) return;

                        wss.sendEvent('topo2navRegion', {
                            dir: 'down',
                            rid: this.get('id')
                        });
                    }
                });

                return {
                    createSubRegionCollection: createSubRegionCollection
                };
            }
        ]);

})();
