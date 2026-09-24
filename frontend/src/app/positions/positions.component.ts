import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { PositionView } from '../shared/models';
import { NgFor, NgIf } from '@angular/common';
import { LotBadgeComponent } from '../shared/lot-badge.component';
import { MoneyPipe } from '../shared/money.pipe';

@Component({
  selector: 'app-positions',
  templateUrl: './positions.component.html',
  standalone: true,
  imports: [NgFor, NgIf, LotBadgeComponent, MoneyPipe]
})
export class PositionsComponent implements OnInit, OnDestroy {
  accountNumber = '';
  positions: PositionView[] = [];
  loading = true;
  error: string | null = null;
  private sub?: Subscription;

  constructor(private route: ActivatedRoute, private api: AdvisorApiService) {}

  ngOnInit(): void {
    this.sub = this.route.paramMap
      .pipe(switchMap(params => {
        this.accountNumber = params.get('accountNumber') || '';
        this.loading = true;
        this.error = null;
        return this.api.positions(this.accountNumber);
      }))
      .subscribe({
        next: positions => { this.positions = positions; this.loading = false; },
        error: err => { this.error = err.status === 404 ? 'Account not found' : 'Service unavailable'; this.loading = false; }
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get totalCostBasis(): number {
    return this.positions.reduce((sum, p) => sum + p.costBasis, 0);
  }
}
