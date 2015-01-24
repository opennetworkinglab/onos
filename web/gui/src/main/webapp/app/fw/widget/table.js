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

    function renderTable(div, config) {
        var table = div.append('table').attr('fixed-header', ''),
            thead, tr, numTableCols, i;
        table.append('thead');
        table.append('tbody');

        thead = table.select('thead');
        tr = thead.append('tr');
        numTableCols = config.colIds.length;

        for(i = 0; i < numTableCols; i += 1) {
            tr.append('th').html(config.colText[i]);
        }

        return config.colIds;
    }

    // I can delete these comments later...
    // loadTableData needs to know which IDs are used to create the table...
    // Potentially, there will be some rows in the JSON that the server is
    // sending back that will be unused. We don't want to create unneeded rows.
        // For example, in device view, we aren't displaying "role" or
        // "available" properties, but they would be displayed if we took it
        // directly from the data being sent in.
    function loadTableData(data, div, colIds) {
        // let me know if you have suggestions for this function

        var numTableCols = colIds.length,
            tbody, tr, i;
        tbody = div.select('tbody');

        // get the array associated with the first object, such as "devices"
        // loop through every object in the array, and every colId in config
        // put the appropriate property in the td of the table
        (data[Object.keys(data)[0]]).forEach(function (item) {
            tr = tbody.append('tr');
            for(i = 0; i < numTableCols; i += 1) {
                if(item.hasOwnProperty(colIds[i])) {
                   tr.append('td').html(item[colIds[i]]);
                }
            }
        });
    }

    function renderAndLoadTable(div, config, data) {
        loadTableData(data, div, (renderTable(div, config)));
    }

    angular.module('onosWidget')
        .factory('TableService', ['$log', function (_$log_) {
            $log = _$log_;

            return {
                renderTable: renderTable,
                loadTableData: loadTableData,
                renderAndLoadTable: renderAndLoadTable
            };
        }]);

}());
