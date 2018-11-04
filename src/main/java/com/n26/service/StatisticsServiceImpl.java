package com.n26.service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.n26.model.Statistics;
import com.n26.model.Transaction;
import com.n26.util.Pocket;

@Service
public class StatisticsServiceImpl implements StatisticsService{

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private AtomicReferenceArray<Pocket> pockets;
	
	@PostConstruct
	private void postConstruct() {
		logger.info(String.format("Initializing with pocket span of %s ms", Pocket.SPAN_MS));
		logger.info(String.format("Initializing with pocket size of %s", Pocket.COUNT));
		initialize();
	}
	
	private void initialize() {
		pockets = new AtomicReferenceArray<>(Pocket.COUNT);
	}
	
	public void register(Transaction tx) {
		int index = Pocket.indexOf(tx.getTimestamp());
		pockets.getAndUpdate(index, prev -> Pocket.from(prev, tx));
	}

	protected Statistics getStatistics(long now) {
		Pocket pocket = IntStream.range(0, Pocket.COUNT)
			.mapToObj(pockets::get)
			.filter(Objects::nonNull)
			.filter(p -> now - p.getTimestamp() < Pocket.SPAN_MS * Pocket.COUNT)
			.reduce(Pocket.EMPTY, Pocket.reducer);
		
		return Statistics.from(pocket);
	}
	
	public Statistics getStatistics() {
		return getStatistics(Instant.now().toEpochMilli());
	}

	public synchronized void clearStatistics() {
		initialize();
	}

}
