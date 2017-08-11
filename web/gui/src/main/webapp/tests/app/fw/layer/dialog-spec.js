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
 ONOS GUI -- Layer -- Dialog Service - Unit Tests
 */

describe('factory: fw/layer/dialog.js', function () {

    var $log, $timeout, fs, ps, d3Elem, dialog;

    beforeEach(module('onosLayer', 'onosNav', 'onosSvg', 'onosRemote'));

    beforeEach(inject(function (_$log_, _$timeout_, FnService, PanelService, DialogService) {
        $log = _$log_;
        $timeout = _$timeout_;
        fs = FnService;
        ps = PanelService;
        dialog = DialogService;

        spyOn(fs, 'debugOn').and.returnValue(true);
        d3Elem = d3.select('body').append('div').attr('id', 'floatpanels');
        ps.init();
    }));

    afterEach(function () {
        d3.select('#floatpanels').remove();
        ps.init();
    });

    it('should define DialogService', function () {
        expect(dialog).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(dialog, [
            'openDialog', 'closeDialog', 'createDiv',
        ])).toBe(true);
    });

    it('should create div', function () {
        var div = dialog.createDiv();
        expect(div).toBeDefined();
    });

    it('should create div with class', function () {
        var div = dialog.createDiv('test-dialog');
        expect(div.classed('test-dialog')).toBe(true);
    });

    it('should openDialog and return API', function () {

        var dialogOptions = {
            edge: 'right',
            width: 400,
            cssCls: 'test-dialog',
        };

        var d = dialog.openDialog('test-dialog', dialogOptions);

        expect(fs.areFunctions(d, [
            'setTitle', 'addContent', 'addButton', 'addOk', 'addOkChained', 'addCancel', 'bindKeys',
        ])).toBe(true);
    });

    it('should append elements, reset and destroy', function () {

        var okCallback = jasmine.createSpy('cb');
        var cancelCallback = jasmine.createSpy('cb');

        var dialogOptions = {
            edge: 'right',
            width: 400,
            cssCls: 'test-dialog',
        };

        var d = dialog.openDialog('test-dialog', dialogOptions);
        // Title
        d.setTitle('Dialog Title');

        // Content
        var content = dialog.createDiv();
        content.append('p').text('Dialog Content');
        d.addContent(content);

        d.addOk(okCallback);
        d.addOk(okCallback, 'Ok');

        d.addCancel(cancelCallback);
        d.addCancel(cancelCallback, 'Cancel');

        // TODO: Test elements have been correctly added
        // Resets dialog
        d = dialog.openDialog('test-dialog', dialogOptions);

        d.addContent('content');
        d.addOkChained(okCallback, 'ok');
        dialog.closeDialog();
    });

});
