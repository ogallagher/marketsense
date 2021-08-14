package ogallagher.marketsense.persistent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQuery;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.*;

import ogallagher.twelvedata_client_java.TwelvedataInterface.TimeSeries;

/**
 * Security trade bar, with OHLC data at a given time.
 * 
 * @author Owen Gallagher
 *
 */
@Entity
public class TradeBar implements Comparable<TradeBar> {
	/**
	 * Name of table in database.
	 */
	public static final String DB_TABLE = "TradeBar";
	
	public static final String DB_COMPCOL_ID = "id";
	@EmbeddedId
	private TradeBarId id;
	
	public static final String DB_COL_OPEN = "open";
	float open;
	public static final String DB_COL_HIGH = "high";
	float high;
	public static final String DB_COL_LOW = "low";
	float low;
	public static final String DB_COL_CLOSE = "close";
	float close;
	
	/**
	 * Convert bars in a {@link TimeSeries} to {@code TradeBar} instances in a sorted list.
	 * 
	 * @param timeSeries
	 * @param order
	 * @return
	 */
	public static List<TradeBar> convertTimeSeries(TimeSeries timeSeries, Comparator<TradeBar> order) {
		DateTimeFormatter dtFormat;
		boolean isDatetime = false;
		
		String dt0 = timeSeries.values.get(0).datetime;
		try {
			LocalDate.parse(dt0, DateTimeFormatter.ISO_LOCAL_DATE);
			dtFormat = DateTimeFormatter.ISO_LOCAL_DATE;
			isDatetime = false;
		}
		catch (DateTimeParseException e1) {
			try {
				LocalDateTime.parse(dt0, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				dtFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
				isDatetime = true;
			}
			catch (DateTimeParseException e2) {
				System.out.println("failed to parse date/datetime of format " + dt0);
				return null;
			}
		}
		
		LinkedList<TradeBar> bars = new LinkedList<>();
		
		for (TimeSeries.TradeBar bar : timeSeries.values) {
			LocalDateTime dt;
			if (isDatetime) {
				dt = LocalDateTime.parse(bar.datetime, dtFormat);
			}
			else {
				dt = LocalDateTime.of(LocalDate.parse(bar.datetime, dtFormat), LocalTime.MIDNIGHT);
			}
			
			bars.add(new TradeBar(
				new Security(
					timeSeries.meta.symbol, 
					timeSeries.meta.exchange, 
					SecurityType.convertTwelvedataType(timeSeries.meta.type)
				), 
				// on failure, see https://www.java67.com/2016/04/how-to-convert-string-to-localdatetime-in-java8-example.html
				dt, 
				timeSeries.meta.interval,
				bar.open,
				bar.high,
				bar.low,
				bar.close
			));
		}
		
		bars.sort(order);
		
		return bars;
	}
	
	public TradeBar() {
		id = new TradeBarId();
	}
	
	/**
	 * Convenience method for identifying a bar but not specifying price data.
	 * 
	 * @param security
	 * @param datetime
	 * @param width
	 */
	public TradeBar(Security security, LocalDateTime datetime, String width) {
		this(security, datetime, width, 0, 0, 0, 0);
	}
	
	/**
	 * {@link TradeBar} constructor for fully defining a historical asset trade price bar.
	 * 
	 * @param security
	 * @param datetime
	 * @param width
	 * @param open
	 * @param high
	 * @param low
	 * @param close
	 */
	public TradeBar(Security security, LocalDateTime datetime, String width, float open, float high, float low, float close) {
		id = new TradeBarId(security,datetime,width);
		
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
	}
	
	public float getOpen() {
		return open;
	}
	
	public float getHigh() {
		return high;
	}
	
	public float getLow() {
		return low;
	}
	
	public float getClose() {
		return close;
	}
	
	public LocalDateTime getDatetime() {
		return id.getDatetime();
	}
	
	@Override
	public String toString() {
		return "TradeBar(id=" + id + ")";
	}
	
	/**
	 * Implements comparison for chronological sort using {@code LocalDateTime.compareTo}.
	 */
	@Override
	public int compareTo(TradeBar other) {
		return id.getDatetime().compareTo(other.id.getDatetime());
	}
}
