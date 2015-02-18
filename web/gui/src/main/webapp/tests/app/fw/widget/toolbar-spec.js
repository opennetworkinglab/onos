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
 ONOS GUI -- Widget -- Toolbar Service - Unit Tests
 */
describe('factory: fw/widget/toolbar.js', function () {
    var $log, fs, tbs;

    beforeEach(module('onosWidget', 'onosUtil'));

    beforeEach(inject(function (_$log_, FnService, ToolbarService) {
        $log = _$log_;
        fs = FnService;
        tbs = ToolbarService;
    }));

    it('should define ToolbarService', function () {
        expect(tbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tbs, [
            'makeButton', 'makeToggle', 'makeRadio', 'separator', 'createToolbar'
        ])).toBeTruthy();
    });

    it("should verify makeButton's returned object", function () {
        var button = tbs.makeButton('foo', 'glyph-bar', function () {});

        expect(button.t).toBe('btn');
        expect(button.id).toBe('foo');
        expect(button.gid).toBe('glyph-bar');
        expect(fs.isF(button.cb)).toBeTruthy();
    });

    it("should verify makeToggle's returned object", function () {
        var toggle = tbs.makeToggle('foo', 'glyph-bar', function () {});

        expect(toggle.t).toBe('tog');
        expect(toggle.id).toBe('foo');
        expect(toggle.gid).toBe('glyph-bar');
        expect(fs.isF(toggle.cb)).toBeTruthy();
    });

    it("should verify makeRadio's returned object", function () {
        // TODO: finish this

        //var rFoo1 = tbs.makeRadio('foo', function () {});
        //var rFoo2 = tbs.makeRadio('foo', function () {});
        //var rFoo3 = tbs.makeRadio('foo', function () {});
        //var rBar1 = tbs.makeRadio('bar', function () {});
        //
        //expect(radio1.t).toBe('rad');
        //expect(radio1.id).toBe('foo');
        //expect(fs.isF(radio1.cb)).toBeTruthy();
    });

    it("should verify separator's returned object", function () {
        var separator = tbs.separator();
        expect(separator.t).toBe('sep');
    });

});
