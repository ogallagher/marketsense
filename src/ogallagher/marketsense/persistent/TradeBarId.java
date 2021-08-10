package ogallagher.marketsense.persistent;

import java.io.Serializable;
import java.time.LocalDate;

import javax.persistence.*;

/**
 * Composite key for {@link TradeBar}
 * 
 * @author Owen Gallagher
 *
 */
@Embeddable
public class TradeBarId implements Serializable {
	private static final long serialVersionUID = -4739218606014682561L;
	
	public static final String DB_COL_SEC_SYMBOL = "securitySymbol";
	public static final String DB_COL_SEC_EXCHANGE = "securityExchange";
	@ManyToOne
	@JoinColumns(value = { 
		@JoinColumn(name=DB_COL_SEC_SYMBOL, referencedColumnName=SecurityId.DB_COL_SYMBOL),
		@JoinColumn(name=DB_COL_SEC_EXCHANGE, referencedColumnName=SecurityId.DB_COL_EXCHANGE)
	})
	private Security security;
	private LocalDate date;
	
	public static final String DB_COL_OPEN = "open";
	float open;
	public static final String DB_COL_HIGH = "high";
	float high;
	public static final String DB_COL_LOW = "low";
	float low;
	public static final String DB_COL_CLOSE = "close";
	float close;
	
	public TradeBarId() {
		this(new Security(), LocalDate.MIN, 0, 0, 0, 0);
	}
	
	public TradeBarId(Security security, LocalDate date, float open, float high, float low, float close) {
		this.security = security;
		this.date = date;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return security.getId().toString() + date.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof SecurityId && hashCode() == other.hashCode();
	}
}
