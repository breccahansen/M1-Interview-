import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
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

  it('GETs positions for an account', () => {
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

  it('propagates HTTP errors from positions', () => {
    let status: number | undefined;
    service.positions('0000-0000').subscribe({ error: e => (status = e.status) });

    http.expectOne('/api/v1/accounts/0000-0000/positions')
      .flush('Account not found: 0000-0000', { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });

  it('POSTs a transfer request and returns the result', () => {
    const request: TransferRequest = {
      fromAccount: '7781-2204', toAccount: '7781-9930', symbol: 'AAPL', quantity: 10, method: 'FIFO'
    };
    const result: TransferResult = {
      transferId: 'TRF-2026-ABCD1234', fromAccount: '7781-2204', toAccount: '7781-9930',
      symbol: 'AAPL', quantity: 10, costBasisMoved: 1432.7, lotsMoved: 1
    };
    let received: TransferResult | undefined;
    service.transfer(request).subscribe(r => (received = r));

    const req = http.expectOne('/api/v1/transfers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(result);

    expect(received).toEqual(result);
  });
});
