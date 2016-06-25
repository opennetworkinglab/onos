/*
 *  Copyright 2015-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Topology Dialog Module.
 Creates a dialog box for the topology view.
 */

(function () {
    'use strict';

    // constants
    var idDialog = 'topo-p-dialog',
        opts = {
            cssCls: 'topo-p'
        };

    // ==========================

    angular.module('ovTopo')
    .factory('TopoDialogService',
        ['DialogService',

        function (ds) {
            return {
                openDialog: function () { return ds.openDialog(idDialog, opts); },
                closeDialog: ds.closeDialog,
                createDiv: ds.createDiv
            };
        }]);
}());
