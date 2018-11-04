package com.n26.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.n26.util.BigDecimalSerializer;
import com.n26.util.Pocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class Statistics {

	@JsonSerialize(using = BigDecimalSerializer.class)
	private @Getter BigDecimal sum;
	@JsonSerialize(using = BigDecimalSerializer.class)
	private @Getter BigDecimal avg;
	@JsonSerialize(using = BigDecimalSerializer.class)
	private @Getter BigDecimal max;
	@JsonSerialize(using = BigDecimalSerializer.class)
	private @Getter BigDecimal min;
	private @Getter long count;
	
	public static Statistics from(Pocket p) {
		return new Statistics(p.getSum(), p.getAvg(), p.getMax(), p.getMin(), p.getCount());
	}
}
