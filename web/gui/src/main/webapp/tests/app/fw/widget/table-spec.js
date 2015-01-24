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
describe('factory: fw/widget/table.js', function() {
    var ts, $log, d3Elem;

    var config = {
        colIds: ['id', 'mfr', 'hw', 'sw', 'serial', 'annotations.protocol'],
        colText: ['URI', 'Vendor', 'Hardware Version', 'Software Version',
            'Serial Number', 'Protocol']
        },
        fakeData = {
            "devices": [{
                "id": "of:0000000000000001",
                "available": true,
                "_iconid_available": "deviceOnline",
                "role": "MASTER",
                "mfr": "Nicira, Inc.",
                "hw": "Open vSwitch",
                "sw": "2.0.1",
                "serial": "None",
                "annotations": {
                    "protocol": "OF_10"
                    }
                },
                {
                    "id": "of:0000000000000004",
                    "available": false,
                    "_iconid_available": "deviceOffline",
                    "role": "MASTER",
                    "mfr": "Nicira, Inc.",
                    "hw": "Open vSwitch",
                    "sw": "2.0.1",
                    "serial": "None",
                    "annotations": {
                        "protocol": "OF_10"
                    }
                },
                {
                    "id": "of:0000000000000092",
                    "available": false,
                    "_iconid_available": "deviceOffline",
                    "role": "MASTER",
                    "mfr": "Nicira, Inc.",
                    "hw": "Open vSwitch",
                    "sw": "2.0.1",
                    "serial": "None",
                    "annotations": {
                        "protocol": "OF_10"
                    }
                }]
        };

    beforeEach(module('onosWidget'));

    beforeEach(inject(function (TableService, _$log_) {
        ts = TableService;
        $log = _$log_;
        d3Elem = d3.select('body').append('div').attr('id', 'myDiv');
    }));

    afterEach(function () {
        d3.select('#myDiv').remove();
    });

    it('should define TableService', function () {
        expect(ts).toBeDefined();
    });

    function verifyTableTags(div) {
        var table = div.select('table'),
            tableHeaders;
        expect(table).toBeTruthy();
        expect(table.attr('fixed-header')).toBeFalsy();
        expect(table.select('thead')).toBeTruthy();
        expect(table.select('tbody')).toBeTruthy();

        tableHeaders = table.select('thead').selectAll('th');
        tableHeaders.each(function(thElement, i) {
            thElement = d3.select(this);
            expect(thElement).toBeTruthy();
            expect(thElement.html()).toBe(config.colText[i]);
        });
    }

    function verifyData(div) {
        var tbody = div.select('tbody'),
            tableBoxes = tbody.selectAll('td');
        expect(tbody).toBeTruthy();
        expect(tbody.select('tr')).toBeTruthy();

        tableBoxes.each(function(tdElement, i){
            tdElement = d3.select(this);
            if(i === 0) {
                expect(tdElement.html()).toBe('of:0000000000000001');
            }
            if(i === 1) {
                expect(tdElement.html()).toBe('Nicira, Inc.');
            }
            if(i === 2) {
                expect(tdElement.html()).toBe('Open vSwitch');
            }
            expect(tdElement).toBeTruthy();
        });
    }

    it('should create table tags', function () {
        ts.renderTable(d3Elem, config);
        verifyTableTags(d3Elem);
    });

    it('should load data into table', function () {
        var colIds = ts.renderTable(d3Elem, config);
        ts.loadTableData(fakeData, d3Elem, colIds);
        verifyData(d3Elem);
    });

    it('should render table and load data', function () {
        ts.renderAndLoadTable(d3Elem, config, fakeData);
        verifyData(d3Elem);
    });

});
