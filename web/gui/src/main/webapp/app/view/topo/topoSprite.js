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
 ONOS GUI -- Topology Sprite Module.
 Defines behavior for loading sprites.
 */

(function () {
    'use strict';

    // injected refs
    var $log, $http, fs, sus;

    // internal state
    var spriteLayer,
        cache = d3.map();

    // constants
    var urlPrefix = 'data/ext/';

    function getUrl(id) {
        return urlPrefix + id + '.json';
    }

    // =========================

    function clearCache() {
        cache = d3.map();
    }


    function loadSpriteData(id, cb) {
        var url = getUrl(id),
            promise = cache.get(id);

        if (!promise) {
            // need to fetch data and cache it
            promise = $http.get(url);

            promise.meta = {
                id: id,
                url: url,
                wasCached: false
            };

            promise.then(function (response) {
                // success
                promise.spriteData = response.data;
                cb(promise.spriteData);
            }, function (response) {
                // error
                $log.warn('Failed to retrieve sprite data: ' + url,
                    response.status, response.data);
            });

        } else {
            promise.meta.wasCached = true;
            cb(promise.spriteData);
        }
    }

    function doSprite(def, item) {
        var g;

        function xfm(x, y, s) {
            return sus.translate([x,y]) + sus.scale(s, s);
        }

        g = spriteLayer.append('g')
            .classed(def['class'], true)
            .attr('transform', xfm(item.x, item.y, def.scale));

        if (item.label) {
            g.append('text')
                .text(item.label)
                .attr({
                    x: def.width / 2,
                    y: def.height * def.textyoff
                });
        }

        g.append('use').attr({
            width: def.width,
            height: def.height,
            'xlink:href': '#' + def.use
        });
    }

    function loadSprites(layer) {
        spriteLayer = layer;

        loadSpriteData('sprites', function (data) {
            var defs = {};

            $log.debug("Loading sprites...", data.file_desc);

            data.defn.forEach(function (d) {
                defs[d.id] = d;
            });

            data.load.forEach(function (item) {
                doSprite(defs[item.id], item);
            });
        });

    }


    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoSpriteService',
        ['$log', '$http', 'FnService', 'SvgUtilService',

        function (_$log_, _$http_, _fs_, _sus_) {
            $log = _$log_;
            $http = _$http_;
            fs = _fs_;
            sus = _sus_;

            return {
                clearCache: clearCache,
                loadSprites: loadSprites
            };
        }]);

}());
