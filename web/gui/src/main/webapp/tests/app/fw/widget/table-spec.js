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
 ONOS GUI -- Widget -- Table Service - Unit Tests
 */

describe('factory: fw/widget/table.js', function () {
    var $log, $compile, $rootScope,
        fs, ts, mast, is,
        scope,
        containerDiv,
        headerDiv, bodyDiv,
        header, body,
        mockHeader,
        mockHeaderHeight = 40;

    var onosFixedHeaderTags =
            '<div class="summary-list" onos-fixed-header>' +
            '<div class="table-header">' +
                '<table>' +
                    '<tr>' +
                        '<td colId="type" class="table-icon"></td>' +
                        '<td colId="id">Host ID </td>' +
                        '<td colId="mac" sortable>MAC Address </td>' +
                        '<td colId="location" col-width="110px">Location </td>' +
                    '</tr>' +
                '</table>' +
            '</div>' +

            '<div class="table-body">' +
                '<table>' +
                    '<tr class="ignore-width">' +
                        '<td class="not-picked"></td>' +
                    '</tr>' +
                    '<tr>' +
                        '<td class="table-icon">Some Icon</td>' +
                        '<td>Some ID</td>' +
                        '<td>Some MAC Address</td>' +
                        '<td>Some Location</td>' +
                    '</tr>' +
                '</table>' +
            '</div>' +
            '</div>',

        onosSortableHeaderTags =
            '<div onos-sortable-header ' +
            'sort-callback="sortCallback(requestParams)">' +
                '<table>' +
                    '<tr>' +
                        '<td colId="type"></td>' +
                        '<td colId="id" sortable>Host ID </td>' +
                        '<td colId="mac" sortable>MAC Address </td>' +
                        '<td colId="location" sortable>Location </td>' +
                    '</tr>' +
                '</table>' +
            '</div>';

    beforeEach(module('onosWidget', 'onosUtil', 'onosMast', 'onosSvg', 'onosRemote', 'onosLayer'));

    var mockWindow = {
        innerWidth: 600,
        innerHeight: 400,
        navigator: {
            userAgent: 'defaultUA'
        },
        on: function () {},
        addEventListener: function () {}
    };

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('$window', mockWindow);
        });
    });

    beforeEach(inject(function (_$log_, _$compile_, _$rootScope_,
                                FnService, TableBuilderService, MastService, IconService) {
        $log = _$log_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        fs = FnService;
        ts = TableBuilderService;
        mast = MastService;
        is = IconService;
        scope = $rootScope.$new();
        scope.tableData = [];
    }));

    // Note: dummy header so that d3 doesn't trip up.
    //       $compile has to be used on the directive tag element, so it can't
    //       be included in the tag strings declared above.
    beforeEach(function () {
        mockHeader = d3.select('body')
            .append('h2')
            .classed('tabular-header', true)
            .style({
                height: mockHeaderHeight + 'px',
                margin: 0,
                padding: 0
            })
            .text('Some Header');
    });

    afterEach(function () {
        containerDiv = undefined;
        headerDiv = undefined;
        bodyDiv = undefined;
        header = undefined;
        body = undefined;
        mockHeader.remove();
    });

    function populateTableData() {
        scope.tableData = [
            {
                type: 'endstation',
                id: '1234',
                mac: '00:00:03',
                location: 'USA'
            }
        ];
    }

    it('should define TableBuilderService', function () {
        expect(ts).toBeDefined();
    });

    it('should define api functions', function () {
        expect(fs.areFunctions(ts, [
            'buildTable'
        ])).toBeTruthy();
    });

    function compile(elem) {
        var compiled = $compile(elem);
        compiled(scope);
        scope.$digest();
    }

    function selectTables() {
        expect(containerDiv.find('div').length).toBe(2);

        headerDiv = angular.element(containerDiv[0].querySelector('.table-header'));
        expect(headerDiv.length).toBe(1);

        bodyDiv = angular.element(containerDiv[0].querySelector('.table-body'));
        expect(bodyDiv.length).toBe(1);

        header = headerDiv.find('table');
        expect(header.length).toBe(1);

        body = bodyDiv.find('table');
        expect(body.length).toBe(1);
    }

    function verifyGivenTags(dirName, div) {
        expect(div).toBeDefined();
        expect(div.attr(dirName)).toBe('');
    }

    function verifyDefaultSize() {
        expect(header.css('width')).toBe('570px');
        expect(body.css('width')).toBe('570px');
    }

    function verifyHeight() {
        var padding = 22,
            mastHeight = 36,
            tableHeight = (mockWindow.innerHeight - mockHeaderHeight) -
                (fs.noPx(headerDiv.css('height')) + mastHeight + padding);

        expect(bodyDiv.css('height')).toBe(tableHeight + 'px');
    }

    function verifyColWidth() {
        var hdrs = header.find('td'),
            cols = body.find('td');

        expect(angular.element(hdrs[0]).css('width')).toBe('33px');
        expect(angular.element(hdrs[3]).css('width')).toBe('110px');

        expect(angular.element(cols[1]).css('width')).toBe('33px');
        expect(angular.element(cols[4]).css('width')).toBe('110px');
    }

    function verifyCallbacks(h) {
        expect(scope.sortCallback).not.toHaveBeenCalled();

        h[0].click();
        expect(scope.sortCallback).not.toHaveBeenCalled();

        h[1].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'id',
            sortDir: 'asc'
        });
        h[1].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'id',
            sortDir: 'desc'
        });
        h[1].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'id',
            sortDir: 'asc'
        });

        h[2].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'mac',
            sortDir: 'asc'
        });
        h[2].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'mac',
            sortDir: 'desc'
        });
        h[2].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'mac',
            sortDir: 'asc'
        });

        h[3].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'location',
            sortDir: 'asc'
        });
        h[3].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'location',
            sortDir: 'desc'
        });
        h[3].click();
        expect(scope.sortCallback).toHaveBeenCalledWith({
            sortCol: 'location',
            sortDir: 'asc'
        });
    }

    function verifyIcons(h) {
        var currH, div;

        h[1].click();
        currH = angular.element(h[1]);
        div = currH.find('div');
        expect(div.html()).toBe(
            '<svg class="embeddedIcon" width="10" height="10" viewBox="0 0 ' +
            '50 50"><g class="icon upArrow"><rect width="50" height="50" ' +
            'rx="5"></rect><use width="50" height="50" class="glyph" xmlns:' +
            'xlink="http://www.w3.org/1999/xlink" xlink:href="#triangleUp">' +
            '</use></g></svg>'
        );
        h[1].click();
        div = currH.find('div');
        expect(div.html()).toBe(
            '<svg class="embeddedIcon" width="10" height="10" viewBox="0 0 ' +
            '50 50"><g class="icon downArrow"><rect width="50" height="50" ' +
            'rx="5"></rect><use width="50" height="50" class="glyph" xmlns:' +
            'xlink="http://www.w3.org/1999/xlink" xlink:href="#triangleDown">' +
            '</use></g></svg>'
        );

        h[2].click();
        div = currH.children();
        // clicked on a new element, so the previous icon should have been deleted
        expect(div.html()).toBeFalsy();

        // the new element should have the ascending icon
        currH = angular.element(h[2]);
        div = currH.children();
        expect(div.html()).toBe(
            '<svg class="embeddedIcon" width="10" height="10" viewBox="0 0 ' +
            '50 50"><g class="icon upArrow"><rect width="50" height="50" ' +
            'rx="5"></rect><use width="50" height="50" class="glyph" xmlns:' +
            'xlink="http://www.w3.org/1999/xlink" xlink:href="#triangleUp">' +
            '</use></g></svg>'
        );
    }

    xit('should affirm that onos-fixed-header is working', function () {
        containerDiv = angular.element(onosFixedHeaderTags);

        compile(containerDiv);

        verifyGivenTags('onos-fixed-header', containerDiv);
        selectTables();
        verifyDefaultSize();

        populateTableData();

        scope.$emit('LastElement');
        scope.$digest();

        verifyHeight();
        verifyColWidth();

        mockWindow.innerHeight = 300;
        scope.$digest();
        verifyHeight();

        mockWindow.innerWidth = 500;
        scope.$digest();
        verifyColWidth();
    });

    xit('should affirm that onos-sortable-header is working', function () {
        headerDiv = angular.element(onosSortableHeaderTags);

        compile(headerDiv);
        verifyGivenTags('onos-sortable-header', headerDiv);

        scope.sortCallback = jasmine.createSpy('sortCallback');

        header = headerDiv.find('td');
        verifyCallbacks(header);
        verifyIcons(header);
    });

    // Note: testing resetSortIcons isn't feasible because due to the nature of
    //       directive compilation: they are jQuery elements, not d3 elements,
    //       so the function doesn't work.

});
