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
 ONOS GUI -- Device View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, $loc, fs, mast, ps, wss, is, ns, ks, dps;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        wSize,
        device;

    // constants
    var topPdg = 28,
        ctnrPdg = 24,
        scrollSize = 17,
        portsTblPdg = 50,

        pName = 'device-details-panel',
        detailsReq = 'deviceDetailsRequest',
        detailsResp = 'deviceDetailsResponse',
        nameChangeReq = 'deviceNameChangeRequest',
        nameChangeResp = 'deviceNameChangeResponse',
        portCols = [
            'enabled', 'id', 'speed', 'type', 'elinks_dest', 'name',
        ],
        friendlyPortCols = [
            'Enabled', 'ID', 'Speed', 'Type', 'Egress Links', 'Name',
        ];

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

    function setUpPanel() {
        // var container, closeBtn, tblDiv;

        dps.empty();
        dps.addContainers();
        dps.addCloseButton(closePanel);

        var top = dps.top();
        var bottom = dps.bottom();

        dps.addHeading('dev-icon', true);
        top.append('div').classed('top-content', true);

        top.append('hr');

        bottom.append('h2').classed('ports-title', true).text('Ports');
        bottom.append('table');
    }

    function friendlyPropsList(details) {
        return {
            'URI': device.id,
            'Type': device.type,
            'Master ID': details['masterid'],
            'Chassis ID': details['chassisid'],
            'Vendor': device.mfr,
            'H/W Version': device.hw,
            'S/W Version': device.sw,
            'Protocol': details['protocol'],
            'Serial #': device.serial,
            'Pipeconf': details['pipeconf'],
        };
    }

    function populateTop(tblDiv, details) {
        is.loadEmbeddedIcon(dps.select('.iconDiv'), details._iconid_type, 40);
        dps.top().select('h2').text(details.name);
        dps.addPropsList(tblDiv, friendlyPropsList(details));
    }

    function addPortRow(tbody, port) {
        var tr = tbody.append('tr');

        portCols.forEach(function (col) {
            tr.append('td').text(port[col]);
        });
    }

    function populateBottom(table, ports) {

        var theader = table.append('thead').append('tr'),
            tbody = table.append('tbody'),
            tbWidth, tbHeight;

        friendlyPortCols.forEach(function (col) {
            theader.append('th').text(col);
        });

        ports.forEach(function (port) {
            addPortRow(tbody, port);
        });

        tbWidth = fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = pHeight
                    - (fs.noPxStyle(detailsPanel.el().select('.top'), 'height')
                    + fs.noPxStyle(detailsPanel.el().select('.ports-title'), 'height')
                    + portsTblPdg);

        table.style({
            height: tbHeight + 'px',
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block',
        });

        detailsPanel.width(tbWidth + ctnrPdg);
    }

    function populateDetails(details) {
        var btmTbl, ports;

        setUpPanel();

        btmTbl = dps.bottom().select('table');
        ports = details.ports;

        populateTop(dps.select('.top-content'), details);
        populateBottom(btmTbl, ports);

        detailsPanel.height(pHeight);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        device = data.device;
        $scope.$apply();
    }

    function respNameCb(data) {
        if (data.warn) {
            $log.warn(data.warn, data.id);
            top.select('h2').text(data.id);
        }
    }

    function createDetailsPanel() {
        detailsPanel = dps.create(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0,
            scope: $scope,
            keyBindings: keyBindings,
            nameChangeRequest: nameChangeReq,
        });

        dps.setResponse(detailsResp, respDetailsCb);

        $scope.hidePanel = function () { detailsPanel.hide(); };
    }

    // Sample functions for detail panel creation
    function popTop(div) {
        $log.debug('populateTop');
        // TODO: real work
        // div.append(.....);
        // div.append(.....);
        // div.append(.....);
    }

    function popMid(div) {
        $log.debug('populateMiddle');
        // TODO: real work
    }

    angular.module('ovDevice', [])
    .controller('OvDeviceCtrl',
        ['$log', '$scope', '$location', 'TableBuilderService',
            'TableDetailService', 'FnService',
            'MastService', 'PanelService', 'WebSocketService', 'IconService',
            'NavService', 'KeyService', 'DetailsPanelService',

        function (_$log_, _$scope_, _$location_,
                  tbs, tds, _fs_, _mast_, _ps_, _wss_, _is_, _ns_, _ks_, _dps_) {
            var params,
                handlers = {};

            $log = _$log_;
            $scope = _$scope_;
            $loc = _$location_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            wss = _wss_;
            is = _is_;
            ns = _ns_;
            ks = _ks_;
            dps = _dps_;

            params = $loc.search();

            $scope.panelData = {};
            $scope.flowTip = 'Show flow view for selected device';
            $scope.portTip = 'Show port view for selected device';
            $scope.groupTip = 'Show group view for selected device';
            $scope.meterTip = 'Show meter view for selected device';
            $scope.pipeconfTip = 'Show pipeconf view for selected device';

            // details panel handlers
            handlers[nameChangeResp] = respNameCb;
            wss.bindHandlers(handlers);

            // query for if a certain device needs to be highlighted
            if (params.hasOwnProperty('devId')) {
                $scope.selId = params['devId'];
                wss.sendEvent(detailsReq, { id: $scope.selId });
            }

            function selCb($event, row) {
                if ($scope.selId) {
                    wss.sendEvent(detailsReq, { id: row.id });
                } else {
                    $scope.hidePanel();
                }
                $log.debug('Got a click on:', row);
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'device',
                selCb: selCb,
            });


            // ==================== for testing for now ===============
            // TODO: more than just an example
            tds.buildBasePanel({
                popTop: popTop,
                popMid: popMid,
            });
            // ==================== for testing for now ===============


            $scope.nav = function (path) {
                if ($scope.selId) {
                    ns.navTo(path, { devId: $scope.selId });
                }
            };

            $scope.$on('$destroy', function () {
                dps.destroy();
                wss.unbindHandlers(handlers);
            });

            $log.log('OvDeviceCtrl has been created');
        }])

    .directive('deviceDetailsPanel',
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
                ['click', 'Select a row to show device details'],
                ['scroll down', 'See more devices'],
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
