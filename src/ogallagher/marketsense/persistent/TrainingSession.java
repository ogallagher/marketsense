package ogallagher.marketsense.persistent;

import java.time.LocalDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

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
	private int sampleCount;
	
	public static final String DB_COL_MAX_LOOKBACK_MONTHS = "maxLookbackMonths";
	private int maxLookbackMonths;
	
	public static final String DB_COL_PASSES = "passes";
	private int passes;
	
	public static final String DB_COL_FAILS = "fails";
	private int fails;
	
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
		passes = 0;
		fails = 0;
	}
	
	@Override
	public String toString() {
		return "TrainingSession(id=" + id + ", type=" + type + ", security=" + security + ")";
	}
}
