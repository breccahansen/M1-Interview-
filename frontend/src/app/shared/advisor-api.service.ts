import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PositionView, TransferRequest, TransferResult } from './models';

@Injectable({ providedIn: 'root' })
export class AdvisorApiService {
  private readonly base = '/api/v1';

  constructor(private http: HttpClient) {}

  positions(accountNumber: string): Observable<PositionView[]> {
    return this.http.get<PositionView[]>(`${this.base}/accounts/${accountNumber}/positions`);
  }

  transfer(request: TransferRequest): Observable<TransferResult> {
    return this.http.post<TransferResult>(`${this.base}/transfers`, request);
  }
}
