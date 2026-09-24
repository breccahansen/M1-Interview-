import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LotBadgeComponent } from './lot-badge.component';

describe('LotBadgeComponent', () => {
  let fixture: ComponentFixture<LotBadgeComponent>;
  let component: LotBadgeComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [LotBadgeComponent] }).compileComponents();
    fixture = TestBed.createComponent(LotBadgeComponent);
    component = fixture.componentInstance;
  });

  function badge(): HTMLElement {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.badge');
  }

  it('labels zero lots as "no lots"', () => {
    component.lots = 0;
    expect(badge().textContent).toBe('no lots');
  });

  it('uses singular for one lot', () => {
    component.lots = 1;
    expect(badge().textContent).toBe('1 lot');
  });

  it('uses plural and no "many" class for 2-3 lots', () => {
    component.lots = 3;
    const el = badge();
    expect(el.textContent).toBe('3 lots');
    expect(el.classList.contains('many')).toBeFalse();
  });

  it('adds the "many" class above three lots', () => {
    component.lots = 4;
    const el = badge();
    expect(el.textContent).toBe('4 lots');
    expect(el.classList.contains('many')).toBeTrue();
  });
});
