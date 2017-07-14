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
 ONOS GUI -- Cluster View Module
 */

(function () {
    'use strict';

    // injected references
    var $log, $scope, fs, mast, ps, is, wss;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        topTable,
        bottom,
        iconDiv,
        wSize;


    // constants
    var topPdg = 28,
        ctnrPdg = 24,
        scrollSize = 17,
        portsTblPdg = 50,
        pName = 'cluster-details-panel',
        detailsReq = 'clusterDetailsRequest',
        detailsResp = 'clusterDetailsResponse',
        propOrder = [
            'id', 'ip',
        ],
        deviceCols = [
            'id', 'type', 'chassisid', 'mfr',
            'hw', 'sw', 'protocol', 'serial',
        ];

    // deferred fetching of user-visible strings, so that lion context is set
    function getLionProps() {
        var l = $scope.lion;
        return [
            l('node_id'),
            l('ip_address'),
        ];
    }

    function getLionDeviceCols() {
        var l = $scope.lion;
        return [
            l('uri'),
            l('type'),
            l('chassis_id'),
            l('vendor'),
            l('hw_version'),
            l('sw_version'),
            l('protocol'),
            l('serial_number'),
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

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h2').classed('devices-title', true).text('Devices');
        bottom.append('table');
        // TODO: add more details
    }

    function addProp(tbody, label, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', label + ' :');
        addCell('value', value);
    }

    function populateTop(details) {
        var propLabels = getLionProps();

        is.loadEmbeddedIcon(iconDiv, 'node', 40);
        top.select('h2').text(details.id);

        var tbody = topTable.append('tbody');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, propLabels[i], details[prop]);
        });
    }

    function addDeviceRow(tbody, device) {
        var tr = tbody.append('tr');

        deviceCols.forEach(function (col) {
            tr.append('td').text(device[col]);
        });
    }

    function populateBottom(devices) {
        var table = bottom.select('table'),
            theader = table.append('thead').append('tr'),
            tbody = table.append('tbody'),
            tbWidth, tbHeight;

        getLionDeviceCols().forEach(function (col) {
            theader.append('th').text(col);
        });
        devices.forEach(function (device) {
            addDeviceRow(tbody, device);
        });

        tbWidth = fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = pHeight
                    - (fs.noPxStyle(detailsPanel.el()
                                        .select('.top'), 'height')
                    + fs.noPxStyle(detailsPanel.el()
                                        .select('.devices-title'), 'height')
                    + portsTblPdg);

        table.style({
            height: tbHeight + 'px',
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block',
        });

        detailsPanel.width(tbWidth + ctnrPdg);
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
        populateBottom(details.devices);
        detailsPanel.height(pHeight);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.$apply();
    }


    angular.module('ovCluster', [])
        .controller('OvClusterCtrl',
        ['$log', '$scope', 'FnService', 'MastService',
        'PanelService', 'IconService', 'WebSocketService',
        'LionService', 'TableBuilderService',

    function (_$log_, _$scope_, _fs_, _mast_, _ps_, _is_, _wss_, lion, tbs) {
            var handlers = {};

            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            is = _is_;
            wss = _wss_;

            $scope.lion = lion.bundle('core.view.Cluster');
            $scope.panelData = {};

            tbs.buildTable({
                scope: $scope,
                selCb: selCb,
                tag: 'cluster',
            });

        // details panel handlers
        handlers[detailsResp] = respDetailsCb;
        wss.bindHandlers(handlers);

        function selCb($event, row) {
            if ($scope.selId) {
                wss.sendEvent(detailsReq, { id: row.id });
            } else {
                $scope.hidePanel();
            }
            $log.debug('Got a click on:', row);
        }

        $scope.$on('$destroy', function () {
            wss.unbindHandlers(handlers);
        });

        $log.log('OvClusterCtrl has been created');
    }])

    .directive('clusterDetailsPanel',
    ['$rootScope', '$window', '$timeout', 'KeyService',
    function ($rootScope, $window, $timeout, ks) {
        return function (scope) {
            var unbindWatch,
                lion = scope.lion;

            function heightCalc() {
                pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                                        + mast.mastHeight() + topPdg;
                wSize = fs.windowSize(pStartY);
                pHeight = wSize.height;
            }

            function initPanel() {
                heightCalc();
                createDetailsPane();
            }

            // Safari has a bug where it renders the fixed-layout table wrong
            // if you ask for the window's size too early
            if (scope.onos.browser === 'safari') {
                $timeout(initPanel);
            } else {
                initPanel();
            }
            // create key bindings to handle panel
            ks.keyBindings({
                esc: [handleEscape, lion('qh_hint_close_detail')],
                _helpFormat: ['esc'],
            });
            ks.gestureNotes([
                [lion('click'), lion('qh_hint_click')],
                [lion('scroll_down'), lion('qh_hint_scroll_down')],
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
