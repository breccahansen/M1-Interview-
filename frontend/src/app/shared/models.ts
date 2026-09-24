export interface PositionView {
  symbol: string;
  quantity: number;
  costBasis: number;
  averageUnitCost: number;
  lots: number;
}

export interface TransferRequest {
  fromAccount: string;
  toAccount: string;
  symbol: string;
  quantity: number;
  method: 'FIFO' | 'LIFO' | 'HIGH_COST' | 'AVERAGE_COST';
}

export interface TransferResult {
  transferId: string;
  fromAccount: string;
  toAccount: string;
  symbol: string;
  quantity: number;
  costBasisMoved: number;
  lotsMoved: number;
}
