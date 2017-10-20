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
 ONOS GUI -- Meter View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, ns, prefs,
        fs, mast, wss, ns, dps, is, ps;

    var detailsPanel,
        pStartY,
        pHeight,
        wSize,
        meter;

    // constants
    var topPdg = 28,
        dPanelWidth = 480,

        pName = 'meter-details-panel',
        detailsReq = 'meterDetailsRequest',
        detailsResp = 'meterDetailsResponse';


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
        $log.debug(details);
        return {
            'ID': details['id'],
            'Device': details['devId'],
            'App Id': details['app_id'],
            'Bytes': details['bytes'],
            'Burst': details['isBurst'],
            'Packets': details['packets'],
            'State': details['state'],
        };
    }


    function populateTop(tblDiv, details) {
        is.loadEmbeddedIcon(dps.select('.iconDiv'), details._iconid_type, 40);
        dps.top().select('h2').text(details.devId + ' Meter ' + details.id);
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
        meter = data.meter;
        $scope.$apply();
    }

    angular.module('ovMeter', [])
    .controller('OvMeterCtrl',
        ['$log', '$scope', '$location', '$sce',
            'FnService', 'TableBuilderService', 'NavService', 'PrefsService',
            'MastService', 'WebSocketService', 'DetailsPanelService', 'IconService',
            'PanelService',

        function (_$log_, _$scope_, _$location_, $sce,
                  _fs_, _tbs_, _ns_, _prefs_,
                  _mast_, _wss_, _dps_, _is_, _ps_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            tbs = _tbs_;
            ns = _ns_;
            fs = _fs_;
            mast = _mast_;
            wss = _wss_;
            prefs = _prefs_;
            dps = _dps_;
            is = _is_;

            $scope.deviceTip = 'Show device table';
            $scope.flowTip = 'Show flow view for this device';
            $scope.portTip = 'Show port view for this device';
            $scope.groupTip = 'Show group view for this device';
            $scope.pipeconfTip = 'Show pipeconf view for selected device';

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

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

            tbs.buildTable({
                scope: $scope,
                tag: 'meter',
                query: params,
                selCb: selCb,
            });

            $scope.$watch('tableData', function () {
                if (!fs.isEmptyObject($scope.tableData)) {
                    $scope.tableData.forEach(function (meter) {
                        meter.bands = $sce.trustAsHtml(meter.bands);
                    });
                }
            });

            $scope.nav = function (path) {
                if ($scope.devId) {
                    ns.navTo(path, { devId: $scope.devId });
                }
            };

            Object.defineProperty($scope, 'queryFilter', {
                get: function () {
                    var out = {};
                    out[$scope.queryBy || '$'] = $scope.query;
                    return out;
                },
            });

            $scope.$on('$destroy', function () {
                dps.destroy();
            });

            $log.log('OvMeterCtrl has been created');
        }])
        .directive('meterDetailsPanel',
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
