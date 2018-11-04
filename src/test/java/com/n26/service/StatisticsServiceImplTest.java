package com.n26.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import com.n26.model.Transaction;
import com.n26.util.Pocket;

@RunWith(SpringRunner.class)
public class StatisticsServiceImplTest {
	
	private static final int poolSize = 16;

	@Autowired StatisticsService service;
	
	ExecutorService executor;
	
	@TestConfiguration
    static class StatisticsServiceTestContextConfiguration {
  
        @Bean
        public StatisticsService statisticsService() {
            return new StatisticsServiceImpl();
        }
    }
	
	@Before
	public void setup() {
		service.clearStatistics();
		executor = Executors.newFixedThreadPool(poolSize);
	}
	
	@After
	public void destroy() {
		executor.shutdown();
	}
	
	@Test
    public void testConcurrentSumFullSpan() throws InterruptedException {
		
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		
		//send a full count*span of transactions
		//for ex: 10 * 100
		int txCount = Math.toIntExact(Pocket.SPAN_MS * Pocket.COUNT);

		List<Callable<Void>> callables = new ArrayList<>();
		
		for (int i=0; i < txCount; i++) {
			Transaction tx = new Transaction(BigDecimal.ONE, rangeMin + i);
			callables.add(() -> {
				service.register(tx);
				return null;
			});
		}
		
		executor.invokeAll(callables);
		
		//read a ms before oldest pocket goes stale
		//expect to see 10 * 100
		BigDecimal sum = ((StatisticsServiceImpl) service).getStatistics(rangeMin + txCount - 1).getSum();
		assertEquals(0, sum.compareTo(new BigDecimal(txCount)));
		
    }
	
	@Test
    public void testConcurrentSumDiscardOldestPocketDuringRead() throws InterruptedException {
        
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		
		//send a full count*span of transactions
		//for ex: 10 * 100
		int txCount = Math.toIntExact(Pocket.SPAN_MS * Pocket.COUNT);
		int acceptedTxCount = txCount - Math.toIntExact(Pocket.SPAN_MS);

		List<Callable<Void>> callables = new ArrayList<>();
		
		for (int i=0; i < txCount; i++) {
			Transaction tx = new Transaction(BigDecimal.ONE, rangeMin + i);
			callables.add(() -> {
				service.register(tx);
				return null;
			});
		}
		
		executor.invokeAll(callables);
		
		//read when the oldest pocket goes stale (array rolls over and discards oldest pocket during read)
		//expect to see 9 * 100 (last pocket is discarded)
		BigDecimal sum = ((StatisticsServiceImpl) service).getStatistics(rangeMin + txCount).getSum();
		assertEquals(0, sum.compareTo(new BigDecimal(acceptedTxCount)));
		
    }
	
	@Test
    public void testConcurrentSumDiscardOldestPocketWhenOverflow() throws InterruptedException {
        
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		
		//send a full count*span of transactions
		//for ex: 10 * 100
		int txCount = Math.toIntExact(Pocket.SPAN_MS * Pocket.COUNT);
		int acceptedTxCount = txCount - Math.toIntExact(Pocket.SPAN_MS) + 1;

		List<Callable<Void>> callables = new ArrayList<>();
		
		for (int i=0; i < txCount; i++) {
			Transaction tx = new Transaction(BigDecimal.ONE, rangeMin + i);
			callables.add(() -> {
				service.register(tx);
				return null;
			});
		}
		
		executor.invokeAll(callables);
		callables.clear();
		
		// now send one more transaction to roll-over
		// reason we send this tx separate is that we do not let tx's in future.
		Transaction tx = new Transaction(BigDecimal.ONE, rangeMin + txCount);
		callables.add(() -> {
			service.register(tx);
			return null;
		});
		executor.invokeAll(callables);
		
		//array rolls over and new pocket to work on is cleared, has only latest tx inside
		//expect to see 9 * 100 + 1
		BigDecimal sum = ((StatisticsServiceImpl) service).getStatistics(rangeMin + txCount).getSum();
		assertEquals(0, sum.compareTo(new BigDecimal(acceptedTxCount)));
		
    }
	
}
