package ogallagher.marketsense.widgets;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import ogallagher.temp_fx_logger.System;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.util.HasCallback;
import ogallagher.marketsense.MarketSense;

/**
 * 
 * @author Owen Gallagher
 * @since 2021-11-01
 *
 */
public class AccountComboBox extends ComboBox<Person> implements HasCallback<AccountComboBox.ShowPeople> {	
	/**
	 * Default constructor.
	 */
	public AccountComboBox() {
		super();
		
		// how to display people in options list
		setCellFactory(new Callback<ListView<Person>, ListCell<Person>>() {
			@Override
			public ListCell<Person> call(ListView<Person> param) {
				return new Person.PersonListCell();
			}
		});
		
		// how to display person selected
		setConverter(new StringConverter<Person>() {
			@Override
			public String toString(Person person) {
				return person.getUsername();
			}

			@Override
			public Person fromString(String string) {
				ObservableList<Person> items = getItems();
				Person namesake = new Person(string);
				int i = items.indexOf(namesake);
				
				if (i != -1) {
					return items.get(i);
				}
				else {
					return null;
				}
			}
		});
		
		// load people from db
		MarketSense.loadPeopleHasCallback(this, true);
	}
	
	/**
	 * 
	 * @author Owen Gallagher
	 * @since 2021-11-01
	 *
	 */
	public class ShowPeople implements Runnable {
		private List<Person> people;
		private AccountComboBox parent;
		
		public ShowPeople(List<Person> people, AccountComboBox parent) {
			this.people = people;
			this.parent = parent;
		}
		
		@Override
		public void run() {
			System.out.println("debug loading " + people.size() + " people into account combo box");
			parent.setItems(FXCollections.observableList(people));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ShowPeople getCallback(Object... args) 
		throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
		NoSuchMethodException, SecurityException {
		return new ShowPeople((List<Person>) args[0], this);
	}
	
	@Override
	public void setCallback(Class<ShowPeople> Callback) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("cannot change account combo box callback");
	}
}
