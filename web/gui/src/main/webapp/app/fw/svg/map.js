/*
 * Copyright 2015-present Open Networking Foundation
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
 */

/*
    The Map Service provides a simple API for loading geographical maps into
    an SVG layer. For example, as a background to the Topology View.

    e.g.  var promise = MapService.loadMapInto(svgLayer, '*continental-us');

    The Map Service makes use of the GeoDataService to load the required data
    from the server and to create the appropriate geographical projection.

    A promise is returned to the caller, which is resolved with the
    map projection once created.
*/

(function () {
    'use strict';

    // injected references
    var $log, $q, gds;

    // NOTE: This method assumes the datafile has exactly the map data
    //       that you want to load; for example id="*continental_us"
    //       mapping to ~/data/map/continental_us.topojson contains
    //       exactly the paths for the continental US.

    function loadMapInto(mapLayer, mapPath, id, opts) {
        var promise = gds.fetchTopoData(mapPath),
            deferredProjection = $q.defer();

        if (!promise) {
            $log.warn('Failed to load map: ' + mapPath);
            return false;
        }

        promise.then(function () {

            // NOTE: This finds the topo object within the topojson file
            var topoObjects = promise.topodata.objects;

                if (topoObjects.hasOwnProperty(id)) {
                    opts.objectTag = id;
                }

            var gen = gds.createPathGenerator(promise.topodata, opts);

            deferredProjection.resolve(gen.settings.projection);

            mapLayer.selectAll('path')
                .data(gen.geodata.features)
                .enter()
                .append('path')
                .attr('d', gen.pathgen);

            reshade(opts.shading);
        });
        return deferredProjection.promise;
    }

    // ---

    // NOTE: This method uses the countries.topojson data file, and then
    //       filters the results based on the supplied options.
    // Usage:
    //     promise = loadMapRegionInto(svgGroup, {
    //         countryFilter: function (country) {
    //             return country.properties.continent === 'South America';
    //         }
    //     });

    function loadMapRegionInto(mapLayer, opts) {
        var promise = gds.fetchTopoData('*countries'),
            deferredProjection = $q.defer();

        if (!promise) {
            $log.warn('Failed to load countries TopoJSON data');
            return false;
        }

        promise.then(function () {
            var width = 1000,
                height = 1000,
                proj = d3.geo.mercator().translate([width/2, height/2]),
                pathGen = d3.geo.path().projection(proj),
                data = promise.topodata,
                features = topojson.feature(data, data.objects.countries).features,
                country = features.filter(opts.countryFilter),
                countryFeature = {
                    type: 'FeatureCollection',
                    features: country,
                },
                path = d3.geo.path().projection(proj);

            gds.rescaleProjection(proj, 0.95, 1000, path, countryFeature, opts.adjustScale);

            deferredProjection.resolve(proj);

            mapLayer.selectAll('path.country')
                .data([countryFeature])
                .enter()
                .append('path').classed('country', true)
                .attr('d', pathGen);

            reshade(opts.shading);
        });
        return deferredProjection.promise;
    }

    function reshade(sh) {
        var p = sh && sh.palette,
            paths, stroke, fill, bg,
            svg = d3.select('#ov-topo').select('svg');
        if (sh) {
            stroke = p.outline;
            fill = sh.flip ? p.sea : p.land;
            bg = sh.flip ? p.land : p.sea;

            paths = d3.select('#topo-map').selectAll('path');
            svg.style('background-color', bg);
            paths.attr({
                stroke: stroke,
                fill: fill,
            });
        } else {
            svg.style('background-color', null);
        }
    }

    angular.module('onosSvg')
        .factory('MapService', ['$log', '$q', 'GeoDataService',
        function (_$log_, _$q_, _gds_) {
            $log = _$log_;
            $q = _$q_;
            gds = _gds_;

            return {
                loadMapRegionInto: loadMapRegionInto,
                loadMapInto: loadMapInto,
                reshade: reshade,
            };
        }]);

}());
