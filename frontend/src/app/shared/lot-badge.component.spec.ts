import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LotBadgeComponent } from './lot-badge.component';

describe('LotBadgeComponent', () => {
  let fixture: ComponentFixture<LotBadgeComponent>;
  let component: LotBadgeComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ declarations: [LotBadgeComponent] });
    fixture = TestBed.createComponent(LotBadgeComponent);
    component = fixture.componentInstance;
  });

  const badge = (): HTMLElement => fixture.nativeElement.querySelector('span.badge');

  it('defaults to "no lots" and no "many" class', () => {
    fixture.detectChanges();
    expect(component.label).toBe('no lots');
    expect(badge().textContent!.trim()).toBe('no lots');
    expect(badge().classList.contains('many')).toBeFalse();
  });

  it('uses singular for exactly one lot', () => {
    component.lots = 1;
    fixture.detectChanges();
    expect(badge().textContent!.trim()).toBe('1 lot');
    expect(badge().classList.contains('many')).toBeFalse();
  });

  it('pluralises for 2-3 lots without the "many" class', () => {
    component.lots = 3;
    fixture.detectChanges();
    expect(badge().textContent!.trim()).toBe('3 lots');
    expect(badge().classList.contains('many')).toBeFalse();
  });

  it('adds the "many" class above 3 lots', () => {
    component.lots = 4;
    fixture.detectChanges();
    expect(badge().textContent!.trim()).toBe('4 lots');
    expect(badge().classList.contains('many')).toBeTrue();
  });
});
