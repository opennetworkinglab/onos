/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- SVG -- Map Service

 @author Simon Hunt
 */

/*
    The Map Service provides a simple API for loading geographical maps into
    an SVG layer. For example, as a background to the Topology View.

    e.g.  var ok = MapService.loadMapInto(svgLayer, '*continental-us');

    The Map Service makes use of the GeoDataService to load the required data
    from the server and to create the appropriate geographical projection.

*/

(function () {
    'use strict';

    // injected references
    var $log, fs, gds;

    angular.module('onosSvg')
        .factory('MapService', ['$log', 'FnService', 'GeoDataService',
        function (_$log_, _fs_, _gds_) {
            $log = _$log_;
            fs = _fs_;
            gds = _gds_;

            function loadMapInto(mapLayer, id, opts) {
                var promise = gds.fetchTopoData(id);
                if (!promise) {
                    $log.warn('Failed to load map: ' + id);
                    return false;
                }

                promise.then(function () {
                    var gen = gds.createPathGenerator(promise.topodata, opts);

                    mapLayer.selectAll('path')
                        .data(gen.geodata.features)
                        .enter()
                        .append('path')
                        .attr('d', gen.pathgen);
                });
                return true;
            }

            return {
                loadMapInto: loadMapInto
            };
        }]);

}());
