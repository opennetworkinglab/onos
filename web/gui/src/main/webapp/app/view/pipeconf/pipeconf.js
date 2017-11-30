/*
 * Copyright 2017-present Open Networking Foundation
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
 ONOS GUI -- Pipeconf View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, $interval, $timeout, fs, ns, wss, ls, ps, mast, is, dps;

    // Constants
    var pipeconfRequest = "pipeconfRequest",
        pipeConfResponse = "pipeConfResponse",
        noPipeconfResp = "noPipeconfResp",
        invalidDevId = "invalidDevId",
        pipeconf = "pipeconf",
        pipelineModel = "pipelineModel",
        devId = "devId",
        topPdg = 28,
        pName = 'pipeconf-detail-panel',
        refreshRate = 5000;

    // For request handling
    var handlers,
        refreshPromise;

    // Details panel
    var pWidth = 600,
        pTopHeight = 111,
        pStartY,
        wSize,
        pHeight,
        detailsPanel;

    // create key bindings to handle panel
    var keyBindings = {
        esc: [closePanel, 'Close the details panel'],
        _helpFormat: ['esc'],
    };

    function fetchPipeconfData() {
        if ($scope.autoRefresh && wss.isConnected() && !ls.waiting()) {
            ls.start();
            var requestData = {
                devId: $scope.devId
            };
            wss.sendEvent(pipeconfRequest, requestData);
        }
    }

    function pipeConfRespCb(data) {
        ls.stop();
        if (!data.hasOwnProperty(pipeconf)) {
            $scope.pipeconf = null;
            return;
        }
        $scope.pipeconf = data[pipeconf];
        $scope.pipelineModel = data[pipelineModel];
        $scope.$apply();
    }

    function noPipeconfRespCb(data) {
        ls.stop();
        $scope.pipeconf = null;
        $scope.pipelineModel = null;
        $scope.$apply();
    }

    function viewDestroy() {
        wss.unbindHandlers(handlers);
        $interval.cancel(refreshPromise);
        refreshPromise = null;
        ls.stop();
    }

    function headerSelectCb($event, header) {
        if ($scope.selectedId !== null &&
            $scope.selectedId.type === 'header' &&
            $scope.selectedId.name === header.name) {

            // Hide the panel when select same row
            closePanel();
            return;
        }

        $scope.selectedId = {
            type: 'header',
            name: header.name,
        };

        var subtitles = [
            {
                label: 'Header Type: ',
                value: header.type.name,
            },
            {
                label: 'Is metadata: ',
                value: header.isMetadata,
            },
            {
                label: 'Index: ',
                value: header.index,
            },
        ];

        var tables = [
            {
                title: 'Fields',
                headers: ['Name', 'Bit width'],
                data: header.type.fields,
                noDataText: 'No header fields'
            },
        ];
        populateDetailPanel(header.name, subtitles, tables);
    }

    function actionSelectCb($event, action) {
        if ($scope.selectedId !== null &&
            $scope.selectedId.type === 'action' &&
            $scope.selectedId.name === action.name) {

            // Hide the panel when select same row
            closePanel();
            return;
        }

        $scope.selectedId = {
            type: 'action',
            name: action.name,
        };

        var subtitles = [];
        var tables = [
            {
                title: 'Parameters',
                headers: ['Name', 'Bit width'],
                data: action.params,
                noDataText: 'No action parameters',
            },
        ];

        populateDetailPanel(action.name, subtitles, tables);
    }

    function tableSelectCb($event, table) {
        if ($scope.selectedId !== null &&
            $scope.selectedId.type === 'table' &&
            $scope.selectedId.name === table.name) {

            // Hide the panel when select same row
            closePanel();
            return;
        }

        $scope.selectedId = {
            type: 'table',
            name: table.name,
        };

        var subtitles = [
            {
                label: 'Max Size: ',
                value: table.maxSize,
            },
            {
                label: 'Has counters: ',
                value: table.hasCounters,
            },
            {
                label: 'Support Aging: ',
                value: table.supportAging,
            },
        ];

        var matchFields = table.matchFields.map(function(mp) {
            return {
                name: mp.field,
                bitWidth: mp.bitWidth,
                matchType: mp.matchType,
            }
        });

        var tables = [
            {
                title: 'Match fields',
                headers: ['Name', 'Bit width', 'Match type'],
                data: matchFields,
                noDataText: 'No match fields'
            },
            {
                title: 'Actions',
                headers: ['Name'],
                data: table.actions,
                noDataText: 'No actions'
            },
        ];

        populateDetailPanel(table.name, subtitles, tables);
    }

    function closePanel() {
        if (detailsPanel.isVisible()) {

            detailsPanel.hide();

            // Avoid Angular inprog error
            $timeout(function() {
                $scope.selectedId = null;
            }, 0);
            return true;
        }
        return false;
    }

    function populateDetailTable(tableContainer, table) {
        var tableTitle = table.title;
        var tableData = table.data;
        var tableHeaders = table.headers;
        var noDataText = table.noDataText;

        tableContainer.append('h2').classed('detail-panel-bottom-title', true).text(tableTitle);

        var detailPanelTable = tableContainer.append('table').classed('detail-panel-table', true);
        var headerTr = detailPanelTable.append('tr').classed('detail-panel-table-header', true);

        tableHeaders.forEach(function(h) {
            headerTr.append('th').text(h);
        });

        if (tableData.length === 0) {
            var row = detailPanelTable.append('tr').classed('detail-panel-table-row', true);
            row.append('td')
                .classed('detail-panel-table-col no-data', true)
                .attr('colspan', tableHeaders.length)
                .text(noDataText);
        }

        tableData.forEach(function(data) {
            var row = detailPanelTable.append('tr').classed('detail-panel-table-row', true);
            if (fs.isS(data)) {
                row.append('td').classed('detail-panel-table-col', true).text(data);
            } else {
                Object.keys(data).forEach(function(k) {
                    row.append('td').classed('detail-panel-table-col', true).text(data[k]);
                });
            }
        });

        tableContainer.append('hr');
    }

    function populateDetailTables(tableContainer, tables) {
        tables.forEach(function(table) {
            populateDetailTable(tableContainer, table);
        })
    }

    function populateDetailPanel(topTitle, topSubtitles, tables) {
        dps.empty();
        dps.addContainers();
        dps.addCloseButton(closePanel);

        var top = dps.top();
        top.append('h2').classed('detail-panel-header', true).text(topTitle);
        topSubtitles.forEach(function(st) {
            var typeText = top.append('div').classed('top-info', true);
            typeText.append('p').classed('label', true).text(st.label);
            typeText.append('p').classed('value', true).text(st.value);
        });

        var bottom = dps.bottom();
        var bottomHeight = pHeight - pTopHeight - 60;
        bottom.style('height', bottomHeight + 'px');
        populateDetailTables(bottom, tables);

        detailsPanel.width(pWidth);
        detailsPanel.show();
        resizeDetailPanel();
    }

    function heightCalc() {
        pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
            + mast.mastHeight() + topPdg;
        wSize = fs.windowSize(pStartY);
        pHeight = wSize.height - 20;
    }

    function resizeDetailPanel() {
        if (detailsPanel.isVisible()) {
            heightCalc();
            var bottomHeight = pHeight - pTopHeight - 60;
            d3.select('.bottom').style('height', bottomHeight + 'px');
            detailsPanel.height(pHeight);
        }
    }

    angular.module('ovPipeconf', [])
        .controller('OvPipeconfCtrl',
            ['$log', '$scope', '$location', '$interval', '$timeout', 'FnService', 'NavService', 'WebSocketService',
                'LoadingService', 'PanelService', 'MastService', 'IconService', 'DetailsPanelService',
                function (_$log_, _$scope_, _$loc_, _$interval_, _$timeout_, _fs_,
                          _ns_, _wss_, _ls_, _ps_, _mast_, _is_, _dps_) {
                    $log = _$log_;
                    $scope = _$scope_;
                    $loc = _$loc_;
                    $interval = _$interval_;
                    $timeout = _$timeout_;
                    fs = _fs_;
                    ns = _ns_;
                    wss = _wss_;
                    ls = _ls_;
                    ps = _ps_;
                    mast = _mast_;
                    is = _is_;
                    dps = _dps_;

                    $scope.deviceTip = 'Show device table';
                    $scope.flowTip = 'Show flow view for this device';
                    $scope.portTip = 'Show port view for this device';
                    $scope.groupTip = 'Show group view for this device';
                    $scope.meterTip = 'Show meter view for selected device';
                    $scope.pipeconfTip = 'Show pipeconf view for selected device';

                    var params = $loc.search();
                    if (params.hasOwnProperty(devId)) {
                        $scope.devId = params[devId];
                    }
                    $scope.nav = function (path) {
                        if ($scope.devId) {
                            ns.navTo(path, { devId: $scope.devId });
                        }
                    };
                    handlers = {
                        pipeConfResponse: pipeConfRespCb,
                        noPipeconfResp: noPipeconfRespCb,
                        invalidDevId: noPipeconfRespCb,
                    };
                    wss.bindHandlers(handlers);
                    $scope.$on('$destroy', viewDestroy);

                    $scope.autoRefresh = true;
                    fetchPipeconfData();

                    // On click callbacks, initialize select id
                    $scope.selectedId = null;
                    $scope.headerSelectCb = headerSelectCb;
                    $scope.actionSelectCb = actionSelectCb;
                    $scope.tableSelectCb = tableSelectCb;

                    // Make them collapsable
                    $scope.collapsePipeconf = false;
                    $scope.collapseHeaders = false;
                    $scope.collapseActions = false;
                    $scope.collapseTables = false;

                    $scope.mapToNames = function(data) {
                        return data.map(function(d) {
                            return d.name;
                        });
                    };

                    $scope.matMatchFields = function(matchFields) {
                        return matchFields.map(function(mf) {
                            return mf.field;
                        });
                    };

                    refreshPromise = $interval(function() {
                        fetchPipeconfData();
                    }, refreshRate);

                    $log.log('OvPipeconfCtrl has been created');
                }])
        .directive('autoHeight', ['$window', 'FnService',
            function($window, fs) {
                return function(scope, element) {
                    var autoHeightElem = d3.select(element[0]);

                    scope.$watchCollection(function() {
                        return {
                            h: $window.innerHeight
                        };
                    }, function() {
                        var wsz = fs.windowSize(140, 0);
                        autoHeightElem.style('height', wsz.height + 'px');
                    });
                };
            }
        ])
        .directive('pipeconfViewDetailPanel', ['$rootScope', '$window', '$timeout', 'KeyService',
            function($rootScope, $window, $timeout, ks) {
                function createDetailsPanel() {
                    detailsPanel = dps.create(pName, {
                        width: wSize.width,
                        margin: 0,
                        hideMargin: 0,
                        scope: $scope,
                        keyBindings: keyBindings,
                        nameChangeRequest: null,
                    });
                    $scope.hidePanel = function () { detailsPanel.hide(); };
                    detailsPanel.hide();
                }

                function initPanel() {
                    heightCalc();
                    createDetailsPanel();
                }

                return function(scope) {
                    var unbindWatch;
                    // Safari has a bug where it renders the fixed-layout table wrong
                    // if you ask for the window's size too early
                    if (scope.onos.browser === 'safari') {
                        $timeout(initPanel);
                    } else {
                        initPanel();
                    }

                    // if the panelData changes
                    scope.$watch('panelData', function () {
                        if (!fs.isEmptyObject(scope.panelData)) {
                            // populateDetails(scope.panelData);
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
                            resizeDetailPanel();
                        }
                    );

                    scope.$on('$destroy', function () {
                        unbindWatch();
                        ks.unbindKeys();
                        ps.destroyPanel(pName);
                    });
                };
            }
        ]);
}());
