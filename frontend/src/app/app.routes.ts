import { Routes } from '@angular/router';
import { PositionsComponent } from './positions/positions.component';
import { TransferComponent } from './transfer/transfer.component';

export const routes: Routes = [
  { path: '', redirectTo: 'accounts/7781-2204/positions', pathMatch: 'full' },
  { path: 'accounts/:accountNumber/positions', component: PositionsComponent },
  { path: 'transfers/new', component: TransferComponent }
];
