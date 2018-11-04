package com.n26.service;

import com.n26.exception.StaleTransactionException;
import com.n26.exception.UnProcessableEntityException;
import com.n26.model.Transaction;

public interface TransactionService {
	
	void addTransaction(Transaction tx) throws StaleTransactionException, UnProcessableEntityException;
	void deleteTransactions();
}
