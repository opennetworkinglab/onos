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
 ONOS GUI -- Host View Module
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
        iconDiv,
        wSize,
        editingName = false,
        host;

    // constants
    var topPdg = 28,
        pName = 'host-details-panel',
        detailsReq = 'hostDetailsRequest',
        detailsResp = 'hostDetailsResponse',
        nameChangeReq = 'hostNameChangeRequest',
        nameChangeResp = 'hostNameChangeResponse';

    var propOrder = [
            'id', 'ip', 'mac', 'vlan', 'configured', 'location'
        ],
        friendlyProps = [
            'Host ID', 'IP Address', 'MAC Address', 'VLAN',
            'Configured', 'Location'
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
            ip = $scope.panelData.ip,
            val,
            newVal;

        if (editingName) {
            val = nameH2.select('input').property('value').trim();
            newVal = val || ip;

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
        var container, closeBtn;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);

        top = container.append('div').classed('top', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('host-icon', true);
        top.append('h2').classed('editable clickable', true).on('click', editName);

        top.append('div').classed('top-tables', true);
        top.append('hr');
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populateTop(details) {
        var tab = top.select('.top-tables').append('tbody');

        is.loadEmbeddedIcon(iconDiv, details._iconid_type, 40);
        top.select('h2').text(details.name);

        propOrder.forEach(function (prop, i) {
            addProp(tab, i, details[prop]);
        });
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(details);
        detailsPanel.height(pHeight);
        // configure width based on content.. for now hardcoded
        detailsPanel.width(400);
    }

    function respDetailsCb(data) {
        $scope.panelData = data.details;
        host = data.host;
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


    // Defines the Host View controller...
    angular.module('ovHost', [])
    .controller('OvHostCtrl',
        ['$log', '$scope',
            '$location',
            'TableBuilderService',
            'FnService', 'MastService', 'PanelService', 'WebSocketService',
            'IconService', 'NavService', 'KeyService',

        function (_$log_, _$scope_, _$location_,
                  tbs,
                  _fs_, _mast_, _ps_, _wss_,
                  _is_, _ns_, _ks_) {

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

            // details panel handlers
            handlers[detailsResp] = respDetailsCb;
            handlers[nameChangeResp] = respNameCb;
            wss.bindHandlers(handlers);

            // query for if a certain host needs to be highlighted
            if (params.hasOwnProperty('hostId')) {
                $scope.selId = params['hostId'];
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
                tag: 'host',
                selCb: selCb
            });

            $scope.nav = function (path) {
                if ($scope.selId) {
                    ns.navTo(path, { hostId: $scope.selId });
                }
            };

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvHostCtrl has been created');
        }])

    .directive('hostDetailsPanel',
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
