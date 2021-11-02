package ogallagher.marketsense.persistent;

import java.time.LocalDate;

import javax.persistence.*;

import javafx.scene.control.ListCell;

/**
 * A person/user.
 * 
 * @author Owen Gallagher
 * @since 12 June 2021
 */
@Entity
public class Person {
	public static final String DB_TABLE = "Person";
	
	@Id
	private String username;
	public static final String DB_COL_USERNAME = "username";
	
	private LocalDate sinceDate;
	public static final String DB_COL_SINCE_DATE = "sinceDate";
	/**
	 * Asset value prediction accuracy, on a 0-1 scale.
	 * 
	 * Initial value is {@value #accuracy}
	 */
	private float accuracy = 0.5f;
	public static final String DB_COL_ACCURACY = "accuracy";
	
	/**
	 * Default constructor required for some JPA queries.
	 */
	public Person() {
		this("");
	}
	
	public Person(String username) {
		this.username = username;
		this.sinceDate = LocalDate.now();
	}
	
	public Person(String username, LocalDate sinceDate) {
		this(username);
		this.sinceDate = sinceDate;
	}
	
	public String getUsername() {
		return username;
	}
	
	public float getAccuracy() {
		return accuracy;
	}
	
	public LocalDate getSince() {
		return sinceDate;
	}
	
	public String toString() {
		return "Person(username=" + username + ", since=" + sinceDate + ", accuracy=" + accuracy + ")";
	}
	
	public boolean equals(Object other) {
		return (other instanceof Person) 
			&& this.getUsername().equals(((Person) other).getUsername());
	}
	
	/**
	 * Custom widget for displaying information about a person in containers like lists and combo boxes.
	 * 
	 * @author Owen Gallagher
	 *
	 */
	public static class PersonListCell extends ListCell<Person> {
		@Override
		public void updateItem(Person item, boolean empty) {
			super.updateItem(item, empty);
			
			if (item != null) {
				setText(item.username + " " + item.accuracy);
			}
		}
	}
}
