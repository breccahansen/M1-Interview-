package com.westlake.advisor.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {

    private final String accountNumber;
    private final String advisorId;
    private final AccountType type;
    private final List<Position> positions = new ArrayList<>();
    private AccountStatus status = AccountStatus.OPEN;

    public Account(String accountNumber, String advisorId, AccountType type) {
        this.accountNumber = accountNumber;
        this.advisorId = advisorId;
        this.type = type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAdvisorId() {
        return advisorId;
    }

    public AccountType getType() {
        return type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public List<Position> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public void addPosition(Position position) {
        positions.add(position);
    }

    public Position findPosition(String symbol) {
        for (Position p : positions) {
            if (p.getSymbol().equalsIgnoreCase(symbol)) {
                return p;
            }
        }
        return null;
    }

    public void removePosition(Position position) {
        positions.remove(position);
    }
}
