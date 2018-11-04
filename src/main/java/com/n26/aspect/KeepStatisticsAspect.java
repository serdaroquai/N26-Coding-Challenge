package com.n26.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.n26.model.Transaction;
import com.n26.service.StatisticsService;

@Component
@Aspect
public class KeepStatisticsAspect {

	@Autowired private StatisticsService statisticsService;
	
	@Around("classAnnotated()")
	public Object keepStatistics(ProceedingJoinPoint pjp) throws Throwable{
		
		Object result = pjp.proceed();
		String methodName = pjp.getSignature().getName();
		
		if ("addTransaction".equals(methodName)) {
			statisticsService.register((Transaction) pjp.getArgs()[0]);
		} else if ("deleteTransactions".equals(methodName)) {
			statisticsService.clearStatistics();
		}
		
		return result;
		
	}
	
	@Pointcut("within(@com.n26.aspect.KeepStatistics *)")
	public void classAnnotated() {}
	
}
