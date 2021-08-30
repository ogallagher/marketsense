package ogallagher.marketsense.persistent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.sound.sampled.SourceDataLine;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import ogallagher.marketsense.MarketSample;
import ogallagher.marketsense.MarketSynth;
import ogallagher.twelvedata_client_java.TwelvedataClient;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;
import ogallagher.twelvedata_client_java.TwelvedataInterface.Failure;
import ogallagher.twelvedata_client_java.TwelvedataInterface.TimeSeries;

/**
 * A single training session.
 * 
 * @author Owen Gallagher
 * @since 2021-08-10
 *
 */
@Entity
public class TrainingSession {
	public static final String DB_TABLE = "TrainingSession";
	
	/**
	 * The confidence level to use when returning a session score confidence interval.
	 */
	private static ConfidenceZscore scoreIntervalConfidence = ConfidenceZscore.NINETY_FIVE;
	
	public static final String DB_COL_ID = "id";
	@EmbeddedId
	private TrainingSessionId id;
	
	public static final String DB_COL_END = "end";
	private LocalDateTime end;
	
	public static final String DB_COL_TYPE = "type";
	private TrainingSessionType type;
	
	public static final String DB_COL_SEC_SYMBOL = "securitySymbol";
	public static final String DB_COL_SEC_EXCHANGE = "securityExchange";
	@ManyToOne
	@JoinColumns(value = { 
		@JoinColumn(name=DB_COL_SEC_SYMBOL, referencedColumnName=SecurityId.DB_COL_SYMBOL),
		@JoinColumn(name=DB_COL_SEC_EXCHANGE, referencedColumnName=SecurityId.DB_COL_EXCHANGE)
	})
	private Security security;
	
	public static final String DB_COL_BAR_WIDTH = "barWidth";
	private String barWidth;
	
	public static final String DB_COL_SAMPLE_SIZE = "sampleSize";
	private int sampleSize;
	
	public static final String DB_COL_SAMPLE_COUNT = "sampleCount";
	/**
	 * Number of samples in the training session.
	 */
	private int sampleCount;
	
	public static final String DB_COL_MAX_LOOKBACK_MONTHS = "maxLookbackMonths";
	private int maxLookbackMonths;
	
	public static final String DB_COL_SCORE = "score";
	/**
	 * The accuracy score of the user for this session, calculated as an average of all scores
	 * per market sample.
	 * 
	 * @see MarketSample#evalGuess(double)
	 */
	@Convert(converter=DoublePropertyPersister.class)
	private DoubleProperty score = new SimpleDoubleProperty(0);
	
	public static final String DB_COL_SCORE_DEVIATION = "scoreDeviation";
	/**
	 * <p>The standard deviation of accuracy scores per market sample throughout the session. This will be used to
	 * create a score confidence interval following the formula:</p>
	 * 
	 * <p>
	 * {@code score +- (z_score * scoreDeviation/sampleCount)}
	 * </p>
	 * 
	 * <p>where {@code z_score} is depends on the confidence level.
	 * 
	 * @see #score
	 * @see #sampleCount
	 * @see #scoreIntervalConfidence
	 */
	@Convert(converter=DoublePropertyPersister.class)
	private DoubleProperty scoreDeviation = new SimpleDoubleProperty(0);
	
	/**
	 * Earliest datetime where a sample can be taken.
	 */
	@Transient
	private LocalDateTime after;
	
	/**
	 * Latest datetime where a sample can be taken, being the first datetime within that corresponding sample.
	 */
	@Transient
	private LocalDateTime before;
	
	/**
	 * Current sample number, in training progress, between {@code 0} and {@link sampleCount sampleCount-1}.
	 */
	@Transient
	private IntegerProperty sampleId = new SimpleIntegerProperty(0);
	
	/**
	 * List of accuracy scores from each sample. Used to calculate {@link #scoreDeviation}.
	 */
	@Transient
	private ArrayList<Double> sampleScores;
	
	@Transient
	private MarketSample sample;
	
	/**
	 * Whether or not the training session was completed.
	 */
	@Transient
	private BooleanProperty complete = new SimpleBooleanProperty(false);
	
	@Transient
	private SourceDataLine sound = null;
	
	/**
	 * Persist a session to the database, ensuring all persistent members exist in the database already.
	 * 
	 * @param session The training session to save to the database.
	 */
	public static void persist(TrainingSession session, EntityManager dbManager) {
		dbManager.getTransaction().begin();
		
		Security dbSecurity = dbManager.find(Security.class, session.security.getId());
		if (dbSecurity != null) {
			session.security = dbSecurity;
		}
		else {
			dbManager.persist(session.security);
		}
		
		dbManager.persist(session);
		
		dbManager.getTransaction().commit();
	}
	
	public TrainingSession() {
		this(new Person(), TrainingSessionType.TBD, new Security(), BarInterval.DY_1, 0, 0, 0);
	}
	
	public TrainingSession(Person person, TrainingSessionType type, Security security, String barWidth, int sampleSize, int sampleCount, int maxLookbackMonths) {
		this.id = new TrainingSessionId(person);
		
		this.type = type;
		end = null;
		this.security = security;
		this.barWidth = barWidth;
		this.sampleSize = sampleSize;
		this.sampleCount = sampleCount;
		this.maxLookbackMonths = maxLookbackMonths;
		
		sampleId.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
				if (nv.intValue() >= sampleCount) {
					// session complete
					complete.set(true);
					end = LocalDateTime.now();
				}
			}
		});
		
		sampleScores = new ArrayList<>(this.sampleCount);
		
		LocalDateTime now = id.getStart();
		after = now.minusMonths(this.maxLookbackMonths);
		before = now.minusDays(2);
		before = BarInterval.offsetBars(before, barWidth, -sampleSize);
	}
	
	/**
	 * Creates a new market sample if the training session is not complete. Otherwise, {@code null} is returned.
	 * 
	 * @param dbManager
	 * @param marketSynth
	 * @return The new market sample, or {@code null} if the training session was completed.
	 */
	public MarketSample nextSample(EntityManager dbManager, MarketSynth marketSynth) {
		if (sample != null) {
			// increment sample id
			sampleId.set(sampleId.get()+1);
		}
		
		if (!complete.get()) {
			int sampleOffsetHours = (int) (Math.random() * (Duration.between(after, before).getSeconds()/(3600)));
			LocalDateTime start = after.plusHours(sampleOffsetHours);
			LocalDateTime end = BarInterval.offsetBars(start, barWidth, sampleSize);
			
			sample = new MarketSample(security, start, end, barWidth);
			sample.prepare(dbManager, marketSynth);
			
			System.out.println("DEBUG prepared next training sample " + sample);
			return sample;
		}
		else {
			return null;
		}
	}
	
	/**
	 * In order to extract samples from historical market data, that population/universe from which the trade bars are
	 * taken needs to exist in the database.
	 * 
	 * Note that this works with the assumption that universes will always be defined as an interval from a lookback to the present,
	 * so there are never any holes between the start and end datetimes.
	 * 
	 * Note that ideal universe bounds won't necessarily match valid market calendars and market hours, so in cases where this is
	 * expected, the universe bounds {@link #after} .. {@link #before} will be updated to match what the database does have.
	 * 
	 * @return The failure, or {@code null} if the needed market data is now in the database.
	 */
	public Failure collectMarketUniverse(EntityManager dbManager, TwelvedataClient marketClient) {
		Failure result = null;
		
		boolean firstUp = false;
		boolean lastDown = false;
		
		TradeBar first = new TradeBar(security, after, barWidth);
		TradeBar last = new TradeBar(security, BarInterval.offsetBars(before, barWidth, sampleSize), barWidth);
		System.out.println("DEBUG ensure market universe for " + first.getDatetime() + " to " + last.getDatetime());
		
		TradeBar preLast = null, postFirst = null;
		
		dbManager.getTransaction().begin();
		if (result == null && !dbManager.contains(first)) {
			// move forward to find earliest bar after first
			String qstr = String.format(
				"select t from %5$s t " + 
				"where t.%1$s = :secSymbol and t.%2$s = :secExchange " + 
				"and t.%3$s = :barWidth and t.%4$s > :datetime " + 
				"order by t.%4$s asc",
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_EXCHANGE,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_WIDTH,
				TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_DATETIME,
				TradeBar.DB_TABLE
			);
			System.out.println("DEBUG " + qstr);
			Query query = dbManager.createQuery(qstr);
			query.setMaxResults(1);
			
			query.setParameter("secSymbol", security.getSymbol());
			query.setParameter("secExchange", security.getExchange());
			query.setParameter("barWidth", barWidth);
			query.setParameter("datetime", first.getDatetime());
			
			try {
				preLast = (TradeBar) query.getSingleResult();
			}
			catch (NoResultException e) {
				preLast = last;
				
				System.out.println("WARNING " + e.getMessage());
				System.out.println("failed to fetch first in market universe after " + first.getDatetime() + "; using last");
			}
			
			// fetch trade bars from first to preLast
			TimeSeries timeSeries = marketClient.fetchTimeSeries(
				security.getSymbol(), barWidth, 
				first.getDatetime(), BarInterval.offsetBars(preLast.getDatetime(), barWidth, -1)
			);
			if (!timeSeries.isFailure()) {
				// convert to db-compat trade bars and persist
				List<TradeBar> bars = TradeBar.convertTimeSeries(timeSeries, Comparator.naturalOrder());
				
				for (TradeBar bar : bars) {
					dbManager.persist(bar);
				}
				System.out.println("persisted " + bars.size() + " new bars");
			}
			else {
				Failure f = (Failure) timeSeries;
				switch (f.code) {
					case Failure.ErrorCode.API_KEY:
					case Failure.ErrorCode.CALL_LIMIT:
					case Failure.ErrorCode.NO_COMMS:
					case Failure.ErrorCode.NULL_RESPONSE:
						result = f;
						break;
						
					default:
						System.out.println(
							"WARNING failed to fetch first-prelast for universe, perhaps no bars exist: " + f.toString()
						);
						// update first to be preLast
						firstUp = true;
						break;
				}
			}
		}
		dbManager.getTransaction().commit();
		
		dbManager.getTransaction().begin();
		if (result == null && !dbManager.contains(last)) {
			// move backward to find latest bar before last
			Query query = dbManager.createQuery(
				String.format(
					"select t from %5$s t " + 
					"where t.%1$s = :secSymbol and t.%2$s = :secExchange " + 
					"and t.%3$s = :barWidth and t.%4$s < :datetime " + 
					"order by t.%4$s desc",
					TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL,
					TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_SECURITY + "." + Security.DB_COL_ID + "." + SecurityId.DB_COL_EXCHANGE,
					TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_WIDTH,
					TradeBar.DB_COMPCOL_ID + "." + TradeBarId.DB_COL_DATETIME,
					TradeBar.DB_TABLE
				)
			);
			query.setMaxResults(1);
			
			query.setParameter("secSymbol", security.getSymbol());
			query.setParameter("secExchange", security.getExchange());
			query.setParameter("barWidth", barWidth);
			query.setParameter("datetime", last.getDatetime());
			
			try {
				postFirst = (TradeBar) query.getSingleResult();
			}
			catch (NoResultException e) {
				postFirst = first;
				
				System.out.println("WARNING " + e.getMessage());
				System.out.println("failed to fetch last in market universe before " + last.getDatetime() + "; using first");
			}
			
			// fetch trade bars from postFirst to last
			TimeSeries timeSeries = marketClient.fetchTimeSeries(
				security.getSymbol(), barWidth, 
				BarInterval.offsetBars(postFirst.getDatetime(), barWidth, 1), last.getDatetime()
			);
			if (!timeSeries.isFailure()) {
				// convert to db-compat trade bars and persist
				List<TradeBar> bars = TradeBar.convertTimeSeries(timeSeries, Comparator.naturalOrder());
				
				for (TradeBar bar : bars) {
					dbManager.persist(bar);
				}
				System.out.println("persisted " + bars.size() + " new bars");
			}
			else {
				Failure f = (Failure) timeSeries;
				switch (f.code) {
					case Failure.ErrorCode.API_KEY:
					case Failure.ErrorCode.CALL_LIMIT:
					case Failure.ErrorCode.NO_COMMS:
					case Failure.ErrorCode.NULL_RESPONSE:
						result = f;
						break;
						
					default:
						System.out.println("WARNING failed to fetch postfirst-last for universe, perhaps no bars exist: " + f);
						// update last to be postFirst
						lastDown = true;
						break;
				}
				
			}
		}
		dbManager.getTransaction().commit();
		
		if (result == null) {
			if (firstUp) {
				after = preLast.getDatetime();
			}
			if (lastDown) {
				before = BarInterval.offsetBars(postFirst.getDatetime(), barWidth, -sampleSize);
			}
			System.out.println("DEBUG universe trimmed to " + first.getDatetime() + " to " + last.getDatetime());
		}
		
		return result;
	}
	
	/**
	 * Given a new guess score corresponding to a market sample, update {@link #score} and {@link #scoreDeviation}.
	 * 
	 * @param guessScore Guess score, usually calculated with {@link MarketSample#evalGuess(double)}.
	 */
	public void updateScore(double guessScore) {
		int i = sampleId.get();
		
		// update score mean
		score.set(((score.get() * i) + guessScore) / (i+1));
		
		// update score std deviation: sqrt of sum of square diffs with mean
		sampleScores.add(guessScore);
		
		double sumSquareDiffs = 0;
		for (Double s : sampleScores) {
			sumSquareDiffs += Math.pow(s-score.get(), 2);
		}
		
		scoreDeviation.set(Math.sqrt(sumSquareDiffs));
	}
	
	/**
	 * <p>{@code radius = confidence.getZscore() * deviation}</p>
	 * 
	 * @return Score confidence interval radius, as in: {@code interval = mean +- radius}.
	 */
	public double getScoreIntervalRadius() {
		return scoreIntervalConfidence.getZscore() * scoreDeviation.get();
	}
	
	public MarketSample getSample() {
		return sample;
	}
	
	public Security getSecurity() {
		return security;
	}
	
	public ReadOnlyIntegerProperty getSampleIdProperty() {
		return sampleId;
	}
	
	public int getSampleCount() {
		return sampleCount;
	}
	
	public int getSampleSize() {
		return sampleSize;
	}
	
	public ReadOnlyBooleanProperty getCompleteProperty() {
		return complete;
	}
	
	public ReadOnlyDoubleProperty getScoreProperty() {
		return score;
	}
	
	public ReadOnlyDoubleProperty getScoreDeviationProperty() {
		return scoreDeviation;
	}
	
	public static ConfidenceZscore getScoreIntervalConfidence() {
		return scoreIntervalConfidence;
	}
	
	/**
	 * Set confidence level for {@link #getScoreIntervalRadius()}.
	 * 
	 * @param scoreIntervalConfidence Confidence level.
	 */
	public static void setScoreIntervalConfidence(ConfidenceZscore scoreIntervalConfidence) {
		TrainingSession.scoreIntervalConfidence = scoreIntervalConfidence;
	}
	
	public int getMaxLookbackMonths() {
		return maxLookbackMonths;
	}
	
	/**
	 * TODO create a conversion between {@link BarInterval} values and readable strings, perhaps not in this method.
	 * 
	 * @return
	 */
	public String getBarWidth() {
		return barWidth;
	}
	
	public Duration getDuration() {
		return Duration.between(id.getStart(), end);
	}
	
	@Override
	public String toString() {
		return "TrainingSession(id=" + id + ", type=" + type + ", security=" + security + ")";
	}
	
	public static class TrainingSessionListCell extends ListCell<TrainingSession> {
		@Override
		public void updateItem(TrainingSession item, boolean empty) {
			super.updateItem(item, empty);
			
			if (item != null) {
				setText(
					item.security.getSymbol() + " --- " + 
					((float) (item.score.get()*100)) + "%"
				);
			}
		}
	}
	
	/**
	 * <p>Calculates zscores for common confidence levels. Note that this implies an infinite sample size, and therefore
	 * does not account for degrees of freedom.</p>
	 * 
	 * <p>Zscores taken from <a href="https://www.statisticshowto.com/tables/t-distribution-table/">this table</a>.</p>
	 * 
	 * @author Owen Gallagher
	 * @since 2021-08-29
	 */
	private static enum ConfidenceZscore {
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
}
