import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdvisorApiService } from '../shared/advisor-api.service';
import { TransferRequest, TransferResult } from '../shared/models';
import { MoneyPipe } from '../shared/money.pipe';

@Component({
  selector: 'app-transfer',
  templateUrl: './transfer.component.html',
  standalone: true,
  imports: [ReactiveFormsModule, MoneyPipe]
})
export class TransferComponent {
  readonly methods = ['FIFO', 'LIFO', 'HIGH_COST', 'AVERAGE_COST'];
  form = this.fb.group({
    fromAccount: ['7781-2204', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]],
    toAccount: ['7781-9930', [Validators.required, Validators.pattern(/^\d{4}-\d{4}$/)]],
    symbol: ['', [Validators.required, Validators.maxLength(6)]],
    quantity: [null, [Validators.required, Validators.min(1)]],
    method: ['FIFO', Validators.required]
  });
  result: TransferResult | null = null;
  error: string | null = null;
  submitting = false;

  constructor(private fb: FormBuilder, private api: AdvisorApiService) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.value;
    if (value.fromAccount === value.toAccount) {
      this.error = 'From and to accounts must differ';
      return;
    }
    this.submitting = true;
    this.error = null;
    this.result = null;
    const request: TransferRequest = {
      fromAccount: value.fromAccount!,
      toAccount: value.toAccount!,
      symbol: value.symbol!.toUpperCase(),
      quantity: Number(value.quantity),
      method: value.method as TransferRequest['method']
    };
    this.api.transfer(request).subscribe({
      next: r => { this.result = r; this.submitting = false; },
      error: err => { this.error = typeof err.error === 'string' ? err.error : 'Transfer rejected'; this.submitting = false; }
    });
  }
}
