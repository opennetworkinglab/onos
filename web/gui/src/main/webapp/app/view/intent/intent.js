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
 ONOS GUI -- Intent View Module
 */

(function () {
    'use strict';

    var dialogId = 'remove-intent-dialog',
        dialogOpts = {
            edge: 'right'
        };

    angular.module('ovIntent', [])
        .controller('OvIntentCtrl',
        ['$log', '$scope', 'TableBuilderService', 'NavService',
            'TopoTrafficService', 'DialogService',

        function ($log, $scope, tbs, ns, tts, ds) {
            $scope.briefTip = 'Switch to brief view';
            $scope.detailTip = 'Switch to detailed view';
            $scope.brief = true;

            function selCb($event, row) {
                $log.debug('Got a click on:', row);
                var m = /(\d+)\s:\s(.*)/.exec(row.appId),
                    id = m ? m[1] : null,
                    name = m ? m[2] : null;

                $scope.intentData = ($scope.selId && m) ? {
                    appId: id,
                    appName: name,
                    key: row.key
                } : null;
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'intent',
                selCb: selCb,
                idKey: 'key'
            });

            $scope.topoTip = 'Show selected intent on topology view';
            $scope.deactivateTip = 'Remove selected intent';

            $scope.showIntent = function () {
                var d = $scope.intentData;
                d && ns.navTo('topo', d);
            };

            $scope.deactivateIntent = function () {
                var content = ds.createDiv();

                content.append('p')
                    .text('Are you sure you want to remove the selected intent?');

                function dOk() {
                    var d = $scope.intentData;
                    d && tts.removeIntent(d);
                }

                function dCancel() {
                    ds.closeDialog();
                    $log.debug('Canceling remove-intent action');
                }

                ds.openDialog(dialogId, dialogOpts)
                    .setTitle('Confirm Action')
                    .addContent(content)
                    .addOk(dOk)
                    .addCancel(dCancel)
                    .bindKeys();
            };

            $scope.briefToggle = function () {
                $scope.brief = !$scope.brief;
            };

            $scope.$on('$destroy', function () {
                ds.closeDialog();
                $log.debug('OvIntentCtrl has been destroyed');
            });

            $log.debug('OvIntentCtrl has been created');
        }]);
}());
