package com.n26.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BinaryOperator;

import com.n26.model.Transaction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Immutable since AtomicReference uses object reference comparison
 */
@AllArgsConstructor(access=AccessLevel.PRIVATE)
@NoArgsConstructor(access=AccessLevel.PRIVATE)
@ToString(of= {"sum","count","timestamp"})
public class Pocket {
	
	public static final long SPAN_MS = 10;
	public static final int COUNT = 6000;
	public static final Pocket EMPTY = new Pocket();
	
	private @Getter BigDecimal sum = BigDecimal.ZERO;
	private @Getter long count = 0L;
	private @Getter BigDecimal max = BigDecimal.ZERO;
	private @Getter BigDecimal min = BigDecimal.ZERO;
	private @Getter long timestamp = 0L;
	
	public static final BinaryOperator<Pocket> reducer = (prev, next) -> {
		
		if (prev == null) prev = Pocket.EMPTY;
		if (next == null) return prev;
		
		return new Pocket(
			prev.getSum().add(next.getSum()),
			prev.getCount() + next.getCount(),
			prev.getMax().compareTo(next.getMax()) == 1 ? prev.getMax() : next.getMax(),
			(prev.getCount() == 0 || prev.getMin().compareTo(next.getMin()) ==1) ? next.getMin() : prev.getMin(),
			prev.getTimestamp() < next.getTimestamp() ? prev.getTimestamp() : next.getTimestamp());
	};
	
	public static Pocket from(Pocket prev, Transaction tx) {
		Pocket pocket = new Pocket();
		
		if (prev == null || prev == Pocket.EMPTY || (tx.getTimestamp() - prev.timestamp >= SPAN_MS)) {
			pocket.timestamp = rangeOf(tx.getTimestamp())[0];
			pocket.sum = tx.getAmount();
			pocket.count = 1;
			pocket.max = tx.getAmount();
			pocket.min = tx.getAmount();
		} else {
			pocket.timestamp = prev.timestamp;
			pocket.sum = prev.sum.add(tx.getAmount());
			pocket.count = prev.count + 1;
			pocket.max = tx.getAmount().compareTo(prev.max) == 1 ? tx.getAmount(): prev.max;
			pocket.min = (prev.getCount() == 0 || prev.getMin().compareTo(tx.getAmount()) == 1) ? tx.getAmount() : prev.getMin();
		}

		return pocket;
	}

	public BigDecimal getAvg() {
		return (count == 0) ? BigDecimal.ZERO : sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
	}
	
	public static int indexOf(long millis) {
		return Math.toIntExact((millis / SPAN_MS) % COUNT);
	}
	
	public static long[] rangeOf(long millis) {
		long offset = millis % SPAN_MS;
		return new long[] {millis - offset, millis + (SPAN_MS - offset - 1)};
	}
	
	
}
