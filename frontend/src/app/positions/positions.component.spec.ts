import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { PositionsComponent } from './positions.component';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { MoneyPipe } from '../shared/money.pipe';
import { LotBadgeComponent } from '../shared/lot-badge.component';
import { PositionView } from '../shared/models';

describe('PositionsComponent', () => {
  let fixture: ComponentFixture<PositionsComponent>;
  let component: PositionsComponent;
  let api: jasmine.SpyObj<AdvisorApiService>;
  let paramMap$: Subject<ParamMap>;

  const positions: PositionView[] = [
    { symbol: 'AAPL', quantity: 133, costBasis: 19974.29, averageUnitCost: 150.1826, lots: 2 },
    { symbol: 'VTI', quantity: 212.5, costBasis: 46609.75, averageUnitCost: 219.34, lots: 1 },
    { symbol: 'VEA', quantity: 33, costBasis: 1654.46, averageUnitCost: 50.14, lots: 1 }
  ];

  beforeEach(() => {
    api = jasmine.createSpyObj<AdvisorApiService>('AdvisorApiService', ['positions']);
    paramMap$ = new Subject<ParamMap>();
    TestBed.configureTestingModule({
      declarations: [PositionsComponent, MoneyPipe, LotBadgeComponent],
      providers: [
        { provide: AdvisorApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$.asObservable() } }
      ]
    });
    fixture = TestBed.createComponent(PositionsComponent);
    component = fixture.componentInstance;
  });

  const el = (): HTMLElement => fixture.nativeElement;
  const text = (selector: string) => el().querySelector(selector)?.textContent?.trim();
  const cells = (row: Element) => Array.from(row.querySelectorAll('td')).map(td => td.textContent!.trim());

  function route(accountNumber: string) {
    paramMap$.next(convertToParamMap({ accountNumber }));
    fixture.detectChanges();
  }

  it('shows the loading indicator before the API responds', () => {
    api.positions.and.returnValue(new Subject<PositionView[]>());
    fixture.detectChanges();
    route('7781-2204');

    expect(component.loading).toBeTrue();
    expect(text('p')).toBe('Loading…');
    expect(el().querySelector('table')).toBeNull();
    expect(api.positions).toHaveBeenCalledOnceWith('7781-2204');
  });

  it('renders one row per position with formatted money and a lot badge, plus the total', () => {
    api.positions.and.returnValue(of(positions));
    fixture.detectChanges();
    route('7781-2204');

    expect(component.loading).toBeFalse();
    expect(component.error).toBeNull();
    expect(text('h1')).toBe('Positions · 7781-2204');
    expect(el().querySelector('p')).toBeNull();

    const rows = Array.from(el().querySelectorAll('tbody tr'));
    expect(rows.length).toBe(3);
    expect(cells(rows[0])).toEqual(['AAPL', '133', '$150.18', '$19,974.29', '2 lots']);
    expect(cells(rows[1])).toEqual(['VTI', '212.5', '$219.34', '$46,609.75', '1 lot']);
    expect(cells(rows[2])).toEqual(['VEA', '33', '$50.14', '$1,654.46', '1 lot']);

    // 19974.29 + 46609.75 + 1654.46
    expect(component.totalCostBasis).toBeCloseTo(68238.5, 2);
    expect(cells(el().querySelector('tfoot tr')!)).toEqual(['Total', '$68,238.50', '']);
  });

  it('total is zero and table is empty for an account with no positions', () => {
    api.positions.and.returnValue(of([]));
    fixture.detectChanges();
    route('7782-0017');

    expect(component.totalCostBasis).toBe(0);
    expect(el().querySelectorAll('tbody tr').length).toBe(0);
    expect(cells(el().querySelector('tfoot tr')!)).toEqual(['Total', '$0.00', '']);
  });

  it('shows "Account not found" on 404 and hides the table', () => {
    api.positions.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    fixture.detectChanges();
    route('0000-0000');

    expect(component.loading).toBeFalse();
    expect(component.error).toBe('Account not found');
    expect(text('p.error')).toBe('Account not found');
    expect(el().querySelector('table')).toBeNull();
  });

  it('shows "Service unavailable" on non-404 errors', () => {
    api.positions.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture.detectChanges();
    route('7781-2204');

    expect(component.error).toBe('Service unavailable');
    expect(text('p.error')).toBe('Service unavailable');
  });

  it('reloads when the route account changes and resets error/loading state', () => {
    api.positions.and.callFake((acct: string) =>
      acct === '7781-2204' ? of(positions) : of([positions[1]]));
    fixture.detectChanges();
    route('7781-2204');
    expect(el().querySelectorAll('tbody tr').length).toBe(3);

    route('7781-9930');

    expect(component.accountNumber).toBe('7781-9930');
    expect(text('h1')).toBe('Positions · 7781-9930');
    expect(el().querySelectorAll('tbody tr').length).toBe(1);
    expect(component.error).toBeNull();
    expect(component.loading).toBeFalse();
    expect(api.positions).toHaveBeenCalledTimes(2);
  });

  it('defaults accountNumber to empty string when the route param is missing', () => {
    api.positions.and.returnValue(of([]));
    fixture.detectChanges();
    paramMap$.next(convertToParamMap({}));
    fixture.detectChanges();

    expect(component.accountNumber).toBe('');
    expect(api.positions).toHaveBeenCalledOnceWith('');
  });

  it('unsubscribes from the route on destroy', () => {
    api.positions.and.returnValue(of(positions));
    fixture.detectChanges();
    expect(paramMap$.observed).toBeTrue();

    fixture.destroy();

    expect(paramMap$.observed).toBeFalse();
  });
});
