package com.n26.model;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class Transaction {

	private @Getter @NonNull BigDecimal amount;
	private @Getter long timestamp;
	
	public Transaction(@JsonProperty("amount") BigDecimal amount, @JsonProperty("timestamp") Date date)	{
		this.amount = amount;
		this.timestamp = date.getTime();
	}

}
