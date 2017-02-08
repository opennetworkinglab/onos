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
 ONOS GUI -- SVG -- Sprite Service - Unit Tests
 */

describe('factory: fw/svg/sprite.js', function () {
    var $log, fs, ss, d3Elem, svg;

    // config...
    var numBaseSprites = 1,
        sampleData = 'M91.2,48.4C121.2,6.3,187.9-13.4,219,45.6';

    // load modules we need
    beforeEach(module('onosUtil', 'onosSvg'));

    // inject the services we need
    beforeEach(inject(function (_$log_, FnService, SpriteService) {
        var body = d3.select('body');
        $log = _$log_;
        fs = FnService;
        ss = SpriteService;

        // NOTE: once we get to loading sprites into the DOM, we'll need these:
        // d3Elem = body.append('defs').attr('id', 'myDefs');
        // svg = body.append('svg').attr('id', 'mySvg');
    }));

    // clean up after each test
    afterEach(function () {
        // d3.select('#mySvg').remove();
        // d3.select('#myDefs').remove();
        ss.clear();
    });

    // === UNIT TESTS ===

    it('should define SpriteService', function () {
        expect(ss).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ss, [
            'clear', 'init',
            'createSprite', 'createLayout',
            'registerSprite', 'registerLayout',
            'sprite', 'layout',
            'count', 'dump'
        ])).toBe(true);
    });

    it('should start no sprites or layouts', function () {
        var c = ss.count();
        expect(c.sprites).toBe(0);
        expect(c.layouts).toBe(0);
    });

    //    Programmatic build of a sprite
    it('should register a simple sprite', function () {
        ss.createSprite('foo', 303, 185)
            .addPath(sampleData)
            .register();

        expect(ss.count().sprites).toBe(1);

        var s = ss.sprite('foo');
        expect(s).toBeDefined();
        // TODO: verify internal structure of sprite

    });
});