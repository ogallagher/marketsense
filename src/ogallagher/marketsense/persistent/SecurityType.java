package ogallagher.marketsense.persistent;

public enum SecurityType {
	STOCK,
	ETF,
	FOREX,
	FUTURE;
	
	/**
	 * Convert a twelvedata api security type string to a {@code SecurityType} value, or {@code null}
	 * on failure.
	 * 
	 * @param type
	 * @return
	 */
	public static SecurityType convertTwelvedataType(String type) {
		switch (type) {
			case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.COMMON_STOCK:
				return STOCK;
				
			case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.ETF:
				return ETF;
			
			default:
				return null;
		}
	}
}
