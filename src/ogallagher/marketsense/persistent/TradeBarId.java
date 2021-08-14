package ogallagher.marketsense.persistent;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.*;

import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;

/**
 * Composite key for {@link TradeBar}
 * 
 * @author Owen Gallagher
 *
 */
@Embeddable
public class TradeBarId implements Serializable {
	private static final long serialVersionUID = -4739218606014682561L;
	
	public static final String DB_COL_SECURITY = "security";
	public static final String DB_COL_SEC_SYMBOL = "securitySymbol";
	public static final String DB_COL_SEC_EXCHANGE = "securityExchange";
	@ManyToOne
	@JoinColumns(value = { 
		@JoinColumn(name=DB_COL_SEC_SYMBOL, referencedColumnName=SecurityId.DB_COL_SYMBOL),
		@JoinColumn(name=DB_COL_SEC_EXCHANGE, referencedColumnName=SecurityId.DB_COL_EXCHANGE)
	})
	private Security security;
	public static final String DB_COL_DATETIME = "datetime";
	private LocalDateTime datetime;
	
	public static final String DB_COL_WIDTH = "width";
	/**
	 * Bar width string. See {@link BarInterval} for valid options.
	 */
	String width;
	
	public TradeBarId() {
		this(new Security(), LocalDateTime.MIN, BarInterval.DY_1);
	}
	
	public TradeBarId(Security security, LocalDateTime datetime, String width) {
		this.security = security;
		this.datetime = datetime;
		this.width = width;
	}
	
	public LocalDateTime getDatetime() {
		return datetime;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return security.getId().toString() + "-" + datetime.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof SecurityId && hashCode() == other.hashCode();
	}
}
