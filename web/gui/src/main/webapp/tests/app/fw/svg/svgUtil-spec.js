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
 ONOS GUI -- SVG -- SVG Util Service - Unit Tests
 */
describe('factory: fw/svg/svgUtil.js', function() {
    var $log, fs, sus, d3Elem;

    beforeEach(module('onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, FnService, SvgUtilService) {
        $log = _$log_;
        fs = FnService;
        sus = SvgUtilService;
        d3Elem = d3.select('body').append('svg').append('defs').attr('id', 'myDefs');
    }));

    afterEach(function () {
        d3.select('svg').remove();
    });

    it('should define SvgUtilService', function () {
        expect(sus).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(sus, [
            'createDragBehavior', 'loadGlow', 'cat7', 'translate'
        ])).toBeTruthy();
    });


    // TODO: add unit tests for drag behavior
    // TODO: add unit tests for loadGlow
    // TODO: add unit tests for cat7


    // === translate()

    it('should translate from two args', function () {
        expect(sus.translate(1,2)).toEqual('translate(1,2)');
    });

    it('should translate from an array', function () {
        expect(sus.translate([3,4])).toEqual('translate(3,4)');
    });


    // === stripPx()

    it('should not affect a number', function () {
        expect(sus.stripPx('4')).toEqual('4');
    });

    it('should remove trailing px', function () {
        expect(sus.stripPx('4px')).toEqual('4');
    });
});
