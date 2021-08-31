package ogallagher.marketsense.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Some unorganized datetime/calendar manipulation utilities.
 * 
 * @author Owen Gallagher
 * @since 2021-08-30
 */
public class DatetimeUtils {
	// unit conversion constants
	public static final int SECS_PER_MIN = 60;
	public static final int MINS_PER_HR = 60;
	public static final int SECS_PER_HR = SECS_PER_MIN * MINS_PER_HR;
	
	/**
	 * @param datetime Base datetime.
	 * 
	 * @return A datetime pushed an integer count of days to the next Monday, so as to avoid the weekend. If the input datetime
	 * is already within Mon-Fri, the datetime is returned unchanged.
	 */
	public static LocalDateTime forwardFromWeekend(LocalDateTime datetime) {
		DayOfWeek day = datetime.getDayOfWeek();
		
		if (day.equals(DayOfWeek.SUNDAY)) {
			return datetime.plusDays(1);
		}
		else if (day.equals(DayOfWeek.SATURDAY)) {
			return datetime.plusDays(2);
		}
		else {
			return datetime;
		}
	}
	
	/**
	 * @param datetime Base datetime.
	 * 
	 * @return A datetime pushed an integer count of days to the prior Friday, so as to avoid the weekend. If the input datetime
	 * is already within Mon-Fri, the datetime is returned unchanged.
	 */
	public static LocalDateTime backwardFromWeekend(LocalDateTime datetime) {
		DayOfWeek day = datetime.getDayOfWeek();
		
		if (day.equals(DayOfWeek.SUNDAY)) {
			return datetime.plusDays(-2);
		}
		else if (day.equals(DayOfWeek.SATURDAY)) {
			return datetime.plusDays(-1);
		}
		else {
			return datetime;
		}
	}
}
