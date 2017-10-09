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
    var $log, $scope, $location, tbs, fs, mast, wss, ns, prefs, dps, is, ps;

    var nz = 'nzFilter',
        del = 'showDelta';

    // internal state
    var nzFilter = true,
        showDelta = false,
        detailsPanel,
        pStartY,
        pHeight,
        wSize,
        port;

    // constants
    var topPdg = 28,
        dPanelWidth = 480,

        pName = 'port-details-panel',
        detailsReq = 'portDetailsRequest',
        detailsResp = 'portDetailsResponse';


    var keyBindings = {
        esc: [closePanel, 'Close the details panel'],
        _helpFormat: ['esc'],
    };

    function closePanel() {
        if (detailsPanel.isVisible()) {
            $scope.selId = null;
            detailsPanel.hide();
            return true;
        }
        return false;
    }

    var defaultPortPrefsState = {
        nzFilter: 1,
        showDelta: 0,
    };

    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        prefs.setPrefs('port_prefs', prefsState);
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
        prefsState = prefs.asNumbers(
            prefs.getPrefs('port_prefs', defaultPortPrefsState)
        );

        $log.debug('Port - Prefs State:', prefsState);
        toggleDeltaState(prefsState.showDelta);
        toggleNZState(prefsState.nzFilter);
    }

    function createDetailsPanel() {
        detailsPanel = dps.create(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
            scope: $scope,
            keyBindings: keyBindings,
        });

        dps.setResponse(detailsResp, respDetailsCb);

        $scope.hidePanel = function () { detailsPanel.hide(); };
    }

    function setUpPanel() {
        dps.empty();
        dps.addContainers();
        dps.addCloseButton(closePanel);

        var top = dps.top();

        dps.addHeading('port-icon');
        top.append('div').classed('top-content', true);

        top.append('hr');
    }

    function friendlyPropsList(details) {
        return {
            'ID': details['id'],
            'Device': details['devId'],
            'Type': details['type'],
            'Speed': details['speed'],
            'Enabled': details['enabled'],
        };
    }


    function populateTop(tblDiv, details) {
        is.loadEmbeddedIcon(dps.select('.iconDiv'), details._iconid_type, 40);
        dps.top().select('h2').text(details.devId + ' port ' + details.id);
        dps.addPropsList(tblDiv, friendlyPropsList(details));
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(dps.select('.top-content'), details);
        detailsPanel.height(pHeight);
        detailsPanel.width(dPanelWidth);

    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        port = data.port;
        $scope.$apply();
    }

    angular.module('ovPort', [])
        .controller('OvPortCtrl', [
            '$log', '$scope', '$location',
            'TableBuilderService', 'FnService', 'MastService', 'WebSocketService',
            'NavService', 'PrefsService', 'DetailsPanelService', 'IconService',
            'PanelService',

            function (_$log_, _$scope_, _$location_,
                      _tbs_, _fs_, _mast_, _wss_,
                      _ns_, _prefs_, _dps_, _is_, _ps_) {
                var params;
                var tableApi;
                $log = _$log_;
                $scope = _$scope_;
                $location = _$location_;
                tbs = _tbs_;
                fs = _fs_;
                mast = _mast_;
                wss = _wss_;
                ns = _ns_;
                prefs = _prefs_;
                dps = _dps_;
                is = _is_;
                ps = _ps_;

                params = $location.search();

                $scope.deviceTip = 'Show device table';
                $scope.flowTip = 'Show flow view for this device';
                $scope.groupTip = 'Show group view for this device';
                $scope.meterTip = 'Show meter view for selected device';
                $scope.pipeconfTip = 'Show pipeconf view for selected device';
                $scope.toggleDeltaTip = 'Toggle port delta statistics';
                $scope.toggleNZTip = 'Toggle non zero port statistics';

                if (params.hasOwnProperty('devId')) {
                    $scope.devId = params['devId'];
                }

                $scope.payloadParams = {
                    nzFilter: nzFilter,
                    showDelta: showDelta,
                };

                function selCb($event, row) {
                    if ($scope.selId) {
                        wss.sendEvent(detailsReq, {
                            id: row.id,
                            devId: $scope.devId,
                        });
                    } else {
                        $scope.hidePanel();
                    }
                    $log.debug('Got a click on:', row);
                }

                tableApi = tbs.buildTable({
                    scope: $scope,
                    tag: 'port',
                    query: params,
                    selCb: selCb,
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

                function getOperatorFromQuery(query) {

                    var operator = query.split(' '),
                        opFunc = null;

                    if (operator[0] === '>') {
                        opFunc = _.gt;
                    } else if (operator[0] === '>=') {
                        opFunc = _.gte;
                    } else if (operator[0] === '<') {
                        opFunc = _.lt;
                    } else if (operator[0] === '<=') {
                        opFunc = _.lte;
                    } else {
                        return {
                            operator: opFunc,
                            searchText: query,
                        };
                    }

                    return {
                        operator: opFunc,
                        searchText: operator[1],
                    };
                }

                $scope.customFilter = function (prop, val) {
                    if (!val) {
                        return;
                    }

                    var search = getOperatorFromQuery(val),
                        operator = search.operator,
                        searchText = search.searchText;

                    if (operator) {
                        return function (row) {
                            var queryBy = $scope.queryBy || '$';

                            if (queryBy !== '$') {
                                var rowValue = parseInt(row[$scope.queryBy].replace(/,/g, ''));
                                return operator(rowValue, parseInt(searchText)) ? row : null;
                            } else {
                                var keys = _.keysIn(row);

                                for (var i = 0, l = keys.length; i < l; i++) {
                                    var rowValue = parseInt(row[keys[i]].replace(/,/g, ''));
                                    if (operator(rowValue, parseInt(searchText))) {
                                        return row;
                                    }
                                }
                            }
                        };
                    } else {
                        var out = {};
                        out[$scope.queryBy || '$'] = $scope.query;
                        return out;
                    }
                };

                restoreConfigFromPrefs();

                $scope.$on('$destroy', function () {
                    dps.destroy();
                });

                $log.log('OvPortCtrl has been created');
            }])
    .directive('portDetailsPanel',
        ['$rootScope', '$window', '$timeout', 'KeyService',
            function ($rootScope, $window, $timeout, ks) {
                return function (scope) {
                    var unbindWatch;

                    function heightCalc() {
                        pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                            + mast.mastHeight() + topPdg;
                        wSize = fs.windowSize(pStartY);
                        pHeight = wSize.height;
                    }

                    function initPanel() {
                        heightCalc();
                        createDetailsPanel();
                    }

                    // Safari has a bug where it renders the fixed-layout table wrong
                    // if you ask for the window's size too early
                    if (scope.onos.browser === 'safari') {
                        $timeout(initPanel);
                    } else {
                        initPanel();
                    }
                    // create key bindings to handle panel
                    ks.keyBindings(keyBindings);

                    ks.gestureNotes([
                        ['click', 'Select a row to show port details'],
                        ['scroll down', 'See more ports'],
                    ]);

                    // if the panelData changes
                    scope.$watch('panelData', function () {
                        if (!fs.isEmptyObject(scope.panelData)) {
                            populateDetails(scope.panelData);
                            detailsPanel.show();
                        }
                    });

                    // if the window size changes
                    unbindWatch = $rootScope.$watchCollection(
                        function () {
                            return {
                                h: $window.innerHeight,
                                w: $window.innerWidth,
                            };
                        }, function () {
                            if (!fs.isEmptyObject(scope.panelData)) {
                                heightCalc();
                                populateDetails(scope.panelData);
                            }
                        }
                    );

                    scope.$on('$destroy', function () {
                        unbindWatch();
                        ks.unbindKeys();
                        ps.destroyPanel(pName);
                    });
                };
            }]);
}());
