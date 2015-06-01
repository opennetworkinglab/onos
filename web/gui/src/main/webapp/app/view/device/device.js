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
 ONOS GUI -- Device View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, mast, ps, wss, is, bns, ns, ttip;

    // internal state
    var detailsPanel,
        pStartY, pHeight,
        top, bottom, iconDiv,
        wSize, selRow;

    // constants
    var topPdg = 13,
        ctnrPdg = 24,
        scrollSize = 17,
        portsTblPdg = 50,
        flowPath = 'flow',
        portPath = 'port',
        groupPath = 'group',

        pName = 'device-details-panel',
        bName = 'dev-dets-p',
        detailsReq = 'deviceDetailsRequest',
        detailsResp = 'deviceDetailsResponse',

        propOrder = [
            'type', 'masterid', 'chassisid',
            'mfr', 'hw', 'sw', 'protocol', 'serial'
        ],
        friendlyProps = [
            'Type', 'Master ID', 'Chassis ID',
            'Vendor', 'H/W Version', 'S/W Version', 'Protocol', 'Serial #'
        ],
        portCols = [
            'enabled', 'id', 'speed', 'type', 'elinks_dest', 'name'
        ],
        friendlyPortCols = [
            'Enabled', 'ID', 'Speed', 'Type', 'Egress Links', 'Name'
        ];

    function addCloseBtn(div) {
        is.loadEmbeddedIcon(div, 'plus', 30);
        div.select('g').attr('transform', 'translate(25, 0) rotate(45)');

        div.on('click', function () {
            detailsPanel.hide();
            selRow.removeClass('selected');
        });
    }

    function setUpPanel() {
        var container, closeBtn, tblDiv;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2');

        tblDiv = top.append('div').classed('top-tables', true);
        tblDiv.append('div').classed('left', true).append('table');
        tblDiv.append('div').classed('right', true).append('table');

        top.append('div').classed('actionBtns', true);
        top.append('hr');

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h2').classed('ports-title', true).html('Ports');
        bottom.append('table');
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populateTop(tblDiv, btnsDiv, details) {
        var leftTbl = tblDiv.select('.left')
                        .select('table')
                        .append('tbody'),
            rightTbl = tblDiv.select('.right')
                        .select('table')
                        .append('tbody');

        is.loadEmbeddedIcon(iconDiv, details._iconid_type, 40);
        top.select('h2').html(details.id);

        propOrder.forEach(function (prop, i) {
            // properties are split into two tables
            addProp(i < 3 ? leftTbl : rightTbl, i, details[prop]);
        });

        bns.button(
            btnsDiv,
            bName + '-flows',
            'flowTable',
            function () {
                ns.navTo(flowPath, { devId: details.id });
            },
            'Show flow view for this device'
        );
        bns.button(
            btnsDiv,
            bName + '-ports',
            'portTable',
            function () {
                ns.navTo(portPath, { devId: details.id });
            },
            'Show port view for this device'
        );
        bns.button(
            btnsDiv,
            bName + '-groups',
            'groupTable',
            function () {
                ns.navTo(groupPath, { devId: details.id });
            },
            'Show group view for this device'
        );
    }

    function addPortRow(tbody, port) {
        var tr = tbody.append('tr');

        portCols.forEach(function (col) {
            tr.append('td').html(port[col]);
        });
    }

    function populateBottom(table, ports) {
        var theader = table.append('thead').append('tr'),
            tbody = table.append('tbody'),
            tbWidth, tbHeight;

        friendlyPortCols.forEach(function (col) {
            theader.append('th').html(col);
        });
        ports.forEach(function (port) {
            addPortRow(tbody, port);
        });

        tbWidth = fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = pHeight
                    - (fs.noPxStyle(detailsPanel.el()
                                        .select('.top'), 'height')
                    + fs.noPxStyle(detailsPanel.el()
                                        .select('.ports-title'), 'height')
                    + portsTblPdg);

        table.style({
            height: tbHeight + 'px',
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block'
        });

        detailsPanel.width(tbWidth + ctnrPdg);
    }

    function populateDetails(details) {
        var topTbs, btnsDiv, btmTbl, ports;
        setUpPanel();

        topTbs = top.select('.top-tables');
        btnsDiv = top.select('.actionBtns');
        btmTbl = bottom.select('table');
        ports = details.ports;

        populateTop(topTbs, btnsDiv, details);
        populateBottom(btmTbl, ports);

        detailsPanel.height(pHeight);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        $scope.$apply();
    }

    function createDetailsPane() {
        detailsPanel = ps.createPanel(pName, {
            width: wSize.width,
            margin: 0,
            hideMargin: 0
        });
        detailsPanel.el().style({
            position: 'absolute',
            top: pStartY + 'px'
        });
        detailsPanel.hide();
    }

    angular.module('ovDevice', [])
    .controller('OvDeviceCtrl',
        ['$log', '$scope', 'TableBuilderService', 'FnService',
            'MastService', 'PanelService', 'WebSocketService', 'IconService',
            'ButtonService', 'NavService', 'TooltipService',

        function (_$log_, _$scope_,
                  tbs, _fs_, _mast_, _ps_, _wss_, _is_, _bns_, _ns_, _ttip_) {
            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            wss = _wss_;
            is = _is_;
            bns = _bns_;
            ns = _ns_;
            ttip = _ttip_;
            var handlers = {};
            $scope.panelData = [];

            function selCb($event, row) {
                selRow = angular.element($event.currentTarget);
                if ($scope.selId) {
                    wss.sendEvent(detailsReq, { id: row.id });
                } else {
                    detailsPanel.hide();
                }
                $log.debug('Got a click on:', row);
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'device',
                selCb: selCb
            });

            // details panel handlers
            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvDeviceCtrl has been created');
        }])

        .directive('deviceDetailsPanel', ['$rootScope', '$window',
        function ($rootScope, $window) {
            return function (scope) {

                function heightCalc() {
                    pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                                            + mast.mastHeight() + topPdg;
                    wSize = fs.windowSize(pStartY);
                    pHeight = wSize.height;
                }
                heightCalc();

                createDetailsPane();

                scope.$watch('panelData', function () {
                    if (!fs.isEmptyObject(scope.panelData)) {
                        populateDetails(scope.panelData);
                        detailsPanel.show();
                    }
                });

                $rootScope.$watchCollection(
                    function () {
                        return {
                            h: $window.innerHeight,
                            w: $window.innerWidth
                        }
                    }, function () {
                        if (!fs.isEmptyObject(scope.panelData)) {
                            heightCalc();
                            populateDetails(scope.panelData);
                        }
                    }
                );

                scope.$on('$destroy', function () {
                    ps.destroyPanel(pName);
                });
            };
        }]);
}());
