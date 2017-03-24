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
  ONOS GUI -- YANG Model table view
 */

(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, mast, ps, wss, is;

    // constants
    var topPdg = 28,
        ctnrPdg = 24,
        pWidth = 800,
        scrollSize = 17,
        tblPdg = 50;

    // internal state
    var detailsPanel,
        ymodel,
        pStartY,
        pHeight,
        wSize,
        top,
        topTable,
        bottom,
        iconDiv;

    // constants
    var pName = 'yang-model-details-panel',
        detailsReq = 'yangModelDetailsRequest',
        detailsResp = 'yangModelDetailsResponse',

        propOrder = [
        ],
        friendlyProps = [
        ],

        moduleCols = [
            'name', 'revision'
        ],
        friendlyModuleCols = [
            'Module Name', 'Revision'
        ];


    function createDetailsPanel() {
        detailsPanel = ps.createPanel(pName, {
            width: pWidth,
            margin: 0,
            hideMargin: 0
        });
        $scope.hidePanel = function () { detailsPanel.hide(); };
        detailsPanel.hide();
    }

    function populateDetails(details) {
        setUpPanel();

        populateTop(details);
        populateBottom(details.modules);

        // topData = top.select('.top-data');
        // populateTop(topData, details);
        detailsPanel.height(pHeight);
    }

    function setUpPanel() {
        var container, closeBtn, dataDiv;
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
        bottom.append('h2').classed('modules-title', true).html('Modules');
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

    function populateTop(details) {
        is.loadEmbeddedIcon(iconDiv, 'nav_yang', 40);
        top.select('h2').html('Model ID ' + details.id);

        var tbody = topTable.append('tbody');

        propOrder.forEach(function (prop, i) {
            addProp(tbody, i, details[prop]);
        });
    }

    function addModuleRow(tbody, module) {
        var tr = tbody.append('tr');
        moduleCols.forEach(function (col) {
            tr.append('td').attr('width', 160)
                .html('<a href="/v1/yang/modules/' + module.name + '" target="_model">' + module[col] + "</a>");
        });
    }

    function populateBottom(modules) {
        var table = bottom.select('table'),
            theader = table.append('thead').append('tr'),
            tbody = table.append('tbody'),
            tbWidth, tbHeight;

        friendlyModuleCols.forEach(function (col) {
            theader.append('th').html(col);
        });
        modules.forEach(function (module) {
            addModuleRow(tbody, module);
        });

        tbWidth = fs.noPxStyle(tbody, 'width') + scrollSize;
        tbHeight = pHeight
            - (fs.noPxStyle(detailsPanel.el()
                .select('.top'), 'height')
            + fs.noPxStyle(detailsPanel.el()
                .select('.modules-title'), 'height')
            + tblPdg);

        table.style({
            height: tbHeight + 'px',
            width: tbWidth + 'px',
            overflow: 'auto',
            display: 'block'
        });

        detailsPanel.width(tbWidth + ctnrPdg);
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


    // callback invoked when data from a details request returns from server
    function respDetailsCb(data) {
        $scope.panelData = data.details;
        ymodel = data.yangModel;
        $scope.$apply();
        // TODO: complete the detail panel directive.
        $log.debug('YANG_MODEL>', detailsResp, data);
    }

    function handleEscape() {
        return closePanel();
    }


    // defines view controller
    angular.module('ovYangModel', [])
    .controller('OvYangModelCtrl', [
        '$log', '$scope', 'TableBuilderService', 'TableDetailService',
        'FnService', 'MastService', 'PanelService', 'WebSocketService',
        'IconService',

        function (_$log_, _$scope_, tbs, tds, _fs_, _mast_, _ps_, _wss_, _is_) {
            var handlers = {};

            $log = _$log_;
            $scope = _$scope_;
            fs = _fs_;
            mast = _mast_;
            ps = _ps_;
            wss = _wss_;
            is = _is_;

            $scope.panelData = {};

            // register response handler
            handlers[detailsResp] = respDetailsCb;
            wss.bindHandlers(handlers);

            // row selection callback
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
                tag: 'yangModel',
                selCb: selCb
            });

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvYangModelCtrl has been created');
        }
    ])

    .directive('yangModelDetailsPanel', [
        '$rootScope', '$window', '$timeout', 'KeyService',

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
            ks.keyBindings({
                esc: [handleEscape, 'Close the details panel'],
                _helpFormat: ['esc']
            });
            ks.gestureNotes([
                ['click', 'Select a row to show YANG model details'],
                ['scroll down', 'See more models']
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