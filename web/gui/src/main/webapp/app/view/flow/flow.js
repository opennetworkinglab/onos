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
 ONOS GUI -- Flow View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, ns, mast, ps, wss, is, ls;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        topTable,
        topSelTable,
        topTrtTable,
        iconDiv,
        wSize;

    // constants
    var topPdg = 28,
        wtPdg = 400,

        pName = 'flow-details-panel',
        detailsReq = 'flowDetailsRequest',
        detailsResp = 'flowDetailsResponse',

        propOrder = [
            'flowId',
            'state',

            'bytes',
            'packets',
            'duration',

            'priority',
            'tableName',
            'appName',
            'appId',

            'groupId',
            'idleTimeout',
            'hardTimeout',
            'permanent',
        ];

// deferred localization strings
    var warnDeactivate,
        warnOwnRisk,
        friendlyProps,
        lion;

    function doLion() {
        lion = ls.bundle('core.view.Flow');

        warnDeactivate = lion('dlg_warn_deactivate');
        warnOwnRisk = lion('dlg_warn_own_risk');

        friendlyProps = [
            lion('flowId'), lion('state'), lion('bytes'), lion('packets'),
            lion('duration'), lion('priority'),lion('tableName'),lion('appName'),lion('appId'),
            lion('groupId'),lion('idleTimeout'),lion('hardTimeout'),lion('permanent')
        ];
    }


    function closePanel() {
        if (detailsPanel.isVisible()) {
            $scope.selId = null;
            detailsPanel.hide();
            return true;
        }
        return false;
    }

    function addCloseBtn(div) {
        is.loadEmbeddedIcon(div, 'close', 20);
        div.on('click', closePanel);
    }

    function handleEscape() {
        return closePanel();
    }

    function setUpPanel() {
        var container, closeBtn;
        detailsPanel.empty();
        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2');
        topTable = top.append('div').classed('top-content', true)
            .append('table');
        top.append('hr');

        top.append('h3').text('Selector');
        topSelTable = top.append('div').classed('top-content', true).append('table');
        top.append('hr');

        top.append('h3').text('Treatment');
        topTrtTable = top.append('div').classed('top-content', true).append('table');
    }

    function addProp(tbody, label, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', label + ' :');
        addCell('value', value);
    }


    function getLionClearDeferred() {
        // TODO: Localization...
        return 'Clear deferred';
    }

    function populateTop(details) {
        is.loadEmbeddedIcon(iconDiv, 'flowTable', 40);
        top.select('h2').text(details.flowId);

        var tbody = topTable.append('tbody'),
            tbodySel = topSelTable.append('tbody'),
            tbodyTrt = topTrtTable.append('tbody'),
            selArray = details.selector,
            treat = details.treatment,
            propLabels = getLionProps();

        function addLabVal(tbody, lv) {
            var bits = lv.match(/^([^:]*):(.*)/);
            addProp(tbody, bits[1], bits[2]);
        }

        function popInstrList(items, tag) {
            items.forEach(function (item) {
                addLabVal(tbodyTrt, tag + item);
            });
        }

        // basic flow properties
        propOrder.forEach(function (prop, i) {
            addProp(tbody, propLabels[i], details[prop]);
        });

        // selection criteria
        selArray.forEach(function (lv) {
            addLabVal(tbodySel, lv);
        });

        // traffic treatment
        treat.immed && popInstrList(treat.immed, '[imm]');
        treat.defer && popInstrList(treat.defer, '[def]');
        treat.meter && addLabVal(tbodyTrt, treat.meter);
        treat.table && addLabVal(tbodyTrt, treat.table);
        treat.meta && addLabVal(tbodyTrt, treat.meta);
        addProp(tbodyTrt, getLionClearDeferred(), treat.clearDef);
    }

    function createDetailsPane() {
        detailsPanel = ps.createPanel(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
        });
        detailsPanel.el().style({
            position: 'absolute',
            top: pStartY + 'px',
        });
        $scope.hidePanel = function () { detailsPanel.hide(); };
        detailsPanel.hide();
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(details);

        // ToDo add more details
        detailsPanel.height(pHeight);
        detailsPanel.width(wtPdg);
    }

    function respDetailsCb(data) {
        $log.debug('Got response from server :', data);
        $scope.panelData = data.details;
        $scope.$apply();
    }

    angular.module('ovFlow', [])
        .controller('OvFlowCtrl',
            ['$log', '$scope', '$location',
                'FnService', 'TableBuilderService', 'NavService',
                'MastService', 'PanelService', 'KeyService', 'IconService',
                'WebSocketService','LionService',

                function (_$log_, _$scope_, _$location_, _fs_, _tbs_, _ns_,
                          _mast_, _ps_, _ks_, _is_, _wss_, _ls_) {
                    var params,
                        handlers = {};

                    $log = _$log_;
                    $scope = _$scope_;
                    $location = _$location_;
                    fs = _fs_;
                    tbs = _tbs_;
                    ns = _ns_;
                    is = _is_;
                    wss = _wss_;
                    mast = _mast_;
                    ps = _ps_;
                    ls = _ls_;

                    doLion();

                    $scope.lion = lion;

                    $scope.deviceTip = lion('tt_ctl_show_device');
                    $scope.portTip = lion('tt_ctl_show_port');
                    $scope.groupTip = lion('tt_ctl_show_group');
                    $scope.meterTip = lion('tt_ctl_show_meter');
                    $scope.pipeconfTip = lion('tt_ctl_show_pipeconf');
                    $scope.briefTip = lion('tt_ctl_switcth_brief');
                    $scope.detailTip = lion('tt_ctl_switcth_detailed');

                    $scope.brief = true;
                    params = $location.search();
                    if (params.hasOwnProperty('devId')) {
                        $scope.devId = params['devId'];
                    }

                    tbs.buildTable({
                        scope: $scope,
                        tag: 'flow',
                        selCb: selCb,
                        query: params,
                    });

                    $scope.nav = function (path) {
                        if ($scope.devId) {
                            ns.navTo(path, { devId: $scope.devId });
                        }
                    };

                    // details panel handlers
                    handlers[detailsResp] = respDetailsCb;
                    wss.bindHandlers(handlers);

                    function selCb($event, row) {
                        if ($scope.selId) {
                            wss.sendEvent(detailsReq, {
                                flowId: row.id,
                                appId: row.appId
                            });
                        } else {
                            $scope.hidePanel();
                        }
                        $log.debug('Got a click on:', row);
                    }

                    $scope.$on('$destroy', function () {
                        wss.unbindHandlers(handlers);
                    });

                    $scope.briefToggle = function () {
                        $scope.brief = !$scope.brief;
                    };

                    Object.defineProperty($scope, 'queryFilter', {
                        get: function () {
                            var out = {};
                            out[$scope.queryBy || '$'] = $scope.queryTxt;
                            return out;
                        },
                    });

                    $log.log('OvFlowCtrl has been created');
                }])

        .directive('flowDetailsPanel',
            ['$rootScope', '$window', '$timeout', 'KeyService',
            function ($rootScope, $window, $timeout, ks) {
                return function (scope) {
                    var unbindWatch;

                    function heightCalc() {
                        var tabhead = d3.select('.tabular-header');

                        pStartY = fs.noPxStyle(tabhead, 'height') +
                            mast.mastHeight() + topPdg;
                        wSize = fs.windowSize(pStartY);
                        pHeight = wSize.height;
                    }

                    function initPanel() {
                        heightCalc();
                        createDetailsPane();
                    }

                    // Safari has a bug where it renders the fixed-layout
                    // table wrong if you ask for the window's size too early
                    if (scope.onos.browser === 'safari') {
                        $timeout(initPanel);
                    } else {
                        initPanel();
                    }
                    // create key bindings to handle panel
                    ks.keyBindings({
                        esc: [handleEscape, 'Close the details panel'],
                        _helpFormat: ['esc'],
                    });
                    ks.gestureNotes([
                        ['click', 'Select a row to show cluster node details'],
                        ['scroll down', 'See available cluster nodes'],
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
                        ps.destroyPanel(pName);
                    });
                };
            }]);

}());
