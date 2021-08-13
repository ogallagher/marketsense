package ogallagher.marketsense;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.sound.sampled.AudioInputStream;

import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.TradeBar;
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
	public void prepare(EntityManager dbManager, MarketSynth marketSynth) {
		// fetch market data from database TODO
		bars.clear();
		
		LocalDateTime step = start;
		while (step.compareTo(end) <= 0) {
			float close = (float) Math.random() * 100;
			bars.add(new TradeBar(security, step, barWidth, 0, 0, 0, close));
			step = BarInterval.offsetBars(step, barWidth, 1);
		}
		
		float close = (float) Math.random() * 100;
		future = new TradeBar(security, step, barWidth, 0, 0, 0, close);
		
		// analyze future movement
		float last = bars.get(bars.size()-1).getClose();
		float future = this.future.getClose();
		
		// constrain to -100% .. 100% of last price, normalize to 0 .. 1
		futureMovement = (((future - last) / last) + 1) / 2;
		if (futureMovement > 1) {
			futureMovement = 1;
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
	 * Calculate the accuracy/score of a guess.
	 * 
	 * @param guess
	 * 
	 * @return Score.
	 */
	public double evalGuess(double guess) {
		double score = 1 - (Math.abs(guess - futureMovement) / 2);
		
		return score;
	}
	
	public AudioInputStream getSound() {
		return sound;
	}
	
	public double getFutureMovement() {
		return futureMovement;
	}
	
	public Color getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return "MarketSample(security=" + security + ", start=" + start + ", end=" + end + ")";
	}
}
