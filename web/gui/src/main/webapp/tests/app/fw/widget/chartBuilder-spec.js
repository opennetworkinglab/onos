/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- Widget -- Chart Builder Service - Unit Tests
 */

describe('factory: fw/widget/chartBuilder.js', function () {
    var $log, $rootScope, fs, cbs, is;

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
                                FnService, ChartBuilderService, IconService) {
        $log = _$log_;
        $rootScope = _$rootScope_;
        fs = FnService;
        cbs = ChartBuilderService;
        is = IconService;
    }));

    beforeEach(function () {
        mockObj = {
            scope: $rootScope.$new(),
            tag: 'foo'
        };
    });

    afterEach(function () {
        mockObj = {};
    });

    it('should define ChartBuilderService', function () {
        expect(cbs).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(cbs, [
            'buildChart'
        ])).toBeTruthy();
    });

    it('should verify requestCb', function () {
        spyOn(mockWss, 'sendEvent');
        expect(mockObj.scope.requestCallback).not.toBeDefined();
        cbs.buildChart(mockObj);
        expect(mockObj.scope.requestCallback).toBeDefined();
        mockObj.scope.requestCallback();
        expect(mockWss.sendEvent).toHaveBeenCalled();
    });

    it('should set chartData', function () {
        expect(mockObj.scope.chartData).not.toBeDefined();
        cbs.buildChart(mockObj);
        expect(fs.isA(mockObj.scope.chartData)).toBeTruthy();
        expect(mockObj.scope.chartData.length).toBe(0);
    });

    it('should unbind handlers on destroyed scope', function () {
        spyOn(mockWss, 'unbindHandlers');
        cbs.buildChart(mockObj);
        expect(mockWss.unbindHandlers).not.toHaveBeenCalled();
        mockObj.scope.$destroy();
        expect(mockWss.unbindHandlers).toHaveBeenCalled();
    });
});
