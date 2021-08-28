package ogallagher.marketsense.persistent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalUnit;
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
	@Convert(converter=DoublePropertyPersister.class)
	private DoubleProperty score = new SimpleDoubleProperty(0);
	
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
	 * Current sample number, in training progress, between {@code 0} and {@link sampleCount}.
	 */
	@Transient
	private IntegerProperty sampleId = new SimpleIntegerProperty(0);
	
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
	
	public void updateScore(double guessScore) {
		int i = sampleId.get();
		score.set(((score.get() * i) + guessScore) / (i+1));
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
}
