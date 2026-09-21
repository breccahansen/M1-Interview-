package com.westlake.advisor.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.westlake.advisor.account.Account;
import com.westlake.advisor.account.AccountRepository;
import com.westlake.advisor.account.Position;
import com.westlake.advisor.costbasis.CostBasisCalculator;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accounts;
    private final CostBasisCalculator costBasis;

    public AccountController(AccountRepository accounts, CostBasisCalculator costBasis) {
        this.accounts = accounts;
        this.costBasis = costBasis;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getAccount(@PathVariable String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .map(a -> ResponseEntity.ok(toView(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountNumber}/positions")
    public ResponseEntity<List<Map<String, Object>>> getPositions(@PathVariable String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .map(a -> ResponseEntity.ok(positionsView(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toView(Account a) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("accountNumber", a.getAccountNumber());
        view.put("advisorId", a.getAdvisorId());
        view.put("type", a.getType());
        view.put("status", a.getStatus());
        view.put("positions", positionsView(a));
        return view;
    }

    private List<Map<String, Object>> positionsView(Account a) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Position p : a.getPositions()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("symbol", p.getSymbol());
            v.put("quantity", p.getQuantity());
            BigDecimal basis = costBasis.totalCostBasis(p);
            v.put("costBasis", basis);
            v.put("averageUnitCost", costBasis.averageUnitCost(p));
            v.put("lots", p.getLots().size());
            out.add(v);
        }
        return out;
    }
}
