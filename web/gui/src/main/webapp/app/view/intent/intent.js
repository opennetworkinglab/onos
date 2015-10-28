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
 ONOS GUI -- Intent View Module
 */

(function () {
    'use strict';

    angular.module('ovIntent', [])
        .controller('OvIntentCtrl',
        ['$log', '$scope', 'TableBuilderService', 'NavService',

        function ($log, $scope, tbs, ns) {

            function selCb($event, row) {
                $log.debug('Got a click on:', row);
                var m = /(\d+)\s:\s(.*)/.exec(row.appId),
                    id = m ? m[1] : null,
                    name = m ? m[2] : null;

                $scope.intentData = ($scope.selId && m) ? {
                    intentAppId: id,
                    intentAppName: name,
                    intentKey: row.key
                } : null;
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'intent',
                selCb: selCb,
                idKey: 'key'
            });

            $scope.topoTip = 'Show selected intent on topology view';

            $scope.showIntent = function () {
                var d = $scope.intentData;
                d && ns.navTo('topo', d);
            };

            $log.log('OvIntentCtrl has been created');
        }]);
}());
