"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
exports.__esModule = true;
/*
 * Copyright ${year}-present Open Networking Foundation
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
var core_1 = require("@angular/core");
var WelcomeComponent = /** @class */ (function () {
    function WelcomeComponent(log) {
        this.log = log;
        this.message = 'Welcome';
        this.welcomeEventEmitter = new core_1.EventEmitter();
        this.log.debug('WelcomeComponent constructed');
    }
    WelcomeComponent.prototype.ngOnInit = function () {
        this.log.debug('WelcomeComponent initialized', this.message, this.colour);
    };
    WelcomeComponent.prototype.welcomeClicked = function (colour) {
        this.log.debug(colour, 'WelcomeComponent clicked - sending event to parent');
        this.welcomeEventEmitter.emit(colour);
    };
    __decorate([
        core_1.Input()
    ], WelcomeComponent.prototype, "message");
    __decorate([
        core_1.Input()
    ], WelcomeComponent.prototype, "colour");
    __decorate([
        core_1.Output()
    ], WelcomeComponent.prototype, "welcomeEventEmitter");
    WelcomeComponent = __decorate([
        core_1.Component({
            selector: '${artifactId}-app-welcome',
            templateUrl: './welcome.component.html',
            styleUrls: ['./welcome.component.css']
        })
    ], WelcomeComponent);
    return WelcomeComponent;
}());
exports.WelcomeComponent = WelcomeComponent;
