/*
 * Copyright 2017-present Open Networking Foundation
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
 * ONOS GUI -- SVG -- Sprite Service
 *  For defining sprites and layouts (of sprites).
 */
(function () {
    'use strict';

    // injected references
    var $log, fs, sds;

    // configuration of default options
    var optDefaults = {
        sprite: {
            builder: {
                // none for now
            },
            addRect: {
                fill: 'gray1',
                stroke: 'none',
            },
            addPath: {
                fill: 'none',
                stroke: 'gray1',
            },
        },
        layout: {
            builder: {
                grid: 10, // grid square size (in layout coord-space)
            },
            addSprite: {
                anchor: 'topleft', // topleft, center
            },
            addLabel: {
                anchor: 'center', // center, left, right
                fontStyle: 'normal', // normal, italic, bold
            },
        },
    };

    // internal state
    var sprites, // sprite cache
        layouts, // layout cache
        api;

    // ----------------------------------------------------------------------
    // === Sprite Builder ===

    // Sample usage:
    //
    //     ss.createSprite('foo', 100, 100)
    //         .addPath('M40,40h20v20h-20z', {fill: 'gold1'})
    //         .addRect(50, 50, 10, 20, {stroke: 'gold1'})
    //         .register();

    function spriteBuilder(id, w, h, opts) {
        var o = angular.extend({}, optDefaults.sprite.builder, opts),
            builder,
            paths = [],
            rects = [];

        // TODO: verify id has not already been registered

        // x,y is top left corner; w,h is width and height of rectangle
        function addRect(x, y, w, h, opts) {
            var o = angular.extend({}, optDefaults.sprite.addRect, opts);

            rects.push({
                x: x, y: y, w: w, h: h, o: o,
            });
            return builder;
        }

        function addPath(d, opts) {
            var o = angular.extend({}, optDefaults.sprite.addPath, opts);

            if (fs.isS(d)) {
                paths.push({ d: d, o: o });
            } else if (fs.isA(d)) {
                paths.push({ d: d.join(''), o: o });
            } else {
                $log.warn('addPath: path not a string or array', d);
            }
            return builder;
        }

        function register() {
            sprites.set(id, builder);
        }

        // define the builder object...
        builder = {
            type: 'sprite',
            data: {
                id: id,
                w: w,
                h: h,
                opts: o,
            },
            paths: paths,
            rects: rects,

            // builder API
            addRect: addRect,
            addPath: addPath,
            register: register,
        };

        return builder;
    }

    // ----------------------------------------------------------------------
    // === Layout Builder ===

    // Sample usage:
    //
    //     ss.createLayout('fooLayout', 400, 300)
    //         .addSprite('foo', 10, 10, 40)
    //         .addSprite('foo', 60, 10, 40)
    //         .addSprite('foo', 110, 10, 40)
    //         .register();

    function layoutBuilder(id, w, h, opts) {
        var o = angular.extend({}, optDefaults.layout.builder, opts),
            builder,
            sprs = [],
            labs = [];

        // TODO: verify id has not already been registered

        function addSprite(id, x, y, w, opts) {
            var o = angular.extend({}, optDefaults.layout.addSprite, opts),
                s = sprites.get(id);

            if (!s) {
                $log.warn('no such sprite:', id);
                return builder;
            }

            sprs.push({
                sprite: s, x: x, y: y, w: w, anchor: o.anchor,
            });
            return builder;
        }

        function addLabel(text, x, y, opts) {
            var o = angular.extend({}, optDefaults.layout.addLabel, opts);

            labs.push({
                text: text, x: x, y: y, anchor: o.anchor, style: o.fontStyle,
            });
            return builder;
        }

        function register() {
            layouts.set(id, builder);
        }

        // define the builder object...
        builder = {
            type: 'layout',
            data: {
                id: id,
                w: w,
                h: h,
                opts: o,
            },
            sprites: sprs,
            labels: labs,

            // builder API
            addSprite: addSprite,
            addLabel: addLabel,
            register: register,
        };

        return builder;
    }

    // ----------------------------------------------------------------------
    // === API functions ===

    // Clears the sprite / layout caches.
    function clear() {
        sprites = d3.map();
        layouts = d3.map();
    }

    // Initializes the sprite / layout caches with core elements.
    function init() {
        sds.registerCoreSprites(api);
    }

    // Returns a sprite "builder", which can be used to programmatically
    // define a sprite.
    function createSprite(id, w, h) {
        $log.debug('createSprite:', id, w, 'x', h);
        return spriteBuilder(id, w, h);
    }

    // Returns a layout "builder", which can be used to programmatically
    // define a layout.
    function createLayout(id, w, h, opts) {
        $log.debug('createLayout:', id, w, 'x', h, '(opts:', opts, ')');
        return layoutBuilder(id, w, h, opts);
    }

    // Registers a sprite defined by the given object (JSON structure).
    function registerSprite(json) {
        $log.debug('registerSprite:', json);
        // TODO: create and register a sprite based on JSON data
    }

    // Registers a layout defined by the given object (JSON structure).
    function registerLayout(json) {
        $log.debug('registerLayout:', json);
        // TODO: create and register a layout based on JSON data
    }

    // Returns the sprite with the given ID, or undefined otherwise.
    function sprite(id) {
        return sprites.get(id);
    }

    // Returns the layout with the given ID, or undefined otherwise.
    function layout(id) {
        return layouts.get(id);
    }

    // Returns a count of registered sprites and layouts.
    function count() {
        return {
            sprites: sprites.size(),
            layouts: layouts.size(),
        };
    }

    // Dumps the cache contents to console
    function dump() {
        $log.debug('Dumping Caches...');
        $log.debug('sprites:', sprites);
        $log.debug('layouts:', layouts);
    }

    // ----------------------------------------------------------------------

    angular.module('onosSvg')
    .factory('SpriteService',
        ['$log', 'FnService', 'SpriteDataService',

        function (_$log_, _fs_, _sds_) {
            $log = _$log_;
            fs = _fs_;
            sds = _sds_;

            api = {
                clear: clear,
                init: init,
                createSprite: createSprite,
                createLayout: createLayout,
                registerSprite: registerSprite,
                registerLayout: registerLayout,
                sprite: sprite,
                layout: layout,
                count: count,
                dump: dump,
            };
            return api;
        }]
    )
    .run(['$log', function ($log) {
        $log.debug('Clearing sprite and layout caches');
        clear();
    }]);

}());
