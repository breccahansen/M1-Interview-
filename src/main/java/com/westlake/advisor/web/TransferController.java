package com.westlake.advisor.web;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.westlake.advisor.costbasis.InsufficientSharesException;
import com.westlake.advisor.transfer.AccountNotFoundException;
import com.westlake.advisor.transfer.TransferNotAllowedException;
import com.westlake.advisor.transfer.TransferRequest;
import com.westlake.advisor.transfer.TransferResult;
import com.westlake.advisor.transfer.TransferService;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    public ResponseEntity<TransferResult> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transfers.transfer(request));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> notFound(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler({TransferNotAllowedException.class, InsufficientSharesException.class})
    public ResponseEntity<String> notAllowed(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
    }
}
