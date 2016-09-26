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
 ONOS GUI -- Topology Force Module.
 Visualization of the topology in an SVG layer, using a D3 Force Layout.
 */

(function () {
    'use strict';

    angular.module('ovTopo2')
    .factory('Topo2CountryFilters', [
        function (_$log_, _wss_, _t2ds_) {
            return {
                s_america: function (c) {
                    return c.properties.continent === 'South America';
                },
                ns_america: function (c) {
                    return c.properties.custom === 'US-cont' ||
                        c.properties.subregion === 'Central America' ||
                        c.properties.continent === 'South America';
                },
                japan: function (c) {
                    return c.properties.geounit === 'Japan';
                },
                europe: function (c) {
                    return c.properties.continent === 'Europe';
                },
                italy: function (c) {
                    return c.properties.geounit === 'Italy';
                },
                uk: function (c) {
                    // technically, Ireland is not part of the United Kingdom,
                    // but the map looks weird without it showing.
                    return c.properties.adm0_a3 === 'GBR' ||
                        c.properties.adm0_a3 === 'IRL';
                },
                s_korea: function (c) {
                    return c.properties.adm0_a3 === 'KOR';
                },
                australia: function (c) {
                    return c.properties.adm0_a3 === 'AUS';
                }
            };
        }
    ]);
})();
