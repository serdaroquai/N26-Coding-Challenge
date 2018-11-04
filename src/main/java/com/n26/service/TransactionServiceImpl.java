package com.n26.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.n26.aspect.KeepStatistics;
import com.n26.exception.StaleTransactionException;
import com.n26.exception.UnProcessableEntityException;
import com.n26.model.Transaction;

@Service
@KeepStatistics
public class TransactionServiceImpl implements TransactionService{

	@Value("${transactionService.stale-after-seconds:60}") private long staleAfterSeconds;

	public void deleteTransactions() {
		// delete all tx
	}

	protected void addTransactionAtInstant(Transaction tx, Instant instant) 
			throws StaleTransactionException, UnProcessableEntityException {
		
		if (tx.getTimestamp() - instant.toEpochMilli() > 0)
			throw new UnProcessableEntityException();
		
		if (tx.getTimestamp() - instant.minusSeconds(staleAfterSeconds).toEpochMilli() < 0)
			throw new StaleTransactionException();
		
		// persist tx
	}
	
	public void addTransaction(Transaction tx) throws StaleTransactionException, UnProcessableEntityException {
		addTransactionAtInstant(tx, Instant.now());
	}
	
	public long getStaleAfterSeconds() {
		return staleAfterSeconds;
	}
	
}
