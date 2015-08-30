/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- App View Module
 */

(function () {
    'use strict';

    // constants
    var INSTALLED = 'INSTALLED',
        ACTIVE = 'ACTIVE',
        APP_MGMENT_REQ = 'appManagementRequest',
        FILE_UPLOAD_URL = 'applications/upload';

    angular.module('ovApp', [])
    .controller('OvAppCtrl',
        ['$log', '$scope', '$http',
        'FnService', 'TableBuilderService', 'WebSocketService', 'UrlFnService',
        'KeyService',

    function ($log, $scope, $http, fs, tbs, wss, ufs, ks) {
        $scope.ctrlBtnState = {};
        $scope.uploadTip = 'Upload an application (.oar file)';
        $scope.activateTip = 'Activate selected application';
        $scope.deactivateTip = 'Deactivate selected application';
        $scope.uninstallTip = 'Uninstall selected application';

        function selCb($event, row) {
            // selId comes from tableBuilder
            $scope.ctrlBtnState.selection = !!$scope.selId;
            refreshCtrls();
        }

        function refreshCtrls() {
            var row, rowIdx;
            if ($scope.ctrlBtnState.selection) {
                rowIdx = fs.find($scope.selId, $scope.tableData);
                row = rowIdx >= 0 ? $scope.tableData[rowIdx] : null;

                $scope.ctrlBtnState.installed = row && row.state === INSTALLED;
                $scope.ctrlBtnState.active = row && row.state === ACTIVE;
            } else {
                $scope.ctrlBtnState.installed = false;
                $scope.ctrlBtnState.active = false;
            }
        }

        tbs.buildTable({
            scope: $scope,
            tag: 'app',
            selCb: selCb,
            respCb: refreshCtrls
        });

        // TODO: reexamine where keybindings should be - directive or controller?
        ks.keyBindings({
            esc: [$scope.selectCallback, 'Deselect app'],
            _helpFormat: ['esc']
        });
        ks.gestureNotes([
            ['click row', 'Select / deselect app'],
            ['scroll down', 'See more apps']
        ]);

        $scope.appAction = function (action) {
            if ($scope.ctrlBtnState.selection) {
                $log.debug('Initiating ' + action + ' of ' + $scope.selId);
                wss.sendEvent(APP_MGMENT_REQ, {
                    action: action,
                    name: $scope.selId,
                    sortCol: $scope.sortParams.sortCol,
                    sortDir: $scope.sortParams.sortDir
                });
            }
        };

        $scope.$on('FileChanged', function () {
            var formData = new FormData();
            if ($scope.appFile) {
                formData.append('file', $scope.appFile);
                $http.post(ufs.rsUrl(FILE_UPLOAD_URL), formData, {
                    transformRequest: angular.identity,
                    headers: {
                        'Content-Type': undefined
                    }
                })
                    .finally(function () {
                        $scope.sortCallback($scope.sortParams);
                        document.getElementById('inputFileForm').reset();
                    });
            }
        });

        $scope.$on('$destroy', function () {
            ks.unbindKeys();
        });

        $log.log('OvAppCtrl has been created');
    }])

    // triggers the input form to appear when button is clicked
    .directive('triggerForm', function () {
        return {
            restrict: 'A',
            link: function (scope, elem) {
                elem.bind('click', function () {
                    document.getElementById('uploadFile')
                        .dispatchEvent(new MouseEvent('click'));
                });
            }
        };
    })

    // binds the model file to the scope in scope.appFile
    // sends upload request to the server
    .directive('fileModel', ['$parse',
            function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, elem, attrs) {
                var model = $parse(attrs.fileModel),
                    modelSetter = model.assign;

                elem.bind('change', function () {
                    scope.$apply(function () {
                        modelSetter(scope, elem[0].files[0]);
                    });
                    scope.$emit('FileChanged');
                });
            }
        };
    }]);
}());
