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

    function createDeviceCollection(data, region) {

        var DeviceCollection = Collection.extend({
            model: Model,
            get: function () {},
            comparator: function(a, b) {

                var order = region.layerOrder;
                return order.indexOf(a.get('layer')) - order.indexOf(b.get('layer'));
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

    angular.module('ovTopo2')
    .factory('Topo2DeviceService',
        ['Topo2Collection', 'Topo2Model',

            function (_Collection_, _Model_) {

                Collection = _Collection_;
                Model = _Model_.extend({});

                return {
                    createDeviceCollection: createDeviceCollection
                };
            }
        ]);

})();
