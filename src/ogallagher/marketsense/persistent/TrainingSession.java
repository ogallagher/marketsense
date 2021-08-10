package ogallagher.marketsense.persistent;

import java.time.LocalDateTime;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

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
	
	public static final String DB_COL_SAMPLE_SIZE = "sampleSize";
	private int sampleSize;
	
	public static final String DB_COL_PASSES = "passes";
	private int passes;
	
	public static final String DB_COL_FAILS = "fails";
	private int fails;
	
	public TrainingSession() {
		this(new Person(), 0);
	}
	
	public TrainingSession(Person person, int sampleSize) {
		this.id = new TrainingSessionId(person);
		
		end = null;
		this.sampleSize = sampleSize;
		passes = 0;
		fails = 0;
	}
}
