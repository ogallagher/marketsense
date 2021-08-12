package ogallagher.marketsense.persistent;

import java.time.LocalDateTime;

import javax.persistence.*;

/**
 * Security trade bar, with OHLC data at a given time.
 * 
 * @author Owen Gallagher
 *
 */
@Entity
public class TradeBar {
	public static final String DB_TABLE = "TradeBar";
	
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
	
	public TradeBar() {
		id = new TradeBarId();
	}
	
	public TradeBar(Security security, LocalDateTime datetime, String width, float open, float high, float low, float close) {
		id = new TradeBarId(security,datetime,width);
		
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
	}
	
	@Override
	public String toString() {
		return "TradeBar(id=" + id + ")";
	}
}
