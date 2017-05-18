/*
 * Copyright 2017-present Open Networking Laboratory
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
 ONOS GUI -- Mapping Management View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, fs, tbs, mast, ps, wss, is, ks;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        top,
        topTable,
        keyDiv,
        valueDiv,
        topKeyTable,
        bottomValueTable,
        iconDiv,
        nameDiv,
        wSize;

    // constants
    var topPdg = 28,
        ctnrPdg = 24,
        scrollSize = 17,
        portsTblPdg = 50,
        htPdg = 479,
        wtPdg = 532,

        pName = 'mapping-details-panel',
        detailsReq = 'mappingDetailsRequest',
        detailsResp = 'mappingDetailsResponse',

        propOrder = [
            'mappingId', 'type', 'state', 'mappingAction'
        ],
        friendlyProps = [
            'Mapping ID', 'Store Type', 'State', 'Mapping Action'
        ],

        treatmentPropOrder = [
            'address', 'unicastPriority', 'unicastWeight', 'multicastPriority',
            'multicastWeight'
        ],
        treatmentFriendlyProps = [
            'Address', 'U P', 'U W', 'M P', 'M W'
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

    function handleEscape() {
        return closePanel();
    }

    function setUpPanel() {
        var container, closeBtn, tblDiv;
        detailsPanel.empty();

        container = detailsPanel.append('div').classed('container', true);
        top = container.append('div').classed('top', true);
        keyDiv = container.append('div').classed('top', true);
        valueDiv = container.append('div').classed('bottom', true);
        closeBtn = top.append('div').classed('close-btn', true);
        addCloseBtn(closeBtn);
        iconDiv = top.append('div').classed('dev-icon', true);
        top.append('h2');
        topTable = top.append('div').classed('top-content', true)
            .append('table');
        top.append('hr');
        keyDiv.append('h2').text('Mapping Key');
        topKeyTable = keyDiv.append('div').classed('top-content', true)
                            .append('table');
        keyDiv.append('hr');
        valueDiv.append('h2').text('Mapping Value');
        bottomValueTable = valueDiv.append('table');

        // TODO: add more details later
    }

    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }

    function populateTable(tbody, label, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('label', label + ' :');
        addCell('value', value);
    }

    function populateTop(details) {
        is.loadEmbeddedIcon(iconDiv, 'mappingTable', 40);
        top.select('h2').text(details.mappingId);

        var tbody = topTable.append('tbody');

        var topKeyTablebody = topKeyTable.append('tbody');
        var keyObject = details['mappingKey'];
        var address = keyObject.address;

        var bottomValueTableheader = bottomValueTable.append('thead').append('tr')
        var bottomValueTablebody = bottomValueTable.append('tbody');
        var valueObject = details['mappingValue'];
        var treatments = valueObject.mappingTreatments;

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, details[prop]);
        });

        topKeyTablebody.append('tr').append('td').attr('class', 'value').text(address);

        treatmentFriendlyProps.forEach(function (col) {
            bottomValueTableheader.append('th').text(col);
        });
        treatments.forEach(function (sel) {
            populateTreatmentTable(bottomValueTablebody, sel);
        });
    }

    function populateTreatmentTable(tbody, treatment) {
        var tr = tbody.append('tr');
        treatmentPropOrder.forEach(function (prop) {
            addTreatmentProp(tr, treatment[prop]);
        });
    }

    function addTreatmentProp(tr, value) {
        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }
        addCell('value', value);
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

    function populateDetails(details) {
        setUpPanel();
        populateTop(details);

        //ToDo add more details
        detailsPanel.height(pHeight);
        detailsPanel.width(wtPdg);
    }

    function respDetailsCb(data) {
        $log.debug("Got response from server :", data);
        $scope.panelData = data.details;
        $scope.$apply();
    }

    angular.module('ovMapping', [])
        .controller('OvMappingCtrl',
        ['$log', '$scope', '$location', 'FnService', 'TableBuilderService',
         'MastService', 'PanelService', 'KeyService', 'IconService',
         'WebSocketService',

        function (_$log_, _$scope_, _$location_, _fs_, _tbs_, _mast_, _ps_,
                    _ks_, _is_, _wss_) {
            var params,
                handlers = {};

            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            tbs = _tbs_;
            mast = _mast_;
            ps = _ps_;
            ks = _ks_;
            is = _is_;
            wss = _wss_;
            $scope.deviceTip = 'Show device table';
            $scope.briefTip = 'Switch to brief view';
            $scope.detailTip = 'Switch to detailed view';
            $scope.brief = true;

            params = $location.search();
            if (params.hasOwnProperty('devId')) {
                $scope.devId = params['devId'];
            }

            tbs.buildTable({
                scope: $scope,
                tag: 'mapping',
                selCb: selCb,
                query: params
             });

            // details panel handlers
            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

             function selCb($event, row) {
                 if ($scope.selId) {
                     wss.sendEvent(detailsReq, {mappingId: row.id, type: row.type});
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

             Object.defineProperty($scope, "queryFilter", {
                get: function() {
                    var out = {};
                    out[$scope.queryBy || "$"] = $scope.queryTxt;
                    return out;
                }
             });

            $log.log('OvMappingCtrl has been created');
        }])

    .directive('mappingDetailsPanel',
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
                esc: [handleEscape, 'Close the details panel'],
                _helpFormat: ['esc']
            });
            ks.gestureNotes([
                ['click', 'Select a row to show cluster node details'],
                ['scroll down', 'See available cluster nodes']
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
                ps.destroyPanel(pName);
            });
        };
    }]);
}());
