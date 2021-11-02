package ogallagher.marketsense.persistent;

import javax.persistence.*;

import ogallagher.temp_fx_logger.System;
import ogallagher.twelvedata_client_java.TwelvedataClient;
import ogallagher.twelvedata_client_java.TwelvedataInterface.SecuritySet;

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
	
	/**
	 * Load a security instance by looking up a given symbol in the database, or via Twelvedata if not already in
	 * the database. If not in the database, this new security is persisted.
	 * 
	 * @param symbol
	 * @param dbManager
	 * @param tdclient
	 * 
	 * @return Loaded security, or <code>null</code> on failure.
	 */
	public static Security loadSecurity(String symbol, EntityManager dbManager, TwelvedataClient tdclient) {
		Security security = null;
		
		// try to fetch security from db
		try {
			Security dbSecurity = (Security) dbManager.createQuery(
				"select s from " + Security.DB_TABLE + " s " + 
				"where s." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL + " = :symbol"
			)
			.setParameter("symbol", symbol)
			.setMaxResults(1)
			.getSingleResult();
			
			security = dbSecurity;
		}
		catch (NoResultException | NullPointerException e) {
			// try to use twelvedata api lookup to determine security specs from symbol if not in db
			SecuritySet securitySet = tdclient.symbolLookup(symbol, 10);
			SecuritySet.Security tdSecurity = null;
			
			for (SecuritySet.Security candidate : securitySet.data) {
				if (candidate.symbol.equals(symbol) && (candidate.exchange.equals("NYSE") || candidate.exchange.equals("NASDAQ"))) {
					tdSecurity = candidate;
					break;
				}
				else {
					System.out.println("skip candidate " + candidate);
				}
			}
			
			if (tdSecurity != null) {
				SecurityType securityType = SecurityType.STOCK;
				switch (tdSecurity.instrument_type) {
					case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.ETF:
						securityType = SecurityType.ETF;
						break;
				
					case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.COMMON_STOCK:
						securityType = SecurityType.STOCK;
						break;
				}
				
				security = new Security(tdSecurity.symbol, tdSecurity.exchange, securityType);
				
				// add security to db
				dbManager.getTransaction().begin();
				dbManager.persist(security);
				dbManager.getTransaction().commit();
			}
		}
		
		return security;
	}
}
