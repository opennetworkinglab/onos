/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Sample View Module
 */

(function () {
    'use strict';

    // injected refs
    var $log, tbs, flash;

    // configuration
    var tbid = 'sample-toolbar';

    // internal state
    var togFnDiv, radFnP;

    function btnFn() {
        flash.flash('Hi there friends!');
    }

    function togFn(display) {
        togFnDiv.style('display', display ? 'block' : 'none');
    }

    function checkFn() {
        radFnP.text('Checkmark radio button active.')
            .style('color', 'green');
    }

    function xMarkFn() {
        radFnP.text('Xmark radio button active.')
            .style('color', 'red');
    }

    function birdFn() {
        radFnP.text('Bird radio button active.')
            .style('color', '#369');
    }


    // define the controller

    angular.module('ovSample', ['onosUtil'])
    .controller('OvSampleCtrl',
        ['$scope', '$log', 'ToolbarService', 'FlashService',

        function ($scope, _$log_, _tbs_, _flash_) {
            var self = this,
                toolbar,
                rset;

            $log = _$log_;
            tbs = _tbs_;
            flash = _flash_;

            self.message = 'Hey there folks!';

            togFnDiv = d3.select('#ov-sample')
                .append('div')
                .text('Look at me!')
                .style({
                    display: 'none',
                    color: 'rgb(204, 89, 81)',
                    'font-size': '20pt'
                });

            radFnP = d3.select('#ov-sample')
                .append('p')
                .style('font-size', '16pt');

            toolbar = tbs.createToolbar(tbid);
            rset = [
                { gid: 'checkMark', cb: checkFn, tooltip: 'rbtn tooltip' },
                { gid: 'xMark', cb: xMarkFn },
                { gid: 'bird', cb: birdFn, tooltip: 'hello' }
            ];

            toolbar.addButton('demo-button', 'crown', btnFn, 'yay a tooltip');
            toolbar.addToggle('demo-toggle', 'chain', false, togFn, 'another tooltip');
            toolbar.addSeparator();
            toolbar.addRadioSet('demo-radio', rset);
            toolbar.hide();

            checkFn();

            // Clean up on destroyed scope
            $scope.$on('$destroy', function () {
                tbs.destroyToolbar(tbid);
            });

         $log.log('OvSampleCtrl has been created');
    }]);
}());
