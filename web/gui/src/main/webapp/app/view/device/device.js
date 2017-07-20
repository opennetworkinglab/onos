/*
 * Copyright 2015-present Open Networking Laboratory
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
    var $log, $scope, $loc, fs, mast, ps, wss, is, ns, ks;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        bottom,
        iconDiv,
        wSize,
        editingName = false,
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

        propSplit = 4,
        propOrder = [
            'id', 'type', 'masterid', 'chassisid',
            'mfr', 'hw', 'sw', 'protocol', 'serial'
        ],
        friendlyProps = [
            'URI', 'Type', 'Master ID', 'Chassis ID',
            'Vendor', 'H/W Version', 'S/W Version', 'Protocol', 'Serial #'
        ],
        portCols = [
            'enabled', 'id', 'speed', 'type', 'elinks_dest', 'name'
        ],
        friendlyPortCols = [
            'Enabled', 'ID', 'Speed', 'Type', 'Egress Links', 'Name'
        ];

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

    function exitEditMode(nameH2, name) {
        nameH2.text(name);
        nameH2.classed('editable clickable', true);
        editingName = false;
        ks.enableGlobalKeys(true);
    }

    function editNameSave() {
        var nameH2 = top.select('h2'),
            id = $scope.panelData.id,
            val,
            newVal;

        if (editingName) {
            val = nameH2.select('input').property('value').trim();
            newVal = val || id;

            exitEditMode(nameH2, newVal);
            $scope.panelData.name = newVal;
            wss.sendEvent(nameChangeReq, { id: id, name: val });
        }
    }

    function editNameCancel() {
        if (editingName) {
            exitEditMode(top.select('h2'), $scope.panelData.name);
            return true;
        }
        return false;
    }

    function editName() {
        var nameH2 = top.select('h2'),
            tf, el;

        if (!editingName) {
            nameH2.classed('editable clickable', false);
            nameH2.text('');
            tf = nameH2.append('input').classed('name-input', true)
                .attr('type', 'text')
                .attr('value', $scope.panelData.name);
            el = tf[0][0];
            el.focus();
            el.select();
            editingName = true;
            ks.enableGlobalKeys(false);
        }
    }

    function handleEscape() {
        return editNameCancel() || closePanel();
    }

    function setUpPanel() {
        var container, closeBtn, tblDiv;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2').classed('editable clickable', true).on('click', editName);

        tblDiv = top.append('div').classed('top-tables', true);
        tblDiv.append('div').classed('left', true).append('table');
        tblDiv.append('div').classed('right', true).append('table');

        top.append('hr');

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h2').classed('ports-title', true).text('Ports');
        bottom.append('table');
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populateTop(tblDiv, details) {
        var leftTbl = tblDiv.select('.left')
                        .select('table')
                        .append('tbody'),
            rightTbl = tblDiv.select('.right')
                        .select('table')
                        .append('tbody');

        is.loadEmbeddedIcon(iconDiv, details._iconid_type, 40);
        top.select('h2').text(details.name);

        // === demonstrate use of JsonCodec object see ONOS-5976
        addProp(leftTbl,  0, device.id);
        addProp(leftTbl,  1, device.type);
        addProp(leftTbl,  2, details['masterid']);
        addProp(leftTbl,  3, details['chassid']);
        addProp(rightTbl, 4, device.mfr);
        addProp(rightTbl, 5, device.hw);
        addProp(rightTbl, 6, device.sw);
        addProp(rightTbl, 7, details['protocol']);
        addProp(rightTbl, 8, device.serial);

        // propOrder.forEach(function (prop, i) {
        //     // properties are split into two tables
        //     addProp(i < propSplit ? leftTbl : rightTbl, i, details[prop]);
        // });
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
        var topTbs, btmTbl, ports;

        setUpPanel();

        topTbs = top.select('.top-tables');
        btmTbl = bottom.select('table');
        ports = details.ports;

        populateTop(topTbs, details);
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
        $scope.hidePanel = function () { detailsPanel.hide(); };
        detailsPanel.hide();
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
            'NavService', 'KeyService',

        function (_$log_, _$scope_, _$location_,
                  tbs, tds, _fs_, _mast_, _ps_, _wss_, _is_, _ns_, _ks_) {
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

            params = $loc.search();

            $scope.panelData = {};
            $scope.flowTip = 'Show flow view for selected device';
            $scope.portTip = 'Show port view for selected device';
            $scope.groupTip = 'Show group view for selected device';
            $scope.meterTip = 'Show meter view for selected device';

            // details panel handlers
            handlers[detailsResp] = respDetailsCb;
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
                selCb: selCb
            });


            // ==================== for testing for now ===============
            // TODO: more than just an example
            tds.buildBasePanel({
                popTop: popTop,
                popMid: popMid
            });
            // ==================== for testing for now ===============


            $scope.nav = function (path) {
                if ($scope.selId) {
                    ns.navTo(path, { devId: $scope.selId });
                }
            };

            $scope.$on('$destroy', function () {
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
                enter: editNameSave,
                esc: [handleEscape, 'Close the details panel'],
                _helpFormat: ['esc']
            });
            ks.gestureNotes([
                ['click', 'Select a row to show device details'],
                ['scroll down', 'See more devices']
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
                        w: $window.innerWidth
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
