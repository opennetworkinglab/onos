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
 ONOS GUI -- Topology SubRegion Module.
 Module that holds the sub-regions for a region
 */

(function () {
    'use strict';

    var Collection, Model;

    var remappedDeviceTypes = {
        virtual: 'cord',
    };

    function createSubRegionCollection(data, region) {

        var SubRegionCollection = Collection.extend({
            model: Model,
            region: region,
        });

        return new SubRegionCollection(data);
    }

    angular.module('ovTopo2')
    .factory('Topo2SubRegionService', [
        '$location', 'WebSocketService', 'Topo2Collection', 'Topo2NodeModel',
        'Topo2SubRegionPanelService', 'Topo2RegionNavigationService',

        function ($loc, wss, _c_, NodeModel, t2srp, t2rns) {

            Collection = _c_;

            Model = NodeModel.extend({

                nodeType: 'sub-region',
                events: {
                    'dblclick': 'navigateToRegion',
                    'click': 'onClick',
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
                showDetails: function () {
                    t2srp.displayPanel(this);
                },
                icon: function () {
                    var type = this.get('type');
                    return remappedDeviceTypes[type] || type || 'm_cloud';
                },
                navigateToRegion: function () {
                    if (d3.event.defaultPrevented) return;
                    t2rns.navigateToRegion(this.get('id'));
                    t2srp.hide();
                },
            });

            return {
                createSubRegionCollection: createSubRegionCollection,
            };
        },
    ]);

})();
