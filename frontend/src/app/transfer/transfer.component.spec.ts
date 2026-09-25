import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { TransferComponent } from './transfer.component';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { MoneyPipe } from '../shared/money.pipe';
import { TransferResult } from '../shared/models';

describe('TransferComponent', () => {
  let fixture: ComponentFixture<TransferComponent>;
  let component: TransferComponent;
  let api: jasmine.SpyObj<AdvisorApiService>;

  const success: TransferResult = {
    transferId: 'TRF-2026-ABCD1234',
    fromAccount: '7781-2204',
    toAccount: '7781-9930',
    symbol: 'AAPL',
    quantity: 40,
    costBasisMoved: 5730.8,
    lotsMoved: 1
  };

  beforeEach(() => {
    api = jasmine.createSpyObj<AdvisorApiService>('AdvisorApiService', ['transfer']);
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [TransferComponent, MoneyPipe],
      providers: [{ provide: AdvisorApiService, useValue: api }]
    });
    fixture = TestBed.createComponent(TransferComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const el = (): HTMLElement => fixture.nativeElement;
  const text = (selector: string) => el().querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();

  // Untyped view of the form so specs can patch the number-typed input rendered into the `null`-seeded control.
  const form = (): FormGroup => component.form;

  function fillValid(overrides: Record<string, unknown> = {}) {
    form().patchValue({ symbol: 'aapl', quantity: 40, ...overrides });
  }

  describe('form defaults and validation', () => {
    it('starts with seeded accounts, FIFO and an invalid (empty) symbol/quantity', () => {
      expect(component.form.value).toEqual({
        fromAccount: '7781-2204',
        toAccount: '7781-9930',
        symbol: '',
        quantity: null,
        method: 'FIFO'
      });
      expect(component.form.invalid).toBeTrue();
      expect(component.form.get('symbol')!.hasError('required')).toBeTrue();
      expect(component.form.get('quantity')!.hasError('required')).toBeTrue();
      expect(component.methods).toEqual(['FIFO', 'LIFO', 'HIGH_COST', 'AVERAGE_COST']);
      expect(el().querySelectorAll('select option').length).toBe(4);
    });

    it('rejects malformed account numbers', () => {
      component.form.patchValue({ fromAccount: '77812204', toAccount: 'abcd-1234' });
      expect(component.form.get('fromAccount')!.hasError('pattern')).toBeTrue();
      expect(component.form.get('toAccount')!.hasError('pattern')).toBeTrue();
    });

    it('rejects symbols longer than 6 characters and quantities below 1', () => {
      form().patchValue({ symbol: 'TOOLONGX', quantity: 0 });
      expect(component.form.get('symbol')!.hasError('maxlength')).toBeTrue();
      expect(component.form.get('quantity')!.hasError('min')).toBeTrue();
    });

    it('does not call the API when invalid and marks all controls touched', () => {
      component.submit();

      expect(api.transfer).not.toHaveBeenCalled();
      expect(component.form.get('symbol')!.touched).toBeTrue();
      expect(component.form.get('quantity')!.touched).toBeTrue();
      expect(component.error).toBeNull();
    });
  });

  describe('same-account guard', () => {
    it('blocks transfers where from and to match, without calling the API', () => {
      fillValid({ toAccount: '7781-2204' });

      component.submit();
      fixture.detectChanges();

      expect(api.transfer).not.toHaveBeenCalled();
      expect(component.error).toBe('From and to accounts must differ');
      expect(text('p.error')).toBe('From and to accounts must differ');
      expect(component.submitting).toBeFalse();
    });
  });

  describe('submission', () => {
    it('sends an upper-cased symbol and numeric quantity, then renders the result', () => {
      api.transfer.and.returnValue(of(success));
      fillValid({ method: 'LIFO' });

      component.submit();
      fixture.detectChanges();

      expect(api.transfer).toHaveBeenCalledOnceWith({
        fromAccount: '7781-2204',
        toAccount: '7781-9930',
        symbol: 'AAPL',
        quantity: 40,
        method: 'LIFO'
      });
      expect(component.result).toEqual(success);
      expect(component.error).toBeNull();
      expect(component.submitting).toBeFalse();
      expect(text('.result')).toBe(
        'TRF-2026-ABCD1234 — moved 40 AAPL (1 lots, $5,730.80 cost basis) from 7781-2204 to 7781-9930.'
      );
      expect(el().querySelector('p.error')).toBeNull();
    });

    it('disables the button and ignores re-entrant submits while the request is in flight', () => {
      api.transfer.and.returnValue(new Subject<TransferResult>());
      fillValid();

      component.submit();
      fixture.detectChanges();
      expect(component.submitting).toBeTrue();
      expect((el().querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBeTrue();

      component.submit();
      expect(api.transfer).toHaveBeenCalledTimes(1);
    });

    it('clears a previous result and error when resubmitting', () => {
      api.transfer.and.returnValue(new Subject<TransferResult>());
      component.result = success;
      component.error = 'stale';
      fillValid();

      component.submit();

      expect(component.result).toBeNull();
      expect(component.error).toBeNull();
    });

    it('shows the server rejection reason on a 422 with a string body', () => {
      api.transfer.and.returnValue(throwError(() => new HttpErrorResponse({
        status: 422,
        error: 'Cross-advisor transfers require ACATS workflow'
      })));
      fillValid({ toAccount: '7782-0017' });

      component.submit();
      fixture.detectChanges();

      expect(component.error).toBe('Cross-advisor transfers require ACATS workflow');
      expect(text('p.error')).toBe('Cross-advisor transfers require ACATS workflow');
      expect(component.result).toBeNull();
      expect(component.submitting).toBeFalse();
      expect(el().querySelector('.result')).toBeNull();
    });

    it('falls back to a generic message when the error body is not a string', () => {
      api.transfer.and.returnValue(throwError(() => new HttpErrorResponse({
        status: 500,
        error: { message: 'boom' }
      })));
      fillValid();

      component.submit();
      fixture.detectChanges();

      expect(component.error).toBe('Transfer rejected');
      expect(text('p.error')).toBe('Transfer rejected');
      expect(component.submitting).toBeFalse();
    });

    it('submits via the form ngSubmit event', () => {
      api.transfer.and.returnValue(of(success));
      fillValid();

      (el().querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
      fixture.detectChanges();

      expect(api.transfer).toHaveBeenCalledTimes(1);
      expect(component.result).toEqual(success);
    });
  });
});
