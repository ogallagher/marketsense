package ogallagher.marketsense.util;

/**
 * <p>Calculates zscores for common confidence levels. Note that this implies an infinite sample size, and therefore
 * does not account for degrees of freedom.</p>
 * 
 * <p>Zscores taken from <a href="https://www.statisticshowto.com/tables/t-distribution-table/">this table</a>.</p>
 * 
 * @author Owen Gallagher
 * @since 2021-08-29
 */
public enum ConfidenceZscore {
	/**
	 * 90% (0.9) confidence level.
	 */
	NINETY(0.9),
	/**
	 * 95% (0.95) confidence level.
	 */
	NINETY_FIVE(0.95),
	/**
	 * 99% (0.99) confidence level.
	 */
	NINETY_NINE(0.99);
	
	private double confidence;
	
	private ConfidenceZscore(double confidence) {
		this.confidence = confidence;
	}
	
	/**
	 * @return The numeric value of this confidence level, between 0 and 1.
	 */
	public double getConfidence() {
		return confidence;
	}
	
	/**
	 * @return The corresponding zscore for this confidence level.
	 */
	public double getZscore() {
		switch (this) {
			case NINETY:
				return 1.645;
				
			case NINETY_FIVE:
				return 1.96;
				
			case NINETY_NINE:
				return 2.576;
				
			default:
				throw new IllegalArgumentException("unknown zscore for confidence " + confidence);
		}
	}
}