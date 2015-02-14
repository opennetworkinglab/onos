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
 ONOS GUI -- Widget -- Table Service - Unit Tests
 */
describe('factory: fw/widget/table.js', function () {
    var $log, $compile, $rootScope,
        fs, is,
        table;

    var onosFixedHeaderTags = '<table ' +
                                'onos-fixed-header ' +
                                'ng-style="setTableHW()">' +
                                '<thead>' +
                                '<tr>' +
                                '<th></th>' +
                                '<th>Device ID </th>' +
                                '<th>H/W Version </th>' +
                                '<th>S/W Version </th>' +
                                '</tr>' +
                                '</thead>' +
                                '<tbody>' +
                                '<tr>' +
                                '<td>' +
                                    '<div icon icon-id="{{dev._iconid_available}}">' +
                                    '</div>' +
                                '</td>' +
                                '<td>Some ID</td>' +
                                '<td>Some HW</td>' +
                                '<td>Some Software</td>' +
                                '</tr>' +
                                '</tbody>' +
                                '</table>',

        onosSortableHeaderTags = '<table class="summary-list" ' +
                                'onos-sortable-header ' +
                                'sort-callback="sortCallback(urlSuffix)">' +
                                '<thead>' +
                                '<tr>' +
                                '<th colId="available"></th>' +
                                '<th colId="id" sortable>Device ID </th>' +
                                '<th colId="hw" sortable>H/W Version </th>' +
                                '<th colId="sw" sortable>S/W Version </th>' +
                                '</tr>' +
                                '</thead>' +
                                '<tbody>' +
                                '<tr>' +
                                '<td>' +
                                    '<div icon icon-id="{{dev._iconid_available}}">' +
                                    '</div>' +
                                '</td>' +
                                '<td>Some ID</td>' +
                                '<td>Some HW</td>' +
                                '<td>Some Software</td>' +
                                '</tr>' +
                                '</tbody>' +
                                '</table>';

    beforeEach(module('onosWidget', 'onosUtil', 'onosSvg'));

    beforeEach(inject(function (_$log_, _$compile_, _$rootScope_,
                                FnService, IconService) {
        $log = _$log_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        fs = FnService;
        is = IconService;
    }));

    beforeEach(function () {
    });

    afterEach(function () {
        table = null;
    });

    it('should affirm that onos-fixed-header is working', function () {
        table = $compile(onosFixedHeaderTags)($rootScope);
        $rootScope.$digest();

        table = d3.select(table);
        expect(table).toBeDefined();

        //expect(table.select('thead').style('display')).toBe('block');
    });

    it('should affirm that onos-sortable-header is working', function () {
        table = $compile(onosSortableHeaderTags)($rootScope);
        $rootScope.$digest();

        table = d3.select(table);
        expect(table).toBeDefined();
    });

    // TODO: write directive unit tests for table.js
});
