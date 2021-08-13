package ogallagher.marketsense.persistent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalUnit;

import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
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
import ogallagher.marketsense.MarketSample;
import ogallagher.marketsense.MarketSynth;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;

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
	
	@Transient
	private LocalDateTime after;
	
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
	 * Persist a session to the database, ensuring all members exist in the database already.
	 * 
	 * @param session The training session to save to the database.
	 */
	public static void persist(TrainingSession session, EntityManager dbManager) {
		dbManager.getTransaction().begin();
		
		if (dbManager.contains(session.security)) {
			session.security = dbManager.find(Security.class, session.security.getId());
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
		
		after = id.getStart().minusMonths(maxLookbackMonths);
		before = id.getStart().minusDays(2);
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
}
