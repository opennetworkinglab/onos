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
 ONOS GUI -- Widget -- Tooltip Service - Unit Tests
 */
describe('factory: fw/widget/tooltip.js', function () {
    var $log, fs, tts, d3Elem;

    beforeEach(module('onosWidget', 'onosUtil'));

    beforeEach(inject(function (_$log_, FnService, TooltipService) {
        $log = _$log_;
        fs = FnService;
        tts = TooltipService;
    }));

    beforeEach(function () {
        d3Elem = d3.select('body').append('div').attr('id', 'tooltip');
    });

    afterEach(function () {
        d3.select('#tooltip').remove();
    });

    it('should define TooltipService', function () {
        expect(tts).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tts, [
            'addTooltip', 'showTooltip', 'cancelTooltip'
        ])).toBeTruthy();
    });

    it('should not accept undefined arguments', function () {
        var btn = d3.select('body').append('div');
        expect(tts.showTooltip()).toBeFalsy();
        expect(tts.showTooltip(btn)).toBeFalsy();

        expect(tts.cancelTooltip()).toBeFalsy();
    });

    // TODO: figure out how to test this
    // testing mouse events is tough
    // showTooltip needs a d3 event, which currently has no test backend
    // .each is a workaround, which provides this, d, and i
    xit('should show a tooltip', function () {
        var btn = d3.select('body').append('div').attr('id', 'foo');
        btn.each(function () {
            tts.showTooltip(this, 'yay a tooltip');
        });
        // tests here
    });

    // can't cancel a tooltip until we show one
    // because currElemId isn't set otherwise
    xit('should cancel an existing tooltip', function () {
        var btn = d3.select('body').append('div').attr('id', 'foo');
        btn.each(function () {
            tts.cancelTooltip(this);
        });
        expect(d3Elem.text()).toBe('');
        expect(d3Elem.style('display')).toBe('none');
    });
});
