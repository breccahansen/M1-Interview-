import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { AdvisorApiService } from './advisor-api.service';
import { PositionView, TransferRequest, TransferResult } from './models';

describe('AdvisorApiService', () => {
  let service: AdvisorApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AdvisorApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('positions', () => {
    it('GETs /api/v1/accounts/{acct}/positions and returns the body', () => {
      const body: PositionView[] = [
        { symbol: 'AAPL', quantity: 133, costBasis: 19974.29, averageUnitCost: 150.1826, lots: 2 }
      ];
      let received: PositionView[] | undefined;

      service.positions('7781-2204').subscribe(p => (received = p));

      const req = http.expectOne('/api/v1/accounts/7781-2204/positions');
      expect(req.request.method).toBe('GET');
      req.flush(body);

      expect(received).toEqual(body);
    });

    it('propagates a 404 as an HttpErrorResponse with status 404', () => {
      let error: HttpErrorResponse | undefined;

      service.positions('0000-0000').subscribe({ error: e => (error = e) });

      http.expectOne('/api/v1/accounts/0000-0000/positions')
        .flush('not found', { status: 404, statusText: 'Not Found' });

      expect(error).toBeInstanceOf(HttpErrorResponse);
      expect(error!.status).toBe(404);
    });
  });

  describe('transfer', () => {
    const request: TransferRequest = {
      fromAccount: '7781-2204',
      toAccount: '7781-9930',
      symbol: 'AAPL',
      quantity: 40,
      method: 'FIFO'
    };

    it('POSTs the request body to /api/v1/transfers and returns the result', () => {
      const result: TransferResult = {
        transferId: 'TRF-2026-ABCD1234',
        fromAccount: '7781-2204',
        toAccount: '7781-9930',
        symbol: 'AAPL',
        quantity: 40,
        costBasisMoved: 5730.8,
        lotsMoved: 1
      };
      let received: TransferResult | undefined;

      service.transfer(request).subscribe(r => (received = r));

      const req = http.expectOne('/api/v1/transfers');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(result, { status: 201, statusText: 'Created' });

      expect(received).toEqual(result);
    });

    it('surfaces the 422 rejection reason from the response body', () => {
      let error: HttpErrorResponse | undefined;

      service.transfer(request).subscribe({ error: e => (error = e) });

      http.expectOne('/api/v1/transfers')
        .flush('Cross-advisor transfers require ACATS workflow', { status: 422, statusText: 'Unprocessable Entity' });

      expect(error!.status).toBe(422);
      expect(error!.error).toBe('Cross-advisor transfers require ACATS workflow');
    });
  });
});
