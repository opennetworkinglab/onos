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
            'showTooltip', 'cancelTooltip'
        ])).toBeTruthy();
    });

    it('should not accept undefined arguments', function () {
        var btn = d3.select('body').append('div');
        expect(tts.showTooltip()).toBeFalsy();
        expect(tts.showTooltip(btn)).toBeFalsy();

        expect(tts.cancelTooltip()).toBeFalsy();
    });

    // testing mouse events is tough

    xit('should show a tooltip', function () {
        var btn = d3.select('body').append('div').attr('id', 'foo');
        // each is used to trigger a "mouse" event, providing this, d, and i
        btn.each(function () {
            tts.showTooltip(this, 'yay a tooltip');
        });
    });
});
