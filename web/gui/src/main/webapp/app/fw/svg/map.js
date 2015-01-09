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
    The Map Service caches GeoJSON maps, which can be loaded into the map
    layer of the Topology View.

    A GeoMap object can be fetched by ID. IDs that start with an asterisk
    identify maps bundled with the GUI. IDs that do not start with an
    asterisk are assumed to be URLs to externally provided data (exact
    format to be decided).

    e.g.  var geomap = MapService.fetchGeoMap('*continental-us');

    The GeoMap object encapsulates topology data (features), and the
    D3 projection object.

    Note that, since the GeoMap instance is cached / shared, it should
    contain no state.
 */

(function () {
    'use strict';

    // injected references
    var $log, $http, $q, fs;

    // internal state
    var maps = d3.map(),
        msgMs = 'MapService.',
        bundledUrlPrefix = '../data/map/';

    function getUrl(id) {
        if (id[0] === '*') {
            return bundledUrlPrefix + id.slice(1) + '.json';
        }
        return id + '.json';
    }

    angular.module('onosSvg')
        .factory('MapService', ['$log', '$http', '$q', 'FnService',

        function (_$log_, _$http_, _$q_, _fs_) {
            $log = _$log_;
            $http = _$http_;
            $q = _$q_;
            fs = _fs_;

            function clearCache() {
                maps = d3.map();
            }

            // NOTE: It is expected that mapLayer is a D3 selection of the
            //       <g> element (a child of zoomLayer) into which the map
            //       path data will be rendered.
            function renderMap(mapLayer) {
                // TODO ---
                $log.log('Hey, let\'s render the map...');
            }

            function fetchGeoMap(id) {
                if (!fs.isS(id)) {
                    return null;
                }
                var url = getUrl(id);

                var promise = maps.get(id);

                if (!promise) {
                    // need to fetch the data and build the object...
                    var deferred = $q.defer();
                    promise = deferred.promise;

                    $http.get(url)
                        .success(function (data) {
                            deferred.resolve(data);
                        })
                        .error(function (msg, code) {
                            deferred.reject(msg);
                            $log.warn(msg, code);
                        });

                    promise.meta = {
                        id: id,
                        url: url,
                        wasCached: false,
                        render: renderMap
                    };

                    maps.set(id, promise);
                } else {
                    promise.meta.wasCached = true;
                }

                return promise;
            }

            return {
                clearCache: clearCache,
                fetchGeoMap: fetchGeoMap
            };
        }]);

}());
