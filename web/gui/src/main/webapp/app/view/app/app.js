/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- App View Module
 */

(function () {
    'use strict';

    var selRow, selection;

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', 'TableBuilderService', 'WebSocketService',

    function ($log, $scope, tbs, wss) {
        function selCb($event, row) {
            selRow = angular.element($event.currentTarget);
            selection = row;
            $log.debug('Got a click on:', row);
            // adjust which toolbar buttons are selected
            d3.select('#app-activate').classed('active', row && row.state === 'INSTALLED');
            d3.select('#app-deactivate').classed('active', row && row.state === 'ACTIVE');
            d3.select('#app-uninstall').classed('active', row);
        }

        d3.select('#app-install').on('click', function () {
            $log.debug('Initiating install');
            var evt = document.createEvent("HTMLEvents");
            evt.initEvent("click", true, true);
            document.getElementById('file').dispatchEvent(evt);
        });

        document.getElementById('app-form-response').onload = function () {
            document.getElementById('app-form').reset();
            $scope.refresh();
        };

        function appAction(action) {
            if (selection) {
                $log.debug('Initiating uninstall of', selection);
                wss.sendEvent('appManagementRequest', {action: action, name: selection.id});
            }
        }

        d3.select('#file').on('change', function () {
            var file = document.getElementById('file').value.replace('C:\\fakepath\\', '');
            $log.info('Handling file', file);
            var evt = document.createEvent("HTMLEvents");
            evt.initEvent("click", true, true);
            document.getElementById('app-upload').dispatchEvent(evt);
        });

        d3.select('#app-uninstall').on('click', function () { appAction('uninstall'); });
        d3.select('#app-activate').on('click', function () { appAction('activate'); });
        d3.select('#app-deactivate').on('click', function () { appAction('deactivate'); });

        tbs.buildTable({
            scope: $scope,
            tag: 'app',
            selCb: selCb
        });

        $log.log('OvAppCtrl has been created');
    }]);
}());
