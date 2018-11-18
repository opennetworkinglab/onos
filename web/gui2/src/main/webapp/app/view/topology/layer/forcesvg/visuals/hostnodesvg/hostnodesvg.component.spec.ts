import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HostNodeSvgComponent } from './hostnodesvg.component';

describe('HostNodeSvgComponent', () => {
  let component: HostNodeSvgComponent;
  let fixture: ComponentFixture<HostNodeSvgComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ HostNodeSvgComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HostNodeSvgComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
