package ogallagher.marketsense.test;

import java.time.LocalDateTime;

import ogallagher.marketsense.util.DatetimeUtils;
import ogallagher.temp_fx_logger.System;

public class TestDatetimeUtils extends Test {
	static {
		name = "datetimeutils";
	}
	
	@Override
	public void evaluate(boolean persistDummies) {
		System.out.println("DEBUG testing " + name);
		
		LocalDateTime datetime = LocalDateTime.now();
		for (int i=0; i<3; i++) {
			System.out.println("DEBUG base = " + datetime + " = " + datetime.getDayOfWeek());
			System.out.println("DEBUG\t next weekday = " + DatetimeUtils.forwardFromWeekend(datetime));
			System.out.println("DEBUG\t last weekday = " + DatetimeUtils.backwardFromWeekend(datetime));
			
			datetime = datetime.plusDays(3);
		}
		
		System.out.println("DEBUG " + name + " testing complete");
	}
}
