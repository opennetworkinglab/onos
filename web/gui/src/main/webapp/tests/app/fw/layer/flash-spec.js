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
 ONOS GUI -- Layer -- Flash Service - Unit Tests
 */
describe('factory: fw/layer/flash.js', function () {
    var $log, $timeout, fs, flash, d3Elem;

    beforeEach(module('onosLayer'));

    beforeEach(inject(function (_$log_, _$timeout_, FnService, FlashService) {
        $log = _$log_;
        $timeout = _$timeout_;
        fs = FnService;
        flash = FlashService;
        jasmine.clock().install();
        d3Elem = d3.select('body').append('div').attr('id', 'myflashdiv');
        flash.initFlash();
    }));

    afterEach(function () {
        jasmine.clock().uninstall();
        d3.select('#myflashdiv').remove();
    });

    function flashItemSelection() {
        return d3Elem.selectAll('.flashItem');
    }

    it('should define FlashService', function () {
        expect(flash).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(flash, [
            'initFlash', 'flash', 'enable', 'tempDiv'
        ])).toBe(true);
    });

    it('should have no items to start', function () {
        expect(flashItemSelection().size()).toBe(0);
    });

    it('should flash the message Foo', function () {
        var item, rect, text;
        flash.flash('foo');
        jasmine.clock().tick(101);
        setTimeout(function () {
            item = flashItemSelection();
            expect(item.size()).toEqual(1);
            expect(item.classed('flashItem')).toBeTruthy();
            expect(item.select('rect').size()).toEqual(1);
            text = item.select('text');
            expect(text.size()).toEqual(1);
            expect(text.text()).toEqual('foo');
        }, 100);
    });
});
