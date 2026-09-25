package com.westlake.advisor.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.westlake.advisor.account.AccountRepository;
import com.westlake.advisor.costbasis.CostBasisCalculator;
import com.westlake.advisor.costbasis.CostBasisMethod;
import com.westlake.advisor.costbasis.InsufficientSharesException;
import com.westlake.advisor.notification.AdvisorNotifier;
import com.westlake.advisor.transfer.AccountNotFoundException;
import com.westlake.advisor.transfer.TransferNotAllowedException;
import com.westlake.advisor.transfer.TransferRequest;
import com.westlake.advisor.transfer.TransferResult;
import com.westlake.advisor.transfer.TransferService;

class TransferControllerTest {

    private final TransferController controller = new TransferController(
            new TransferService(new AccountRepository(), new CostBasisCalculator(), new AdvisorNotifier()));

    private TransferRequest request(String from, String to, String symbol, String qty) {
        TransferRequest r = new TransferRequest();
        r.setFromAccount(from);
        r.setToAccount(to);
        r.setSymbol(symbol);
        r.setQuantity(new BigDecimal(qty));
        r.setMethod(CostBasisMethod.FIFO);
        return r;
    }

    @Test
    void successfulTransferReturns201WithResult() {
        ResponseEntity<TransferResult> response =
                controller.transfer(request("7781-2204", "7781-9930", "AAPL", "10"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        TransferResult body = response.getBody();
        assertEquals("AAPL", body.getSymbol());
        assertEquals(new BigDecimal("10"), body.getQuantity());
        assertEquals(new BigDecimal("1432.70"), body.getCostBasisTransferred());
        assertEquals(Arrays.asList("L1-T"), body.getLotIds());
    }

    @Test
    void serviceExceptionsPropagateToHandlers() {
        AccountNotFoundException nf = assertThrows(AccountNotFoundException.class,
                () -> controller.transfer(request("0000-0000", "7781-9930", "AAPL", "1")));
        assertEquals("Account not found: 0000-0000", nf.getMessage());

        TransferNotAllowedException na = assertThrows(TransferNotAllowedException.class,
                () -> controller.transfer(request("7781-2204", "7782-0017", "AAPL", "1")));
        assertEquals("Cross-advisor transfers require ACATS workflow", na.getMessage());
    }

    @Test
    void notFoundHandlerMaps404() {
        ResponseEntity<String> response = controller.notFound(new AccountNotFoundException("1234-5678"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Account not found: 1234-5678", response.getBody());
    }

    @Test
    void notAllowedHandlerMaps422ForBothRejectionTypes() {
        ResponseEntity<String> r1 = controller.notAllowed(new TransferNotAllowedException("Source account X is CLOSED"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, r1.getStatusCode());
        assertEquals("Source account X is CLOSED", r1.getBody());

        ResponseEntity<String> r2 = controller.notAllowed(
                new InsufficientSharesException("AAPL", new BigDecimal("500"), new BigDecimal("133")));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, r2.getStatusCode());
        assertEquals("Insufficient shares of AAPL: requested 500, available 133", r2.getBody());
    }
}
