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
 ONOS GUI -- Widget -- Table Service
 */
(function () {
    'use strict';

    var $log;

    function renderTable(div, config, data) {
        var table = div.append('table').attr('fixed-header', ''),
            colIds = config.colIds,
            colText = config.colText,
            dataObjects = data[Object.keys(data)[0]],
            thead, tbody, tRows;

        thead = table.append('thead');
        tbody = table.append('tbody');

        thead.append('tr').selectAll('th')
            .data(colText)
            .enter()
            .append('th')
            .text(function(d) { return d });

        tRows = tbody.selectAll('tr')
            .data(dataObjects)
            .enter()
            .append('tr');

        tRows.selectAll('td')
            .data(function(row) {
                return colIds.map(function(headerId) {
                    return {
                        column: headerId, value: row[headerId]
                    };
                });
            })
            .enter()
            .append('td')
            .html(function(d) { return d.value });

        return table;
    }

    angular.module('onosWidget')
        .factory('TableService', ['$log', function (_$log_) {
            $log = _$log_;

            return {
                renderTable: renderTable
            };
        }]);

}());
