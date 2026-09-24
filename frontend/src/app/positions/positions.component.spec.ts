import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { Subject, throwError } from 'rxjs';
import { PositionsComponent } from './positions.component';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { LotBadgeComponent } from '../shared/lot-badge.component';
import { MoneyPipe } from '../shared/money.pipe';
import { PositionView } from '../shared/models';

describe('PositionsComponent', () => {
  let fixture: ComponentFixture<PositionsComponent>;
  let component: PositionsComponent;
  let api: jasmine.SpyObj<AdvisorApiService>;
  let paramMap$: Subject<ReturnType<typeof convertToParamMap>>;
  let positions$: Subject<PositionView[]>;

  const positions: PositionView[] = [
    { symbol: 'AAPL', quantity: 133, costBasis: 19974.29, averageUnitCost: 150.1826, lots: 2 },
    { symbol: 'VEA', quantity: 33, costBasis: 1654.46, averageUnitCost: 50.135, lots: 1 }
  ];

  beforeEach(async () => {
    api = jasmine.createSpyObj<AdvisorApiService>('AdvisorApiService', ['positions']);
    paramMap$ = new Subject();
    positions$ = new Subject();
    api.positions.and.returnValue(positions$);

    await TestBed.configureTestingModule({
      declarations: [PositionsComponent, LotBadgeComponent, MoneyPipe],
      providers: [
        { provide: AdvisorApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$ } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PositionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function text(selector: string): string | null {
    const el: HTMLElement | null = fixture.nativeElement.querySelector(selector);
    return el ? el.textContent!.trim() : null;
  }

  it('shows a loading indicator until positions arrive', () => {
    paramMap$.next(convertToParamMap({ accountNumber: '7781-2204' }));
    fixture.detectChanges();

    expect(component.loading).toBeTrue();
    expect(text('p')).toBe('Loading…');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(api.positions).toHaveBeenCalledOnceWith('7781-2204');
  });

  it('renders rows, per-row badges and the total cost basis', () => {
    paramMap$.next(convertToParamMap({ accountNumber: '7781-2204' }));
    positions$.next(positions);
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
    expect(text('h1')).toBe('Positions · 7781-2204');
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('AAPL');
    expect(rows[0].textContent).toContain('$150.18');
    expect(rows[0].textContent).toContain('$19,974.29');
    expect(rows[0].querySelector('.badge').textContent).toBe('2 lots');
    expect(rows[1].querySelector('.badge').textContent).toBe('1 lot');
    expect(component.totalCostBasis).toBeCloseTo(21628.75, 2);
    expect(text('tfoot td:nth-child(2)')).toBe('$21,628.75');
  });

  it('shows "Account not found" on 404', () => {
    api.positions.and.returnValue(throwError(() => ({ status: 404 })));
    paramMap$.next(convertToParamMap({ accountNumber: '0000-0000' }));
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Account not found');
    expect(text('p.error')).toBe('Account not found');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });

  it('shows "Service unavailable" on other errors', () => {
    api.positions.and.returnValue(throwError(() => ({ status: 500 })));
    paramMap$.next(convertToParamMap({ accountNumber: '7781-2204' }));
    fixture.detectChanges();

    expect(component.error).toBe('Service unavailable');
  });

  it('reloads when the route account changes', () => {
    paramMap$.next(convertToParamMap({ accountNumber: '7781-2204' }));
    positions$.next(positions);
    fixture.detectChanges();

    paramMap$.next(convertToParamMap({ accountNumber: '7781-9930' }));
    fixture.detectChanges();

    expect(component.accountNumber).toBe('7781-9930');
    expect(component.loading).toBeTrue();
    expect(api.positions).toHaveBeenCalledTimes(2);
    expect(api.positions.calls.mostRecent().args[0]).toBe('7781-9930');
  });

  it('totals zero with no positions and unsubscribes on destroy', () => {
    expect(component.totalCostBasis).toBe(0);
    paramMap$.next(convertToParamMap({ accountNumber: '7781-2204' }));
    expect(paramMap$.observed).toBeTrue();
    fixture.destroy();
    expect(paramMap$.observed).toBeFalse();
  });
});
