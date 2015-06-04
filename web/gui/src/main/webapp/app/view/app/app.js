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

    var selectionObj;

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', 'FnService', 'TableBuilderService', 'WebSocketService',

    function ($log, $scope, fs, tbs, wss) {
        $scope.ctrlBtnState = {};
        // TODO: clean up view
        // all DOM manipulation (adding styles, getting elements and doing stuff
        //     with them) should be done in directives

        function selCb($event, row) {
            $scope.ctrlBtnState.selection = !!$scope.selId;
            selectionObj = row;
            $log.debug('Got a click on:', row);

            if ($scope.ctrlBtnState.selection) {
                $scope.ctrlBtnState.installed = row.state === 'INSTALLED';
                $scope.ctrlBtnState.active = row.state === 'ACTIVE';
            } else {
                $scope.ctrlBtnState.installed = false;
                $scope.ctrlBtnState.active = false;
            }
        }

        tbs.buildTable({
            scope: $scope,
            tag: 'app',
            selCb: selCb
        });

        // TODO: use d3 click events -- move to directive
        d3.select('#app-install').on('click', function () {
            $log.debug('Initiating install');
            var evt = document.createEvent("HTMLEvents");
            evt.initEvent("click", true, true);
            document.getElementById('file').dispatchEvent(evt);
        });

        // TODO: use d3 to select elements -- move to directive
        document.getElementById('app-form-response').onload = function () {
            document.getElementById('app-form').reset();
            $scope.$apply();
            //$scope.sortCallback($scope.sortParams);
        };

        function appAction(action) {
            if ($scope.ctrlBtnState.selection) {
                $log.debug('Initiating ' + action + ' of', selectionObj);
                wss.sendEvent('appManagementRequest', {action: action, name: selectionObj.id});
            }
        }

        // TODO: use d3 to select elements -- move to directive
        d3.select('#file').on('change', function () {
            var file = document.getElementById('file').value.replace('C:\\fakepath\\', '');
            $log.info('Handling file', file);
            var evt = document.createEvent("HTMLEvents");
            evt.initEvent("click", true, true);
            document.getElementById('app-upload').dispatchEvent(evt);
        });

        // TODO: move to directive
        d3.select('#app-uninstall').on('click', function () { appAction('uninstall'); });
        d3.select('#app-activate').on('click', function () { appAction('activate'); });
        d3.select('#app-deactivate').on('click', function () { appAction('deactivate'); });

        $log.log('OvAppCtrl has been created');
    }]);
}());
