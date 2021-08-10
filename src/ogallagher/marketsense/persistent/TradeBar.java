package ogallagher.marketsense.persistent;

import java.time.LocalDate;

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
	
	public TradeBar() {
		id = new TradeBarId();
	}
	
	public TradeBar(Security security, LocalDate date, float open, float high, float low, float close) {
		id = new TradeBarId(security,date,open,high,low,close);
	}
}
