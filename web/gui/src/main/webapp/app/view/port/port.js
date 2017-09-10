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
 ONOS GUI -- Port View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, tbs, ns, ps;

    var nz = 'nzFilter',
        del = 'showDelta';

    // internal state
    var nzFilter = true,
        showDelta = false;

    var defaultPortPrefsState = {
        nzFilter: 1,
        showDelta: 0,
    };

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('port_prefs', prefsState);
    }

    function toggleNZState(b) {
        if (b === undefined) {
            nzFilter = !nzFilter;
        } else {
            nzFilter = b;
        }
        updatePrefsState(nz, nzFilter);
    }

    function toggleDeltaState(b) {
        if (b === undefined) {
            showDelta = !showDelta;
        } else {
            showDelta = b;
        }
        updatePrefsState(del, b);
    }

    function restoreConfigFromPrefs() {
        prefsState = ps.asNumbers(
            ps.getPrefs('port_prefs', defaultPortPrefsState)
        );

        $log.debug('Port - Prefs State:', prefsState);
        toggleDeltaState(prefsState.showDelta);
        toggleNZState(prefsState.nzFilter);
    }

    angular.module('ovPort', [])
        .controller('OvPortCtrl', [
            '$log', '$scope', '$location',
            'TableBuilderService', 'NavService', 'PrefsService',

            function (_$log_, _$scope_, _$location_, _tbs_, _ns_, _ps_) {
                var params;
                var tableApi;
                $log = _$log_;
                $scope = _$scope_;
                $location = _$location_;
                tbs = _tbs_;
                ns = _ns_;
                ps = _ps_;

                $scope.deviceTip = 'Show device table';
                $scope.flowTip = 'Show flow view for this device';
                $scope.groupTip = 'Show group view for this device';
                $scope.meterTip = 'Show meter view for selected device';
                $scope.pipeconfTip = 'Show pipeconf view for selected device';
                $scope.toggleDeltaTip = 'Toggle port delta statistics';
                $scope.toggleNZTip = 'Toggle non zero port statistics';

                params = $location.search();
                if (params.hasOwnProperty('devId')) {
                    $scope.devId = params['devId'];
                }

                $scope.payloadParams = {
                    nzFilter: nzFilter,
                    showDelta: showDelta,
                };

                tableApi = tbs.buildTable({
                    scope: $scope,
                    tag: 'port',
                    query: params,
                });

                function filterToggleState() {
                    return {
                        nzFilter: nzFilter,
                        showDelta: showDelta,
                    };
                }

                $scope.nav = function (path) {
                    if ($scope.devId) {
                        ns.navTo(path, { devId: $scope.devId });
                    }
                };

                $scope.toggleNZ = function () {
                    toggleNZState();
                    $scope.payloadParams = filterToggleState();
                    tableApi.forceRefesh();
                };

                $scope.toggleDelta = function () {
                    toggleDeltaState();
                    $scope.payloadParams = filterToggleState();
                    tableApi.forceRefesh();
                };

                $scope.isDelta = function () {
                    return showDelta;
                };

                $scope.isNZ = function () {
                    return nzFilter;
                };

                Object.defineProperty($scope, 'queryFilter', {
                    get: function () {
                        var out = {};
                        out[$scope.queryBy || '$'] = $scope.query;
                        return out;
                    },
                });

                restoreConfigFromPrefs();
                $log.log('OvPortCtrl has been created');
            }]);
}());
