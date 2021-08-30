package ogallagher.marketsense;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sound.sampled.AudioInputStream;

import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.SecurityId;
import ogallagher.marketsense.persistent.TradeBar;
import ogallagher.marketsense.persistent.TradeBarId;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;

/**
 * Encapsulates market data its marketsense data mapping for use with training and testing sessions.
 * 
 * @author Owen Gallagher
 * @since 2021-08-12
 *
 */
public class MarketSample {
	public static final Color COLOR_LOW = Color.RED;
	public static final Color COLOR_HIGH = Color.GREEN;
	
	private Security security;
	
	private LocalDateTime start;
	private LocalDateTime end;
	private String barWidth;
	private ArrayList<TradeBar> bars;
	private TradeBar future;
	
	private AudioInputStream sound;
	/**
	 * The last bar is the sample's _future_, whose value relative to the previous determines the future movement
	 * for that sample.
	 */
	private double futureMovement = 0.5;
	/**
	 * The {@link futureMovement} of a sample is directly mapped to a color, interpolated between
	 * {@link COLOR_LOW} and {@link COLOR_HIGH}.
	 */
	private Color color;
	
	private FutureMovementFormula futureMovementFormula = FutureMovementFormula.DELTA_SAMPLE_RANGE;
	
	public static Color valueToColor(double value, Color lowColor, Color highColor) {
		double antivalue = 1-value;
		
		return new Color(
			(int) Math.round(value*highColor.getRed() + antivalue*lowColor.getRed()),
			(int) Math.round(value*highColor.getGreen() + antivalue*lowColor.getGreen()),
			(int) Math.round(value*highColor.getBlue() + antivalue*lowColor.getBlue())
		);
	}
	
	/**
	 * 
	 * @param security The security for which to fetch market data.
	 * 
	 * @param start First market datapoint datetime.
	 * 
	 * @param end Last market datapoint datetime in the sample. Note this **does not** include the future bar,
	 * which will determine the future movement for the sample.
	 */
	public MarketSample(Security security, LocalDateTime start, LocalDateTime end, String barWidth) {
		this.security = security;
		this.start = start;
		this.end = end;
		this.barWidth = barWidth;
		this.bars = new ArrayList<TradeBar>();
	}
	
	/**
	 * Fetch the required market data from the database and create the resulting sound and color.
	 */
	@SuppressWarnings("unchecked")
	public void prepare(EntityManager dbManager, MarketSynth marketSynth) {
		// fetch market data from database
		Query query = dbManager.createQuery(
			String.format(
				"select t from %1$s t " + 
				"where t.%2$s = :securitySymbol and t.%3$s = :securityExchange and t.%4$s = :barWidth " +
				"and t.%5$s >= :start and t.%5$s <= :end " +
				"order by t.%5$s asc",
				TradeBar.DB_TABLE,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_EXCHANGE,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_WIDTH,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_DATETIME
			)
		);
		query.setParameter("securitySymbol", security.getSymbol());
		query.setParameter("securityExchange", security.getExchange());
		query.setParameter("barWidth", barWidth);
		query.setParameter("start", start);
		query.setParameter("end", BarInterval.offsetBars(end, barWidth, 1)); // note this includes one future bar
		
		// set bars
		bars.clear();
		bars.addAll((List<TradeBar>) query.getResultList());
		
		// set future
		future = bars.remove(bars.size()-1);
		
		// analyze future movement
		switch (futureMovementFormula) {
			case PCT_LAST_PX:
				float last = bars.get(bars.size()-1).getClose();
				float future = this.future.getClose();
				
				// constrain to -1 .. 1 proportion of last price, normalize to 0 .. 1
				futureMovement = (((future - last) / last) + 1) / 2;
				break;
				
			case DELTA_SAMPLE_RANGE:
				float deltaMax = 0;
				
				float a = bars.get(0).getClose();
				for (int i=1; i<bars.size(); i++) {
					float b = bars.get(i).getClose();
					float d = Math.abs(b-a);
					
					if (d > deltaMax) {
						deltaMax = d;
					}
					
					a = b;
				}
				
				// normalize future-last delta between -max and max
				futureMovement = (this.future.getClose()-a) / deltaMax;
				
				// map -1 .. 1 to 0 .. 1
				futureMovement = (futureMovement+1) / 2;
				break;
		}
		
		// limit future movement to 0 .. 1
		if (futureMovement > 1) {
			futureMovement = 1;
		}
		else if (futureMovement < 0) {
			futureMovement = 0;
		}
		
		// extract raw market datapoints
		float[] marketData = new float[bars.size()];
		int b = 0;
		for (TradeBar bar : bars) {
			marketData[b++] = bar.getClose();
		}
		
		// create sound
		sound = marketSynth.synthesize(marketData, true);
		
		// create color
		color = valueToColor(futureMovement, COLOR_LOW, COLOR_HIGH);
	}
	
	/**
	 * Calculate the accuracy/score of a guess. Assumes that {@link #futureMovement} and {@code guess} are both
	 * between 0 and 1.
	 * 
	 * @param guess
	 * 
	 * @return Score.
	 */
	public double evalGuess(double guess) {
		double score = 1 - Math.abs(guess - futureMovement);
		
		return score;
	}
	
	public AudioInputStream getSound() {
		return sound;
	}
	
	/**
	 * @return {@link #futureMovement}
	 */
	public double getFutureMovement() {
		return futureMovement;
	}
	
	public TradeBar getFuture() {
		return future;
	}
	
	public Color getColor() {
		return color;
	}
	
	public List<TradeBar> getBars() {
		return bars;
	}
	
	@Override
	public String toString() {
		return "MarketSample(security=" + security + ", start=" + start + ", end=" + end + ")";
	}
	
	/**
	 * @return A concise and unique identifying string for this sample.
	 */
	public String idString() {
		return new StringBuilder()
			.append(security.getSymbol())
			.append('_')
			.append(start)
			.append('_')
			.append(end)
			.append('_')
			.append(barWidth)
			.append('_')
			.append(bars.size())
			.toString();
	}
	
	public static enum FutureMovementFormula {
		/**
		 * Proportional difference from last px in sample.
		 */
		PCT_LAST_PX,
		/**
		 * Normalize within range of unsigned price differences for all neighbors in the sample.
		 */
		DELTA_SAMPLE_RANGE
	}
}
