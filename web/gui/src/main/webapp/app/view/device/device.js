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
        detailsPane,
        container, top, bottom, closeBtn;

    // constants
    // TODO: consider having a set y height that all tables start at
    var h2Pdg = 40,
        mastPdg = 8,
        tbodyPdg = 5,
        cntrPdg = 24,
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

    function setUpPanel() {
        detailsPane.empty();

        container = detailsPane.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        top.append('h2');
        top.append('table');

        container.append('hr');

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h2').text('Ports');
        bottom.append('table');
    }

    function createDetailsPane() {
        var headerHeight = h2Pdg + fs.noPxStyle(d3.select('h2'), 'height'),
            panelTop = headerHeight + tbodyPdg + mast.mastHeight() + mastPdg,
            wSize = fs.windowSize(panelTop);

        detailsPane = ps.createPanel(pName, {
            height: wSize.height,
            width: wSize.width / 2,
            margin: 0,
            hideMargin: 0
        });

        detailsPane.el().style({
            position: 'absolute',
            top: panelTop + 'px'
        });

        setUpPanel();

        detailsPane.hide();
    }

    function addCloseBtn(div) {
        is.loadEmbeddedIcon(div, 'appPlus', 30);
        div.select('g').attr('transform', 'translate(25, 0) rotate(45)');
        div.on('click', function () {
            detailsPane.hide();
            // TODO: deselect the table row when button is clicked
            //$scope.sel = null;
        });
    }

    function populateTopHalf(tbody, details) {
        top.select('h2').text(details['id']);

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, details[prop]);
        });
    }

    function populateBottomHalf(table, ports) {
        var theader = table.append('thead').append('tr'),
            tbody = table.append('tbody'),
            tbWidth, tbHeight,
            scrollSize = 20,
            btmPdg = 50;

        friendlyPortCols.forEach(function (header) {
            theader.append('th').html(header);
        });
        ports.forEach(function (port) {
            addPortRow(tbody, port);
        });

        tbWidth = fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = detailsPane.height()
                    - (fs.noPxStyle(detailsPane.el().select('.top'), 'height')
                    + fs.noPxStyle(detailsPane.el().select('hr'), 'height')
                    + fs.noPxStyle(detailsPane.el().select('h2'), 'height')
                    + btmPdg);

        table.style({
            height: tbHeight + 'px',
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block'
        });

        detailsPane.width(tbWidth + cntrPdg);
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
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

    function populateDetails(details) {
        setUpPanel();

        var toptbody = top.select('table').append('tbody'),
            btmTable = bottom.select('table'),
            ports = details.ports;

        populateTopHalf(toptbody, details);
        populateBottomHalf(btmTable, ports);
    }

    function respDetailsCb(data) {
        self.panelData = data['details'];
        populateDetails(self.panelData);
        detailsPane.show();
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

            function selCb(row) {
                // request the server for more information
                // get the id from the row to request details with
                if ($scope.sel) {
                    wss.sendEvent(detailsReq, { id: row.id });
                } else {
                    detailsPane.hide();
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

            // bind websocket handlers
            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            $scope.$on('$destroy', function () {
                ps.destroyPanel(pName);
                wss.unbindHandlers(handlers);
            });

            $log.log('OvDeviceCtrl has been created');
        }]);
}());
