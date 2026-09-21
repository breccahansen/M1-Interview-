package com.westlake.advisor.transfer;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.westlake.advisor.costbasis.CostBasisMethod;

public class TransferRequest {

    @NotBlank
    private String fromAccount;

    @NotBlank
    private String toAccount;

    @NotBlank
    private String symbol;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    private CostBasisMethod method = CostBasisMethod.FIFO;

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public CostBasisMethod getMethod() {
        return method;
    }

    public void setMethod(CostBasisMethod method) {
        this.method = method;
    }
}
