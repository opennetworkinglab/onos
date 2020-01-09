"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
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
var testing_1 = require("@angular/core/testing");
var welcome_component_1 = require("./welcome.component");
var router_1 = require("@angular/router");
var rxjs_1 = require("rxjs");
var gui2_fw_lib_1 = require("gui2-fw-lib");
var animations_1 = require("@angular/platform-browser/animations");
var forms_1 = require("@angular/forms");
var testing_2 = require("@angular/router/testing");
var MockActivatedRoute = /** @class */ (function (_super) {
    __extends(MockActivatedRoute, _super);
    function MockActivatedRoute(params) {
        var _this = _super.call(this) || this;
        _this.queryParams = rxjs_1.of(params);
        return _this;
    }
    return MockActivatedRoute;
}(router_1.ActivatedRoute));
var MockIconService = /** @class */ (function () {
    function MockIconService() {
    }
    MockIconService.prototype.loadIconDef = function () { };
    return MockIconService;
}());
describe('WelcomeComponent', function () {
    var fs;
    var ar;
    var windowMock;
    var logServiceSpy;
    var component;
    var fixture;
    beforeEach(testing_1.async(function () {
        var logSpy = jasmine.createSpyObj('LogService', ['info', 'debug', 'warn', 'error']);
        ar = new MockActivatedRoute({ 'debug': 'txrx' });
        windowMock = {
            location: {
                hostname: 'foo',
                host: 'foo',
                port: '80',
                protocol: 'http',
                search: { debug: 'true' },
                href: 'ws://foo:123/onos/ui/websock/path',
                absUrl: 'ws://foo:123/onos/ui/websock/path'
            }
        };
        fs = new gui2_fw_lib_1.FnService(ar, logSpy, windowMock);
        testing_1.TestBed.configureTestingModule({
            imports: [
                animations_1.BrowserAnimationsModule,
                forms_1.FormsModule,
                testing_2.RouterTestingModule,
                gui2_fw_lib_1.Gui2FwLibModule
            ],
            declarations: [welcome_component_1.WelcomeComponent],
            providers: [
                { provide: gui2_fw_lib_1.FnService, useValue: fs },
                { provide: gui2_fw_lib_1.LogService, useValue: logSpy },
                { provide: gui2_fw_lib_1.IconService, useClass: MockIconService },
                { provide: 'Window', useValue: windowMock },
            ]
        })
            .compileComponents();
        logServiceSpy = testing_1.TestBed.get(gui2_fw_lib_1.LogService);
    }));
    beforeEach(function () {
        fixture = testing_1.TestBed.createComponent(welcome_component_1.WelcomeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });
    it('should create', function () {
        expect(component).toBeTruthy();
    });
});
