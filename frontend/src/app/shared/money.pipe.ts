import { Pipe, PipeTransform } from '@angular/core';

/** Formats a number as USD with exactly two decimals; blank for null/undefined. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, showSign = false): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '';
    }
    const abs = Math.abs(value).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    const sign = value < 0 ? '-' : showSign ? '+' : '';
    return `${sign}$${abs}`;
  }
}
