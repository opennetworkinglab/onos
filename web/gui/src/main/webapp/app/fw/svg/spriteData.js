/*
 * Copyright 2017-present Open Networking Laboratory
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
 * ONOS GUI -- SVG -- Sprite Data Service
 *  Bundled sprite and layout definitions.
 */
(function () {
    'use strict';

    // ----------------------------------------------------------------------
    // === Sprite Data ===

    var cloud = {
        // TODO: define cloud sprite...
        vbox: '0 0 305 186',
        d: [
            "M91.2,48.4C121.2,6.3,187.9-13.4,219,45.6",
            "M43.1,139.6C21.8,142.9-15.6,108.4,26.1,79",
            "M103.7,150C89,205.2-11.2,167.4,30.5,138",
            "M192.3,147.3c-33.5,48-82.1,32.3-94.5-8.2",
            "M267.1,115c27.9,67.8-77.6,74.3-83.1,41",
            "M34.3,89.9C10.8,79,59.5,10.7,97.2,39.6",
            "M211.9,34.2c51.9-38.8,118,57.4,59,94.5"
        ],
        style: {
            fill: 'none',
            'stroke-miterlimit': 10
        }
    };

    // TODO: define other core sprites here...

    // ----------------------------------------------------------------------
    // === API functions ===

    function registerCoreSprites(ssApi) {
        ssApi.registerSprite(cloud);

        // TODO: add base set of sprites here ...

        // ----------------------------------------------------------$$$
        // This following code is for initial development of Topo2 sprite layer
        ssApi.createSprite('rack', 40, 50)
            .addRect(0, 0, 40, 50, {fill: 'gold1'})
            .addPath([
                'M5,20h30v5h-30z',
                'M5,30h30v5h-30z',
                'M5,40h30v5h-30z'
            ], {stroke: 'gray1'})
            .register();

        ssApi.createLayout('segmentRouting', 130, 75)
            .addSprite('rack', 10, 40, 20)
            .addSprite('rack', 40, 40, 20)
            .addSprite('rack', 70, 40, 20)
            .addSprite('rack', 100, 40, 20)
            .addLabel('Segment Routing', 120, 10, {anchor: 'right'})
            .register();

        ssApi.createLayout('segmentRoutingTwo', 70, 75)
            .addSprite('rack', 10, 40, 20)
            .addSprite('rack', 40, 40, 20)
            .addLabel('Segment Routing 2', 120, 10, {anchor: 'right'})
            .register();

        ssApi.createLayout('plain', 80, 60)
            .register();

        ssApi.dump();
        // ----------------------------------------------------------$$$
    }

    // ----------------------------------------------------------------------

    angular.module('onosSvg')
        .factory('SpriteDataService', [
            function () {
                return {
                    registerCoreSprites: registerCoreSprites
                };
            }
        ]);

}());
