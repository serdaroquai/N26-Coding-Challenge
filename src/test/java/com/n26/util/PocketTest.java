package com.n26.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.n26.model.Transaction;

@RunWith(SpringRunner.class)
public class PocketTest {

	@Test
	public void testIndexOf() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		
		int indexNow = Math.toIntExact((now / Pocket.SPAN_MS) % Pocket.COUNT);
		assertEquals(indexNow, Pocket.indexOf(now));
		
	}
	
	@Test
	public void testIndexOfPeriod() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		long previous = instant.minusMillis(Pocket.SPAN_MS*Pocket.COUNT).toEpochMilli();
		
		assertEquals(Pocket.indexOf(now), Pocket.indexOf(previous));
	}
	
	@Test
	public void testIndexOfEdgeValues() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		long spanOffset = now % Pocket.SPAN_MS;
		
		// minimum long that belongs to particular index
		long lowEdge = now - spanOffset;
		assertEquals(Pocket.indexOf(now), Pocket.indexOf(lowEdge));
		assertNotEquals(Pocket.indexOf(now), Pocket.indexOf(lowEdge - 1));
		
		// max long that belongs to same index
		long highEdge = now + (Pocket.SPAN_MS - spanOffset - 1);
		assertEquals(Pocket.indexOf(now), Pocket.indexOf(highEdge));
		assertNotEquals(Pocket.indexOf(now), Pocket.indexOf(highEdge + 1));
	}
	
	@Test
	public void testRangeOf() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		long offset = now % Pocket.SPAN_MS;
		
		long[] edges = Pocket.rangeOf(now);
		assertEquals(now - offset, edges[0]);
		assertEquals(now + (Pocket.SPAN_MS - offset - 1), edges[1]);
		assertEquals(edges[1] - edges[0], Pocket.SPAN_MS -1);
	}
	
	@Test
	public void testRangeOfNoOverlapping() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		long lastInterval[] = Pocket.rangeOf(now);
		
		long previous = instant.minusMillis(Pocket.SPAN_MS).toEpochMilli();
		long prevInterval[] = Pocket.rangeOf(previous); 
		
		assertNotEquals(Arrays.equals(lastInterval, prevInterval), true);
	}
	
	@Test
	public void testRangeOfTightEdges() {
		Instant instant = Instant.now();
		long now = instant.toEpochMilli();
		long lastInterval[] = Pocket.rangeOf(now);
		
		long previous = instant.minusMillis(Pocket.SPAN_MS).toEpochMilli();
		long prevInterval[] = Pocket.rangeOf(previous); 
		
		assertNotEquals(lastInterval[0], prevInterval[1]);
		assertEquals(lastInterval[0]-1, prevInterval[1]);
		
		now = previous;
		lastInterval = prevInterval;
		previous = instant.minusMillis(Pocket.SPAN_MS * 2).toEpochMilli();
		prevInterval = Pocket.rangeOf(previous);
		
		assertNotEquals(lastInterval[0], prevInterval[1]);
		assertEquals(lastInterval[0]-1, prevInterval[1]);
	}
	
	@Test
	public void testPocketImmutability() {
		Pocket prev = Pocket.EMPTY;
		Transaction tx = new Transaction(BigDecimal.ZERO, 0L);
		
		// from should always return a new value;
		Pocket next = Pocket.from(prev, tx);
		assertNotEquals(prev,next);
		
		// since reducer scope has access to fields, you never know ;)
		Pocket q = Pocket.reducer.apply(prev, next);
		assertNotEquals(prev, q);
		assertNotEquals(next, q);
		
	}
	
	@Test
	public void testPocketFromNullPrevious() {
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		Transaction tx = new Transaction(BigDecimal.ONE, now);
		
		Pocket next = Pocket.from(null, tx);
		assertNotNull(next);
		assertEquals(tx.getAmount(), next.getSum());
		assertEquals(1, next.getCount());
		assertEquals(tx.getAmount(), next.getMax());
		assertEquals(tx.getAmount(), next.getMin());
		assertEquals(rangeMin, next.getTimestamp());
	}
	
	@Test
	public void testPocketFrom() {
		Pocket prev = Pocket.EMPTY;
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		long rangeMax = Pocket.rangeOf(now)[1];
		
		Transaction tx = new Transaction(BigDecimal.ONE, rangeMax - 1);
		Transaction tx2 = new Transaction(new BigDecimal("3"), rangeMax);
		
		prev = Pocket.from(prev, tx);
		Pocket next = Pocket.from(prev, tx2);
		
		assertNotEquals(prev, next);
		assertEquals(new BigDecimal("4"), next.getSum());
		assertEquals(2, next.getCount());
		assertEquals(new BigDecimal("3"), next.getMax());
		assertEquals(BigDecimal.ONE, next.getMin());
		assertEquals(rangeMin, next.getTimestamp());
	}
	
	@Test
	public void testPocketFromStale() {
		Pocket prev = Pocket.EMPTY;
		long now = Instant.now().toEpochMilli();
		long rangeMin = Pocket.rangeOf(now)[0];
		long rangeMax = Pocket.rangeOf(now)[1];
		
		Transaction tx = new Transaction(BigDecimal.ONE, rangeMax - (Pocket.SPAN_MS * Pocket.COUNT));
		Transaction tx2 = new Transaction(new BigDecimal("3"), rangeMax);
		
		prev = Pocket.from(prev, tx);
		Pocket next = Pocket.from(prev, tx2);
		
		assertNotEquals(prev, next);
		assertEquals(new BigDecimal("3"), next.getSum());
		assertEquals(1, next.getCount());
		assertEquals(new BigDecimal("3"), next.getMax());
		assertEquals(new BigDecimal("3"), next.getMin());
		assertEquals(rangeMin, next.getTimestamp());
	}
	
	@Test
	public void testPocketReducer() {
		long now = Instant.now().toEpochMilli();
		Pocket p = Pocket.from(Pocket.EMPTY, new Transaction(BigDecimal.ONE, now-1));
		Pocket q = Pocket.from(Pocket.EMPTY, new Transaction(new BigDecimal("2"), now));
		
		Pocket r = Pocket.reducer.apply(p, q);
		
		assertEquals(new BigDecimal("3"), r.getSum());
		assertEquals(2, r.getCount());
		assertEquals(new BigDecimal("2"), r.getMax());
		assertEquals(BigDecimal.ONE, r.getMin());
		assertEquals(Pocket.rangeOf(now-1)[0], r.getTimestamp());
		
	}
	
	@Test
	public void testPocketReducerNullArguments() {
		assertEquals(Pocket.EMPTY, Pocket.reducer.apply(null, null));
		
		Pocket p = Pocket.from(Pocket.EMPTY, new Transaction(BigDecimal.ONE, Instant.now().toEpochMilli()));
		
		Pocket r = Pocket.reducer.apply(p, null);
		assertNotEquals(Pocket.EMPTY, r);
		
		Pocket q = Pocket.reducer.apply(null, p);
		assertNotEquals(Pocket.EMPTY, q);
	}
	
	@Test
	public void testPocketGetAvg() {
		long now = Instant.now().toEpochMilli();
		Transaction tx = new Transaction(new BigDecimal("4"), now);
		Pocket next = Pocket.from(null, tx);
		assertEquals(new BigDecimal("4").setScale(2, RoundingMode.HALF_UP), next.getAvg());
		
		tx = new Transaction(new BigDecimal("8"), now);
		next = Pocket.from(next, tx);
		assertEquals(new BigDecimal("6").setScale(2, RoundingMode.HALF_UP), next.getAvg());
		
		tx = new Transaction(new BigDecimal("6"), now);
		next = Pocket.from(next, tx);
		assertEquals(new BigDecimal("6").setScale(2, RoundingMode.HALF_UP), next.getAvg());
	}
	
	@Test
	public void testPocketGetAvgDivisionByZero() {
		assertEquals(BigDecimal.ZERO, Pocket.EMPTY.getAvg());
	}
	
	
	
}
