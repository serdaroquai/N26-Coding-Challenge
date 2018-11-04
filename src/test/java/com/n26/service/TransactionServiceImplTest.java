package com.n26.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import com.n26.exception.StaleTransactionException;
import com.n26.exception.UnProcessableEntityException;
import com.n26.model.Transaction;

@RunWith(SpringRunner.class)
public class TransactionServiceImplTest {

	@Autowired TransactionService transactionService;
	
	@TestConfiguration
    static class TransactionServiceTestContextConfiguration {
  
        @Bean
        public TransactionServiceImpl transactionService() {
            return new TransactionServiceImpl();
        }
    }
	
	@Test
	public void testAddStaleTransaction() throws Exception {
		TransactionServiceImpl impl = (TransactionServiceImpl) transactionService;
		
		Instant now = Instant.now();
		Instant notStale = now.minusSeconds(impl.getStaleAfterSeconds());
		Instant stale = now.minusSeconds(impl.getStaleAfterSeconds()).minusMillis(1);
		
		// expect no exception
		Transaction tx = new Transaction(BigDecimal.ONE, notStale.toEpochMilli());
		transactionService.addTransaction(tx);
		
		// expect exception
		Transaction tx2 = new Transaction(BigDecimal.ONE, stale.toEpochMilli());
		assertThatExceptionOfType(StaleTransactionException.class)
			.isThrownBy(() -> ((TransactionServiceImpl) transactionService).addTransactionAtInstant(tx2, now));
	}
	
	@Test
	public void testAddFutureTransaction() throws Exception {
		Instant now = Instant.now();
		Instant future = now.plusMillis(1);
				
		// expect no exception
		Transaction tx = new Transaction(BigDecimal.ONE, now.toEpochMilli());
		transactionService.addTransaction(tx);
		
		// expect exception
		Transaction tx2 = new Transaction(BigDecimal.ONE, future.toEpochMilli());
		assertThatExceptionOfType(UnProcessableEntityException.class)
			.isThrownBy(() -> ((TransactionServiceImpl) transactionService).addTransactionAtInstant(tx2, now));
	}
}
