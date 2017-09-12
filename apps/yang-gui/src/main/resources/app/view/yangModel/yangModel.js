/*
 * Copyright 2017-present Open Networking Foundation
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
    var topPdg = 20,
        hFudge = 12,
        srcFudge = 130,
        pWidth = 800;

    // internal state
    var detailsPanel,
        pStartY,
        pHeight,
        wSize,
        top,
        bottom,
        iconDiv,
        srcFrame,
        srcDiv;

    // constants
    var pName = 'yang-model-details-panel',
        detailsReq = 'yangModelDetailsRequest',
        detailsResp = 'yangModelDetailsResponse',
        fileUploadUrl = '/onos/yang/models?modelId=';


    function createDetailsPanel() {
        detailsPanel = ps.createPanel(pName, {
            width: pWidth,
            margin: 0,
            hideMargin: 0
        });
        detailsPanel.el().style({ top: pStartY + 'px'});
        $scope.hidePanel = function () { detailsPanel.hide(); };
        detailsPanel.hide();
    }

    function fixHeight(d, h) {
        d.style({ height: h + 'px'});
    }

    function populateDetails(details) {
        setUpPanel();
        populateTop(details);
        populateBottom(details.source);

        detailsPanel.height(pHeight);
        // also have to fix the height of the source frame to make scroll work
        fixHeight(srcFrame, pHeight - srcFudge);
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
        top.append('hr');

        bottom = container.append('div').classed('bottom', true);
        bottom.append('h3').text('YANG Source');

        srcFrame = bottom.append('div').classed('src-frame', true);
        srcDiv = srcFrame.append('div').classed('module-source', true);
        srcDiv.append('pre');
    }

    function populateTop(details) {
        is.loadEmbeddedIcon(iconDiv, 'nav_yang', 40);
        top.select('h2')
            .text('Module ' + details.id + ' (' + details.revision + ')');
    }

    function populateBottom(source) {
        var src = srcDiv.select('pre');
        src.text(source.join('\n'));
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
        $scope.$apply();
        $log.debug('YANG_MODEL>', detailsResp, data);
    }

    function handleEscape() {
        return closePanel();
    }


    // defines view controller
    angular.module('ovYangModel', [])
    .controller('OvYangModelCtrl', [
        '$log', '$scope', '$http', '$timeout',
        'TableBuilderService', 'TableDetailService',
        'FnService', 'MastService', 'PanelService', 'WebSocketService',
        'IconService', 'UrlFnService', 'FlashService',

        function (_$log_, _$scope_, $http, $timeout, tbs, tds, _fs_, _mast_, _ps_, _wss_, _is_, ufs, _flash_) {
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
                    wss.sendEvent(detailsReq, {
                        modelId: row.modelId,
                        id: row.id,
                        revision: row.revision
                    });
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

            $scope.$on('YangFileChanged', function () {
                var formData = new FormData(),
                    url, modelId, finished = false;

                if ($scope.yangFile) {
                    modelId = $scope.yangFile.name;
                    modelId = modelId.substr(0, modelId.lastIndexOf("."));
                    url = fileUploadUrl + modelId;
                    formData.append('file', $scope.yangFile);
                    $log.info('Compiling', $scope.yangFile);
                    d3.select('#frame').classed('dropping', false);

                    // FIXME: Replace this with dialog that shows progress...
                    for (var i = 0; i < 10; i++) {
                        $timeout(function () {
                            if (!finished) _flash_.flash('Compiling ' + modelId);
                        }, i * 1100);
                    }

                    $http.post(url, formData, {
                        transformRequest: angular.identity,
                        headers: {
                            'Content-Type': undefined
                        }
                    })
                    .finally(function () {
                        finished = true;
                        _flash_.flash('Compile completed for ' + modelId);
                        $scope.sortCallback($scope.sortParams);
                        document.getElementById('inputYangFileForm').reset();
                    });
                }
            });

            $scope.yangDropped = function() {
                $scope.$emit('YangFileChanged');
                $scope.yangFile = null;
            };

            $scope.$on('$destroy', function () {
                wss.unbindHandlers(handlers);
            });

            $log.log('OvYangModelCtrl has been created');
        }
    ])

    // triggers the input form to appear when button is clicked
    .directive('triggerYangForm', function () {
        return {
            restrict: 'A',
            link: function (scope, elem) {
                elem.bind('click', function () {
                    document.getElementById('uploadYangFile')
                        .dispatchEvent(new MouseEvent('click'));
                });
            }
        };
    })

    // binds the model file to the scope in scope.yangFile
    // sends upload request to the server
    .directive('yangFileModel', ['$parse',
        function ($parse) {
            return {
                restrict: 'A',
                link: function (scope, elem, attrs) {
                    var model = $parse(attrs.yangFileModel),
                        modelSetter = model.assign;

                    elem.bind('change', function () {
                        scope.$apply(function () {
                            modelSetter(scope, elem[0].files[0]);
                        });
                        scope.$emit('YangFileChanged');
                    });
                }
            };
        }
     ])

    .directive("yangfiledrop", ['$parse', '$document', function ($parse, $document) {
        return {
            restrict: "A",
            link: function (scope, element, attrs) {
                var onYangDrop = $parse(attrs.onFileDrop);

                // When an item is dragged over the document
                var onDragOver = function (e) {
                    d3.select('#frame').classed('dropping', true);
                    e.preventDefault();
                };

                // When the user leaves the window, cancels the drag or drops the item
                var onDragEnd = function (e) {
                    d3.select('#frame').classed('dropping', false);
                    e.preventDefault();
                };

                // When a file is dropped
                var loadFile = function (file) {
                    scope.yangFile = file;
                    scope.$apply(onYangDrop(scope));
                };

                // Dragging begins on the document
                $document.bind("dragover", onDragOver);

                // Dragging ends on the overlay, which takes the whole window
                element.bind("dragleave", onDragEnd)
                    .bind("drop", function (e) {
                        $log.info('Drag leave', e);
                        loadFile(e.dataTransfer.files[0]);
                        onDragEnd(e);
                    });
            }
        };
    }])

    .directive('yangModelDetailsPanel', [
        '$rootScope', '$window', '$timeout', 'KeyService',

    function ($rootScope, $window, $timeout, ks) {
        return function (scope) {
            var unbindWatch;

            function heightCalc() {
                pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
                        + mast.mastHeight() + topPdg;
                wSize = fs.windowSize(pStartY);
                pHeight = wSize.height - hFudge;
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