package ogallagher.marketsense;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import ogallagher.twelvedata_client_java.TwelvedataClient;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.test.Test;
import ogallagher.marketsense.test.TestDatabase;
import ogallagher.marketsense.test.TestMarketSynth;
import ogallagher.temp_fx_logger.System;

/**
 * @author Owen Gallagher <github.com/ogallagher>
 * @since 9 June 2021
 * @version 0.0.2
 */
public class MarketSense {
	private static final String NAME = "MarketSense";
	
	public static boolean runTests = false;
	
	private static final URL PROPERTIES_FILE = MarketSense.class.getResource("resources/config.properties");
	private static Properties properties;
	
	public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat(); 
	
	private static final String PROP_TWELVEDATA_API_KEY = "twelvedata_api_key";
	private static final String PROP_PERSIST_UNIT = "persistence_unit";
	private static final String PROP_RUN_TESTS = "run_tests";
	
	private static TwelvedataClient tdclient = null;
	
	public static EntityManager dbManager = null;
	
	private static Person person = null;
	
	static {
		FLOAT_FORMAT.setMaximumFractionDigits(2);
	}
	
	/**
	 * Program entrypoint.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// launch gui, which in turn calls MarketSenseGUI.start
		MarketSenseGUI.main(args);
	}
	
	/**
	 * Handles any initialization that can be kept apart from the javafx gui application thread.
	 * 
	 * @author Owen Gallagher
	 *
	 */
	public static class MarketSenseInit implements Runnable {
		@Override
		public void run() {
			try {
				properties = getProperties();
				System.out.println("found " + properties.size() + " program properties");
			}
			catch (FileNotFoundException e) {
				properties = new Properties();
				System.out.println(e.getMessage());
			}
			
			// decide whether to run tests
			runTests = Boolean.valueOf(properties.getProperty(PROP_RUN_TESTS, "false"));
			
			// connect twelvedata client
//			tdclient = new TwelvedataClient(properties.getProperty(PROP_TWELVEDATA_API_KEY, null));
		}
	}
	
	public static class MarketSenseGUI extends Application {
		private static Stage mainWindow = null;
		private static int MAIN_WINDOW_WIDTH_INIT = 600;
		private static int MAIN_WINDOW_HEIGHT_INIT = 500;
		
		public static void main(String[] args) {
			launch(args);
		}
		
		@Override
		public void start(Stage primaryStage) throws Exception {
			System.out.println("MarketSense.start start");
			
			// non-gui initialization
			new Thread(new MarketSenseInit()).start();
			
			// main window
			mainWindow = primaryStage;			
			mainWindow.setWidth(MAIN_WINDOW_WIDTH_INIT);
			mainWindow.setHeight(MAIN_WINDOW_HEIGHT_INIT);
			mainWindow.setTitle(NAME);
			mainWindow.centerOnScreen();
			mainWindow.show();
			
			Scene mainScene = new Scene(
				(Parent) FXMLLoader.load(MarketSense.class.getResource("resources/MainWindow.fxml"))
			);
			mainWindow.setScene(mainScene);
			
			// connect db entity manager
			dbManager = Persistence
			.createEntityManagerFactory(properties.getProperty(PROP_PERSIST_UNIT))
			.createEntityManager();
			
			// run tests
			if (runTests) {
				runTests(false, false, false);
			}
			
			// load login form
			loadPeople(ShowLogin.class, true);
		}
		
		/**
		 * Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 *
		 */
		public static class ShowLogin implements Runnable {
			private List<Person> people;
			
			public ShowLogin(List<Person> people) {
				this.people = people;
			}
			
			@Override
			public void run() {
				Scene mainScene = mainWindow.getScene();
				
				try {
					Pane contentPane = (Pane) mainScene.lookup("#content");
					ObservableList<Node> content = contentPane.getChildren();
					content.clear();
					content.add(
						(Node) FXMLLoader.load(MarketSense.class.getResource("resources/LoginForm.fxml"))
					);
					
					// handle login via text field
					final TextField loginUsername = (TextField) mainScene.lookup("#loginUsername");
					loginUsername.setOnKeyReleased(new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							if (event.getCode().equals(KeyCode.ENTER)) {
								login(loginUsername.getText());
							}
						}
					});
					
					// handle login via user selection
					@SuppressWarnings("unchecked")
					final ListView<Person> registeredUsers = (ListView<Person>) mainScene.lookup("#registeredUsers");
					registeredUsers.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
					
					ObservableList<Person> people = FXCollections.observableList(this.people);
					registeredUsers.setItems(people);
					registeredUsers.setCellFactory(new Callback<ListView<Person>, ListCell<Person>>() {
						@Override
						public ListCell<Person> call(ListView<Person> param) {
							return new Person.PersonListCell();
						}
					});
					
					registeredUsers.setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							Person person = registeredUsers.getSelectionModel().getSelectedItem();
							
							if (person != null) {
								login(person.getUsername());
							}
							else {
								System.out.println("no person selected");
							}
						}
					});
				}
				catch (IOException e) {
					System.out.println("error showing login: " + e.getMessage());
				}
			}
		}
		
		public static class ShowDashboard implements Runnable {
			@Override
			public void run() {
				Scene mainScene = mainWindow.getScene();
				
				try {
					Pane contentPane = (Pane) mainScene.lookup("#content");
					ObservableList<Node> content = contentPane.getChildren();
					content.clear();
					
					content.add(
						(Node) FXMLLoader.load(MarketSense.class.getResource("resources/Dashboard.fxml"))
					);
					
					// fill in user stats
					if (person != null) {
						Label nameLabel = (Label) mainScene.lookup("#userName");
						nameLabel.setText(person.getUsername());
						
						Label accuracyLabel = (Label) mainScene.lookup("#userAccuracy");
						accuracyLabel.setText(String.valueOf(person.getAccuracy()));
						
						Label sinceLabel = (Label) mainScene.lookup("#userSince");
						sinceLabel.setText(person.getSince().toString());
					}
					
					// enable logout
					Button logoutButton = (Button) mainScene.lookup("#logout");
					logoutButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							logout();
						}
					});
				} 
				catch (IOException e) {
					System.out.println("error showing dashboard: " + e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Loads the entities from the person table to compile a list of known accounts.
	 * 
	 * @param <T>
	 * @param OnLoad The callback to which the list of people is passed.
	 * @param guiThread Whether to use the javafx thread for the callback.
	 */
	public static <T extends Runnable> void loadPeople(Class<T> OnLoad, boolean guiThread) {
		System.out.println("loading people from local db");
		
		@SuppressWarnings("unchecked")
		List<Person> people = (List<Person>) dbManager
		.createQuery("select p from " + Person.DB_TABLE + " p")
		.getResultList();
		System.out.println("loaded " + people.size() + " people from db");
		
		try {
			Runnable runnable = OnLoad.getDeclaredConstructor(List.class).newInstance(people);
			
			if (guiThread) {
				Platform.runLater(runnable);
			}
			else {
				new Thread(runnable).start();
			}
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.out.println("error loading people: " + e.getMessage());
		}
	}
	
	/**
	 * Set the current person according to the given username. If that person is not already in the db, it's first
	 * persisted. If there is already someone logged in, they are first logged out.
	 * 
	 * @param username The username of the person to log in as.
	 */
	public static void login(String username) {
		System.out.println("log in as " + username);
		
		if (person != null) {
			logout();
		}
		
		person = dbManager.find(Person.class, username);
		if (person == null) {
			System.out.println("person " + username + " is not yet registered; creating new person");
			
			person = new Person(username);
			// only needs to be called once; the program object will be persisted whenever a persistent field is changed
			dbManager.getTransaction().begin();
			dbManager.persist(person);
			dbManager.getTransaction().commit();
		}
		
		// load dashboard
		Platform.runLater(new MarketSenseGUI.ShowDashboard());
	}
	
	/**
	 * If signed in, log out by removing the reference to that person object. Then, return to the
	 * login form.
	 */
	public static void logout() {
		if (person != null) {
			// sign out
			person = null;
		}
		else {
			System.out.println("already logged out");
		}
		
		// show login
		loadPeople(MarketSenseGUI.ShowLogin.class, true);
	}
	
	/**
	 * Run designated tests.
	 * 
	 * @param base Basic test.
	 * @param database Database test. 
	 * @param marketSynth Market sound synth test.
	 */
	public static void runTests(boolean base, boolean database, boolean marketSynth) {
		System.out.println("running tests");
		
		if (base) {
			new Test().evaluate(false);
		}
		if (database) {
			new TestDatabase(dbManager).evaluate(true);
		}
		if (marketSynth) {
			new TestMarketSynth().evaluate(false);
		}
	}
	
	private static Properties getProperties() throws FileNotFoundException {
		Properties properties = new Properties();
		
		FileInputStream istream = new FileInputStream(PROPERTIES_FILE.getPath());
		
		try {
			properties.load(istream);
			return properties;
		} 
		catch (IOException e) {
			throw new FileNotFoundException("failed to read properties file " + PROPERTIES_FILE);
		}
	}
}
