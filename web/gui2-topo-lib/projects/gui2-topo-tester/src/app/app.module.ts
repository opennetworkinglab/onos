import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import {ConsoleLoggerService, Gui2FwLibModule, LogService} from 'gui2-fw-lib';
import {Gui2TopoLibModule} from 'gui2-topo-lib';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterModule, Routes} from '@angular/router';
import {HttpClientModule} from '@angular/common/http';

const appRoutes: Routes = [
    { path: '**', component: AppComponent }
];

@NgModule({
    declarations: [
        AppComponent
    ],
    imports: [
        RouterModule.forRoot(appRoutes),
        BrowserModule,
        BrowserAnimationsModule,
        Gui2FwLibModule,
        Gui2TopoLibModule,
        HttpClientModule
    ],
    providers: [
        { provide: LogService, useClass: ConsoleLoggerService },
        { provide: 'Window', useValue: window }
    ],
    bootstrap: [AppComponent]
})
export class AppModule { }
