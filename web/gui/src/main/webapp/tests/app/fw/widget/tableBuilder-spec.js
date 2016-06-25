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
 ONOS GUI -- Widget -- Table Builder Service - Unit Tests
 */

describe('factory: fw/widget/tableBuilder.js', function () {
    var $log, $rootScope, fs, tbs, is;

    var mockObj,
        mockWss = {
            bindHandlers: function () {},
            sendEvent: function () {},
            unbindHandlers: function () {},
            _setLoadingDelegate: function(){},
            isConnected: function() {
                return true;
            }
        };

    beforeEach(module('onosWidget', 'onosUtil', 'onosRemote', 'onosSvg', 'onosLayer'));

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('WebSocketService', mockWss);
        });
    });

    beforeEach(inject(function (_$log_, _$rootScope_,
                                FnService, TableBuilderService, IconService) {
        $log = _$log_;
        $rootScope = _$rootScope_;
        fs = FnService;
        tbs = TableBuilderService;
        is = IconService;
    }));

    function mockSelCb(event, sel) {}

    beforeEach(function () {
        mockObj = {
            scope: $rootScope.$new(),
            tag: 'foo',
            selCb: mockSelCb
        };
    });

    afterEach(function () {
        mockObj = {};
    });

    it('should define TableBuilderService', function () {
        expect(tbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(tbs, [
            'buildTable'
        ])).toBeTruthy();
    });

    it('should verify sortCb', function () {
        spyOn(mockWss, 'sendEvent');
        expect(mockObj.scope.sortCallback).not.toBeDefined();
        tbs.buildTable(mockObj);
        expect(mockObj.scope.sortCallback).toBeDefined();
        expect(mockWss.sendEvent).toHaveBeenCalled();
    });

    it('should set tableData', function () {
        expect(mockObj.scope.tableData).not.toBeDefined();
        tbs.buildTable(mockObj);
        expect(fs.isA(mockObj.scope.tableData)).toBeTruthy();
        expect(mockObj.scope.tableData.length).toBe(0);
    });

    it('should unbind handlers on destroyed scope', function () {
        spyOn(mockWss, 'unbindHandlers');
        tbs.buildTable(mockObj);
        expect(mockWss.unbindHandlers).not.toHaveBeenCalled();
        mockObj.scope.$destroy();
        expect(mockWss.unbindHandlers).toHaveBeenCalled();
    });

});
