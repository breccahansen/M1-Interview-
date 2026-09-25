package com.westlake.advisor.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.westlake.advisor.account.AccountRepository;
import com.westlake.advisor.account.AccountStatus;
import com.westlake.advisor.account.AccountType;
import com.westlake.advisor.costbasis.CostBasisCalculator;

class AccountControllerTest {

    private final AccountController controller = new AccountController(new AccountRepository(), new CostBasisCalculator());

    @Test
    void getAccountReturnsViewWithPositions() {
        ResponseEntity<Map<String, Object>> response = controller.getAccount("7781-2204");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> view = response.getBody();
        assertEquals(Arrays.asList("accountNumber", "advisorId", "type", "status", "positions"),
                Arrays.asList(view.keySet().toArray()));
        assertEquals("7781-2204", view.get("accountNumber"));
        assertEquals("ADV-114", view.get("advisorId"));
        assertEquals(AccountType.INDIVIDUAL_BROKERAGE, view.get("type"));
        assertEquals(AccountStatus.OPEN, view.get("status"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> positions = (List<Map<String, Object>>) view.get("positions");
        assertEquals(3, positions.size());
    }

    @Test
    void getPositionsReturnsQuantityBasisAverageAndLotCount() {
        ResponseEntity<List<Map<String, Object>>> response = controller.getPositions("7781-2204");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> positions = response.getBody();
        assertEquals(3, positions.size());

        Map<String, Object> aapl = positions.get(0);
        assertEquals("AAPL", aapl.get("symbol"));
        assertEquals(new BigDecimal("133"), aapl.get("quantity"));
        assertEquals(new BigDecimal("19974.29"), aapl.get("costBasis"));
        // 19974.29 / 133 = 150.1826...
        assertEquals(new BigDecimal("150.1826"), aapl.get("averageUnitCost"));
        assertEquals(2, aapl.get("lots"));

        Map<String, Object> vea = positions.get(2);
        assertEquals("VEA", vea.get("symbol"));
        assertEquals(new BigDecimal("1654.46"), vea.get("costBasis"));
        assertEquals(new BigDecimal("50.1350"), vea.get("averageUnitCost"));
        assertEquals(1, vea.get("lots"));
    }

    @Test
    void emptyAccountHasNoPositions() {
        ResponseEntity<List<Map<String, Object>>> response = controller.getPositions("7782-0017");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void unknownAccountIs404() {
        ResponseEntity<Map<String, Object>> account = controller.getAccount("0000-0000");
        assertEquals(HttpStatus.NOT_FOUND, account.getStatusCode());
        assertNull(account.getBody());

        ResponseEntity<List<Map<String, Object>>> positions = controller.getPositions("0000-0000");
        assertEquals(HttpStatus.NOT_FOUND, positions.getStatusCode());
        assertNull(positions.getBody());
    }
}
