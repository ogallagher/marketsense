package ogallagher.marketsense.persistent;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Composite primary key for {@link TrainingSession}.
 * 
 * @author Owen Gallagher
 *
 */
@Embeddable
public class TrainingSessionId implements Serializable {
	private static final long serialVersionUID = -4254662257445784411L;
	
	private static LocalDateTime lastStart = LocalDateTime.MIN;
	
	public static final String DB_COL_PERSON = "person";
	@ManyToOne
	@JoinColumn(name=DB_COL_PERSON)
	private Person person;
	
	public static final String DB_COL_START = "start";
	private LocalDateTime start;
	
	public TrainingSessionId() {
		this(new Person());
	}
	
	public TrainingSessionId(Person person) {
		this.person = person;
		
		LocalDateTime rawStart = LocalDateTime.now();
		
		// prevent any overlapping start times
		if (!rawStart.isAfter(lastStart)) {
			start = lastStart.plusSeconds(1);
		}
		else {
			start = rawStart;
		}
		
		lastStart = start;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return person.getUsername() + "-" + start.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof TrainingSessionId && hashCode() == other.hashCode();
	}
}
