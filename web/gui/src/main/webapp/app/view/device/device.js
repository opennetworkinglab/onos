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
    var $log, $scope, fs, mast, ps, wss, is;

    // internal state
    var self,
        detailsPanel,
        pStartY, pHeight,
        top, bottom, iconDiv,
        wSize, selRow;

    // constants
    var topPdg = 13,
        ctnrPdg = 24,
        scrollSize = 17,
        portsTblPdg = 50,

        pName = 'device-details-panel',
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
            'enabled', 'id', 'speed', 'type', 'elinks_dest'
        ],
        friendlyPortCols = [
            'Enabled', 'ID', 'Speed', 'Type', 'Egress Links'
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
        var container, closeBtn;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2');
        top.append('table');
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

    function populateTop(tbody, details) {
        is.loadEmbeddedIcon(iconDiv, details._iconid_type, 40);
        top.select('h2').html(details.id);

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, details[prop]);
        });
    }

    function addPortRow(tbody, port) {
        var tr = tbody.append('tr');

        portCols.forEach(function (col) {
            if (col === 'type' || col === 'id') {
                port[col] = fs.cap(port[col]);
            }
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
        var topTb, btmTbl, ports;
        setUpPanel();

        topTb = top.select('table').append('tbody');
        btmTbl = bottom.select('table');
        ports = details.ports;

        populateTop(topTb, details);
        populateBottom(btmTbl, ports);

        detailsPanel.height(pHeight);
    }

    function respDetailsCb(data) {
        self.panelData = data.details;
        populateDetails(self.panelData);
        detailsPanel.show();
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

        function (_$log_, _$scope_, tbs, _fs_, _mast_, _ps_, _wss_, _is_) {
            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            wss = _wss_;
            is = _is_;
            self = this;
            var handlers = {};
            self.panelData = [];
            pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                                            + mast.mastHeight() + topPdg;
            wSize = fs.windowSize(pStartY);
            pHeight = wSize.height;

            function selCb($event, row) {
                selRow = angular.element($event.currentTarget);
                if ($scope.sel) {
                    wss.sendEvent(detailsReq, { id: row.id });
                } else {
                    detailsPanel.hide();
                }
                $log.debug('Got a click on:', row);
            }

            tbs.buildTable({
                self: self,
                scope: $scope,
                tag: 'device',
                selCb: selCb
            });
            createDetailsPane();

            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            $scope.$on('$destroy', function () {
                ps.destroyPanel(pName);
                wss.unbindHandlers(handlers);
            });

            $log.log('OvDeviceCtrl has been created');
        }]);
}());
