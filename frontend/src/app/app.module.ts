import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { PositionsComponent } from './positions/positions.component';
import { TransferComponent } from './transfer/transfer.component';
import { MoneyPipe } from './shared/money.pipe';
import { LotBadgeComponent } from './shared/lot-badge.component';

@NgModule({
  declarations: [
    AppComponent,
    PositionsComponent,
    TransferComponent,
    MoneyPipe,
    LotBadgeComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
