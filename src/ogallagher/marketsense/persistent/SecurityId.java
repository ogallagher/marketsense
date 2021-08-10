package ogallagher.marketsense.persistent;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class SecurityId implements Serializable {
	private static final long serialVersionUID = 7344248639231276093L;
	
	public static final String DB_COL_SYMBOL = "symbol";
	private String symbol;
	
	public static final String DB_COL_EXCHANGE = "exchange";
	private String exchange;
	
	public SecurityId() {
		// required according to https://www.baeldung.com/jpa-composite-primary-keys
		this("","");
	}
	
	public SecurityId(String symbol, String exchange) {
		this.symbol = symbol;
		this.exchange = exchange;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return exchange + ':' + symbol;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof SecurityId && hashCode() == other.hashCode();
	}
}
