/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- SVG -- GeoData Service
 */

/*
 The GeoData Service facilitates the fetching and caching of TopoJSON data
 from the server, as well as providing a way of creating a path generator
 for that data, to be used to render the map in an SVG layer.

 A TopoData object can be fetched by ID. IDs that start with an asterisk
 identify maps bundled with the GUI. IDs that do not start with an
 asterisk are assumed to be URLs to externally provided data.

     var topodata = GeoDataService.fetchTopoData('*continental-us');

 The path generator can then be created for that data-set:

     var gen = GeoDataService.createPathGenerator(topodata, opts);

 opts is an optional argument that allows the override of default settings:
     {
         objectTag: 'states',
         projection: d3.geo.mercator(),
         logicalSize: 1000,
         mapFillScale: .95
     };

 The returned object (gen) comprises transformed data (TopoJSON -> GeoJSON),
 the D3 path generator function, and the settings used ...

    {
        geodata:  { ... },
        pathgen:  function (...) { ... },
        settings: { ... }
    }
 */

(function () {
    'use strict';

    // injected references
    var $log, $http, fs;

    // internal state
    var cache = d3.map(),
        bundledUrlPrefix = 'data/map/';

    function getUrl(id) {
        if (id[0] === '*') {
            return bundledUrlPrefix + id.slice(1) + '.topojson';
        }
        return id + '.topojson';
    }


    // start afresh...
    function clearCache() {
        cache = d3.map();
    }

    // returns a promise decorated with:
    //   .meta -- id, url, and whether the data was cached
    //   .topodata -- TopoJSON data (on response from server)

    function fetchTopoData(id) {
        if (!fs.isS(id)) {
            return null;
        }
        var url = getUrl(id),
            promise = cache.get(id);

        if (!promise) {
            // need to fetch the data, build the object,
            // cache it, and return it.
            promise = $http.get(url);

            promise.meta = {
                id: id,
                url: url,
                wasCached: false
            };

            promise.then(function (response) {
                // success
                promise.topodata = response.data;
            }, function (response) {
                // error
                $log.warn('Failed to retrieve map TopoJSON data: ' + url,
                    response.status, response.data);
            });

            cache.set(id, promise);

        } else {
            promise.meta.wasCached = true;
        }

        return promise;
    }

    var defaultGenSettings = {
        objectTag: 'states',
        projection: d3.geo.mercator(),
        logicalSize: 1000,
        mapFillScale: .95
    };

    // converts given TopoJSON-format data into corresponding GeoJSON
    //  data, and creates a path generator for that data.
    function createPathGenerator(topoData, opts) {
        var settings = angular.extend({}, defaultGenSettings, opts),
            topoObject = topoData.objects[settings.objectTag],
            geoData = topojson.feature(topoData, topoObject),
            proj = settings.projection,
            dim = settings.logicalSize,
            mfs = settings.mapFillScale,
            path = d3.geo.path().projection(proj);

        rescaleProjection(proj, mfs, dim, path, geoData, opts.adjustScale);

        // return the results
        return {
            geodata: geoData,
            pathgen: path,
            settings: settings
        };
    }

    function rescaleProjection(proj, mfs, dim, path, geoData, adjustScale) {
        var adj = adjustScale || 1;
        // adjust projection scale and translation to fill the view
        // with the map

        // start with unit scale, no translation..
        proj.scale(1).translate([0, 0]);

        // figure out dimensions of map data..
        var b = path.bounds(geoData),
            x1 = b[0][0],
            y1 = b[0][1],
            x2 = b[1][0],
            y2 = b[1][1],
            dx = x2 - x1,
            dy = y2 - y1,
            x = (x1 + x2) / 2,
            y = (y1 + y2) / 2;

        // size map to 95% of minimum dimension to fill space..
        var s = (mfs / Math.min(dx / dim, dy / dim)) * adj,
            t = [dim / 2 - s * x, dim / 2 - s * y];

        // set new scale, translation on the projection..
        proj.scale(s).translate(t);
    }

    angular.module('onosSvg')
        .factory('GeoDataService', ['$log', '$http', 'FnService',
        function (_$log_, _$http_, _fs_) {
            $log = _$log_;
            $http = _$http_;
            fs = _fs_;


            return {
                clearCache: clearCache,
                fetchTopoData: fetchTopoData,
                createPathGenerator: createPathGenerator,
                rescaleProjection: rescaleProjection
            };
        }]);
}());