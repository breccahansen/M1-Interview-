import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, of, throwError } from 'rxjs';
import { TransferComponent } from './transfer.component';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { MoneyPipe } from '../shared/money.pipe';
import { TransferResult } from '../shared/models';

describe('TransferComponent', () => {
  let fixture: ComponentFixture<TransferComponent>;
  let component: TransferComponent;
  let api: jasmine.SpyObj<AdvisorApiService>;

  const result: TransferResult = {
    transferId: 'TRF-2026-ABCD1234', fromAccount: '7781-2204', toAccount: '7781-9930',
    symbol: 'AAPL', quantity: 10, costBasisMoved: 1432.7, lotsMoved: 1
  };

  beforeEach(async () => {
    api = jasmine.createSpyObj<AdvisorApiService>('AdvisorApiService', ['transfer']);
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [TransferComponent, MoneyPipe],
      providers: [{ provide: AdvisorApiService, useValue: api }]
    }).compileComponents();

    fixture = TestBed.createComponent(TransferComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function fill(overrides: Partial<{ fromAccount: string; toAccount: string; symbol: string; quantity: number | null; method: string }> = {}) {
    // the untyped view mirrors what the <input type="number"> writes at runtime
    (component.form as FormGroup).patchValue({ symbol: 'aapl', quantity: 10, ...overrides });
  }

  function submitForm() {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('starts with default accounts, FIFO and an invalid form', () => {
    expect(component.form.value.fromAccount).toBe('7781-2204');
    expect(component.form.value.toAccount).toBe('7781-9930');
    expect(component.form.value.method).toBe('FIFO');
    expect(component.form.invalid).toBeTrue();
    expect(fixture.nativeElement.querySelectorAll('option').length).toBe(4);
  });

  it('does not submit an invalid form and marks controls touched', () => {
    submitForm();

    expect(api.transfer).not.toHaveBeenCalled();
    expect(component.form.get('symbol')!.touched).toBeTrue();
    expect(component.form.get('quantity')!.touched).toBeTrue();
  });

  it('validates account format, symbol length and minimum quantity', () => {
    fill({ fromAccount: '7781', symbol: 'TOOLONGX', quantity: 0 });
    expect(component.form.get('fromAccount')!.hasError('pattern')).toBeTrue();
    expect(component.form.get('symbol')!.hasError('maxlength')).toBeTrue();
    expect(component.form.get('quantity')!.hasError('min')).toBeTrue();

    fill({ fromAccount: '7781-2204', symbol: 'AAPL', quantity: 1 });
    expect(component.form.valid).toBeTrue();
  });

  it('rejects same from/to account without calling the API', () => {
    fill({ toAccount: '7781-2204' });
    submitForm();

    expect(api.transfer).not.toHaveBeenCalled();
    expect(component.error).toBe('From and to accounts must differ');
    expect(fixture.nativeElement.querySelector('p.error').textContent.trim()).toBe('From and to accounts must differ');
  });

  it('submits an upper-cased, numeric request and renders the result', () => {
    api.transfer.and.returnValue(of(result));
    fill({ method: 'LIFO' });
    submitForm();

    expect(api.transfer).toHaveBeenCalledOnceWith({
      fromAccount: '7781-2204', toAccount: '7781-9930', symbol: 'AAPL', quantity: 10, method: 'LIFO'
    });
    expect(component.result).toEqual(result);
    expect(component.error).toBeNull();
    expect(component.submitting).toBeFalse();
    const text = fixture.nativeElement.querySelector('.result').textContent.replace(/\s+/g, ' ');
    expect(text).toContain('TRF-2026-ABCD1234');
    expect(text).toContain('moved 10 AAPL');
    expect(text).toContain('1 lots, $1,432.70 cost basis');
    expect(fixture.nativeElement.querySelector('p.error')).toBeNull();
  });

  it('disables the button while a request is in flight and ignores re-submits', () => {
    const pending = new Subject<TransferResult>();
    api.transfer.and.returnValue(pending);
    fill();
    submitForm();

    expect(component.submitting).toBeTrue();
    expect(fixture.nativeElement.querySelector('button').disabled).toBeTrue();

    component.submit();
    expect(api.transfer).toHaveBeenCalledTimes(1);

    pending.next(result);
    fixture.detectChanges();
    expect(component.submitting).toBeFalse();
    expect(fixture.nativeElement.querySelector('button').disabled).toBeFalse();
  });

  it('shows the server message on a 422 with a string body', () => {
    api.transfer.and.returnValue(throwError(() => ({ status: 422, error: 'Cross-advisor transfers require ACATS workflow' })));
    fill();
    submitForm();

    expect(component.error).toBe('Cross-advisor transfers require ACATS workflow');
    expect(component.result).toBeNull();
    expect(component.submitting).toBeFalse();
    expect(fixture.nativeElement.querySelector('p.error').textContent.trim()).toBe('Cross-advisor transfers require ACATS workflow');
  });

  it('falls back to a generic message when the error body is not a string', () => {
    api.transfer.and.returnValue(throwError(() => ({ status: 500, error: { message: 'boom' } })));
    fill();
    submitForm();

    expect(component.error).toBe('Transfer rejected');
  });

  it('clears a previous result and error on a new submission', () => {
    api.transfer.and.returnValue(of(result));
    fill();
    submitForm();
    expect(component.result).toEqual(result);

    api.transfer.and.returnValue(throwError(() => ({ status: 422, error: 'Insufficient shares' })));
    submitForm();
    expect(component.result).toBeNull();
    expect(component.error).toBe('Insufficient shares');
  });
});
