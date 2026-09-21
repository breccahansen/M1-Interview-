package com.westlake.advisor.transfer;

import java.math.BigDecimal;
import java.util.List;

public class TransferResult {

    private final String transferId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal costBasisTransferred;
    private final List<String> lotIds;

    public TransferResult(String transferId, String symbol, BigDecimal quantity,
                          BigDecimal costBasisTransferred, List<String> lotIds) {
        this.transferId = transferId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.costBasisTransferred = costBasisTransferred;
        this.lotIds = lotIds;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getCostBasisTransferred() {
        return costBasisTransferred;
    }

    public List<String> getLotIds() {
        return lotIds;
    }
}
