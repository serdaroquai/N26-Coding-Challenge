package com.n26.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.n26.exception.StaleTransactionException;
import com.n26.exception.UnProcessableEntityException;
import com.n26.model.Transaction;
import com.n26.service.TransactionService;

@RestController
public class TransactionController {

	@Autowired
	private TransactionService transactionService;
	
	@PostMapping("/transactions")
	public ResponseEntity<Void> add(@RequestBody Transaction tx) throws StaleTransactionException, UnProcessableEntityException {
		transactionService.addTransaction(tx);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
	
	@DeleteMapping("/transactions")
	public ResponseEntity<Void> delete() {
		transactionService.deleteTransactions();
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@ExceptionHandler(InvalidFormatException.class)
	public ResponseEntity<Void> handleJacksonMappingError(InvalidFormatException ex) {
		return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
	}
}
