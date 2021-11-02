package ogallagher.marketsense.widgets;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.MarketSense;

/**
 * 
 * @author Owen Gallagher
 * @since 2021-11-01
 *
 */
public class AccountComboBox extends ComboBox<Person> {
	private AccountComboBox self;
	
	/**
	 * Default constructor.
	 */
	public AccountComboBox() {
		super();
		
		// load people from db
		MarketSense.loadPeople(ShowPeople.class, true);
		self = this;
	}
	
	/**
	 * 
	 * @author Owen Gallagher
	 * @since 2021-11-01
	 *
	 */
	public class ShowPeople implements Runnable {
		private List<Person> people;
		
		public ShowPeople(List<Person> people) {
			this.people = people;
		}
		
		@Override
		public void run() {
			self.setItems(FXCollections.observableList(people));
			self.setCellFactory(new Callback<ListView<Person>, ListCell<Person>>() {
				@Override
				public ListCell<Person> call(ListView<Person> param) {
					return new Person.PersonListCell();
				}
			});
		}
	}
}
