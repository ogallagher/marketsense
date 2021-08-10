package ogallagher.marketsense.persistent;

import javax.persistence.*;

/**
 * A tradable financial asset/security. 
 * 
 * @author Owen Gallagher
 */
@Entity
public class Security {
	public static final String DB_TABLE = "Security";
	
	public static final String DB_COL_ID = "id";
	@EmbeddedId
	private SecurityId id;
	
	public static final String DB_COL_TYPE = "type";
	private SecurityType type;
	
	public Security() {
		this("", "", SecurityType.STOCK);
	}
	
	public Security(String symbol, String exchange, SecurityType type) {
		this.id = new SecurityId(symbol, exchange);
		this.type = type;
	}
	
	public SecurityId getId() {
		return id;
	}
	
	public String getSymbol() {
		return id.getSymbol();
	}
	
	public String getExchange() {
		return id.getExchange();
	}
	
	@Override
	public String toString() {
		return "Security(id=" + id + ",type=" + type + ")";
	}
}
