package ogallagher.marketsense;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sound.sampled.AudioInputStream;

import org.h2.command.dml.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;

import ogallagher.twelvedata_client_java.TwelvedataClient;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;
import ogallagher.twelvedata_client_java.TwelvedataInterface.SecuritySet;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.SecurityId;
import ogallagher.marketsense.persistent.SecurityType;
import ogallagher.marketsense.persistent.TrainingSession;
import ogallagher.marketsense.persistent.TrainingSessionType;
import ogallagher.marketsense.test.Test;
import ogallagher.marketsense.test.TestDatabase;
import ogallagher.marketsense.test.TestMarketSynth;
import ogallagher.temp_fx_logger.System;

/**
 * @author Owen Gallagher <github.com/ogallagher>
 * @since 9 June 2021
 * @version 0.0.3
 */
public class MarketSense {
	private static final String NAME = "MarketSense";
	public static boolean runTests = false;
	
	private static final URL PROPERTIES_FILE = MarketSense.class.getResource("resources/config.properties");
	private static final URL SYMBOLS_FILE_NYSE = MarketSense.class.getResource("resources/markets/NYSE_symbols_all.txt");
	private static final URL SYMBOLS_FILE_NASDAQ = MarketSense.class.getResource("resources/markets/NASDAQ_symbols_all.txt");
	
	private static Properties properties;
	
	public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat(); 
	
	private static final String PROP_TWELVEDATA_API_KEY = "twelvedata_api_key";
	private static final String PROP_PERSIST_UNIT = "persistence_unit";
	
	private static final String PROP_RUN_TESTS = "run_tests";
	
	private static final String PROP_TRAIN_SYMBOL = "train_symbol";
	private static final String PROP_TRAIN_BAR_WIDTH = "train_bar_width";
	private static final String PROP_TRAIN_SAMPLE_SIZE = "train_sample_size";
	private static final String PROP_TRAIN_SAMPLE_COUNT = "train_sample_count";
	private static final String PROP_TRAIN_LOOKBACK_MAX_MONTHS = "train_lookback_max_months";
	
	private static TwelvedataClient tdclient = null;
	
	public static EntityManager dbManager = null;
	
	private static Person person = null;
	
	private static MarketSynth marketSynth = null;
	
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
			tdclient = new TwelvedataClient(properties.getProperty(PROP_TWELVEDATA_API_KEY, null));
			
			// create market synth with selected timbre and amplitude formulas
			marketSynth = new MarketSynth(MarketSynth.TimbreFormula.HARMONIC_REDOI, MarketSynth.AmplitudeFormula.CONST);
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
			private static ComboBox<String> symbolDropdown = null;
			
			@Override
			public void run() {
				Scene mainScene = mainWindow.getScene();
				
				try {
					Pane contentPane = (Pane) mainScene.lookup("#content");
					ObservableList<Node> content = contentPane.getChildren();
					content.clear();
					
					Node dashboard = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/Dashboard.fxml")); 
					content.add(dashboard);
					
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
					logoutButton.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							logout();
						}
					});
					
					// fill in training session history TODO
					
					// fill in new training session form
					loadNewTrainingSessionForm(dashboard);
				} 
				catch (IOException e) {
					System.out.println("error showing dashboard: " + e.getMessage());
				}
			}
			
			@SuppressWarnings("unchecked")
			private void loadNewTrainingSessionForm(Node dashboard) {
				// symbol
				symbolDropdown = (ComboBox<String>) dashboard.lookup("#trainSymbol");
				symbolDropdown.setValue(properties.getProperty(PROP_TRAIN_SYMBOL, "AAPL"));
				
				loadSymbols(ShowSymbols.class, true);
				
				// bar widths
				ComboBox<String> barWidthDropdown = (ComboBox<String>) dashboard.lookup("#trainBarWidth");
				barWidthDropdown.setValue(properties.getProperty(PROP_TRAIN_BAR_WIDTH, BarInterval.DY_1));
				
				// current supported bar widths between 1 hour and 1 day
				HashSet<String> barWidths = new HashSet<String>();
				for (String width : new String[] {
					BarInterval.HR_1,
					BarInterval.HR_2,
					BarInterval.HR_4,
					BarInterval.HR_8,
					BarInterval.DY_1
				}) {
					barWidths.add(width);
				}
				
				barWidthDropdown.setItems(FXCollections.observableList(new ArrayList<String>(barWidths)));
				
				// sample sizes, number of trade bars per sample
				ComboBox<Integer> sampleSizeDropdown = (ComboBox<Integer>) dashboard.lookup("#trainSampleSize");
				
				// current supported sample sizes
				HashSet<Integer> sampleSizes = new HashSet<Integer>();
				sampleSizes.add(Integer.valueOf(properties.getProperty(PROP_TRAIN_SAMPLE_SIZE, "7")));
				for (Integer size : new Integer[] {
					7, 10, 15, 20, 30
				}) {
					sampleSizes.add(size);
				}
				
				sampleSizeDropdown.setItems(FXCollections.observableList(
					// convert to list
					new ArrayList<Integer>(
						// sort with TreeSet constructor
						new TreeSet<Integer>(sampleSizes)
					)));
				
				// max lookback
				TextField maxLookback = (TextField) dashboard.lookup("#trainMaxLookback");
				maxLookback.setText(properties.getProperty(PROP_TRAIN_LOOKBACK_MAX_MONTHS, "24"));
				
				// sample count
				TextField sampleCount = (TextField) dashboard.lookup("#trainSampleCount");
				sampleCount.setText(properties.getProperty(PROP_TRAIN_SAMPLE_COUNT, "30"));
				
				// train button
				Button trainButton = (Button) dashboard.lookup("#trainButton");
				trainButton.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						newTrainingSession(
							symbolDropdown.getValue(), 
							barWidthDropdown.getValue(), 
							sampleSizeDropdown.getValue(), 
							Integer.parseInt(sampleCount.getText()), 
							Integer.parseInt(maxLookback.getText())
						);
					}
				});
			}
			
			/**
			 * Symbols load from files is done on separate thread, and load into dropdown options
			 * is done in this callback, on the gui thread.
			 * 
			 * @author Owen Gallagher
			 *
			 */
			public static class ShowSymbols implements Runnable {
				private HashSet<String> symbols;
				
				public ShowSymbols(HashSet<String> symbols) {
					this.symbols = symbols;
				}
				
				@Override
				public void run() {
					symbolDropdown.setItems(FXCollections.observableList(
						// convert to list
						new ArrayList<String>(
							// sort with TreeSet constructor
							new TreeSet<String>(symbols)
						)));
				}
			}
		}
		
		public static class ShowTrainingSession implements Runnable {
			private TrainingSession session;
			
			public ShowTrainingSession(TrainingSession session) {
				this.session = session;
			}
			
			@Override
			public void run() {
				// switch to training session scene
				Scene mainScene = mainWindow.getScene();
				
				Pane contentPane = (Pane) mainScene.lookup("#content");
				ObservableList<Node> content = contentPane.getChildren();
				content.clear();
				
				try {
					Node sessionRoot = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/TrainingSession.fxml"));
					content.add(sessionRoot);
					System.out.println("DEBUG added training session root to content");
					
					// enable quit/abort
					Button abort = (Button) sessionRoot.lookup("#quit");
					abort.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							abortTrainingSession();
						}
					});
					System.out.println("DEBUG enabled abort button");
					
					// load sample id (should be zero)
					Label sampleId = (Label) sessionRoot.lookup("#sampleId");
					session.getSampleIdProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
							// update sample id label
							sampleId.setText(Integer.toString(nv.intValue() + 1));
							
							// swap next sample button for finish session button if last sample
							if (session.getSampleIdProperty().get() == session.getSampleCount()-1) {
								Button nextSample = (Button) sessionRoot.lookup("#nextSample");
								nextSample.setText("finish session");
								nextSample.setOnAction(new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent event) {
										commitTrainingSession(session);
									}
								});
							}
						}
					});
					System.out.println("DEBUG bound sample id");
					
					// load overall score (should be 0.5)
					Label score = (Label) sessionRoot.lookup("#score");
					session.getScoreProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
							score.setText(Float.toString(nv.floatValue() * 100));
						}
					});
					System.out.println("DEBUG loaded average score");
					
					// load config params
					loadConfig(sessionRoot);
					
					// load training controls
					loadControls(sessionRoot);
					
					// prepare first sample
					session.nextSample(dbManager, marketSynth);
				} 
				catch (IOException e) {
					System.out.println("failed to load training session: " + e.getMessage());
				}
			}
			
			/**
			 * Load training session config into corresponding widgets.
			 */
			private void loadConfig(Node sessionRoot) {
				// security
				Label security = (Label) sessionRoot.lookup("#security");
				security.setText(session.getSecurity().getId().toString());
				
				// sample count
				Label sampleCount = (Label) sessionRoot.lookup("#sampleCount");
				sampleCount.setText(Integer.toString(session.getSampleCount()));
				
				// sample size
				Label sampleSize = (Label) sessionRoot.lookup("#sampleSize");
				sampleSize.setText(Integer.toString(session.getSampleSize()));
				
				// bar width
				Label barWidth = (Label) sessionRoot.lookup("#barWidth");
				barWidth.setText(session.getBarWidth());
				
				System.out.println("DEBUG loaded session config");
			}
			
			/**
			 * Load training session controls.
			 */
			private void loadControls(Node sessionRoot) {
				// play sound
				Button playSound = (Button) sessionRoot.lookup("#playSound");
				playSound.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						AudioInputStream sound = session.getSample().getSound();
						marketSynth.playback(sound, 1);
					}
				});
				
				// guess color
				Slider colorGuess = (Slider) sessionRoot.lookup("#colorGuess");
				colorGuess.setMinorTickCount(0);
				
				Region colorGuessTile = (Region) sessionRoot.lookup("#colorGuessTile");
				ChangeListener<Number> guessListener = new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
						// update color guess tile
						Color color = MarketSample.valueToColor(nv.doubleValue(), MarketSample.COLOR_LOW, MarketSample.COLOR_HIGH);
						colorGuessTile.setBackground(new Background(new BackgroundFill(
							javafx.scene.paint.Color.rgb(color.getRed(), color.getGreen(), color.getBlue()), 
							CornerRadii.EMPTY, 
							Insets.EMPTY
						)));
					}
				};
				colorGuess.valueProperty().addListener(guessListener);
				guessListener.changed(null, null, colorGuess.valueProperty().getValue());
				
				// next sample (enabled on color guess)
				Button nextSample = (Button) sessionRoot.lookup("#nextSample");
				nextSample.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						// next sample
						if (session.nextSample(dbManager, marketSynth) != null) {
							// reset last score
							((Label) sessionRoot.lookup("#scoreLast")).setText("0.0");
							
							// disable next button
							nextSample.setDisable(true);
							
							// enable color guess submit
							((Button) sessionRoot.lookup("#colorGuessSubmit")).setDisable(false);
							
							// reset color guess
							colorGuess.setValue(0.5);
						}
						// else, training session already complete
						else {
							System.out.println(
								"ERROR: training session of " + session.getSampleCount() + 
								" samples requested sample " + session.getSampleIdProperty().get()
							);
						}
					}
				});
				
				// submit guess color
				Button colorGuessSubmit = (Button) sessionRoot.lookup("#colorGuessSubmit");
				colorGuessSubmit.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						MarketSample sample = session.getSample();
						
						// disable color guess submit
						colorGuessSubmit.setDisable(true);
						
						// load true color
						Slider colorTrue = (Slider) sessionRoot.lookup("#colorTrue");
						colorTrue.setValue(sample.getFutureMovement());
						
						Region colorTrueTile = (Region) sessionRoot.lookup("#colorTrueTile");
						Color color = sample.getColor();
						colorTrueTile.setBackground(new Background(new BackgroundFill(
							javafx.scene.paint.Color.rgb(color.getRed(), color.getGreen(), color.getBlue()), 
							CornerRadii.EMPTY, 
							Insets.EMPTY
						)));
						
						// score training guess
						double guess = colorGuess.getValue();
						double score = sample.evalGuess(guess);
						
						// update last score
						Label scoreLast = (Label) sessionRoot.lookup("#scoreLast");
						scoreLast.setText(Float.toString((float) score * 100));
						
						// update average score
						session.updateScore(score);
						
						// enable next sample button
						nextSample.setDisable(false);
					}
				});
				
				System.out.println("DEBUG loaded session controls");
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
	 * Loads all supported asset symbols from resource files: {@link SYMBOLS_FILE_NYSE}, {@link SYMBOLS_FILE_NASDAQ}.
	 * 
	 * @param <T>
	 * @param OnLoad The callback to which the {@code HashSet<String>} of symbols is passed.
	 * @param guiThread Whether the callback needs to be executed on the gui thread.
	 */
	public static <T extends Runnable> void loadSymbols(Class<T> OnLoad, boolean guiThread) {
		System.out.println("loading symbols from resource files");
		
		HashSet<String> allSymbols = new HashSet<>();
		
		// read symbols from resources
		for (URL url : new URL[] {
			SYMBOLS_FILE_NYSE,
			SYMBOLS_FILE_NASDAQ
		}) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(url.getPath()));
				Iterator<String> line = reader.lines().iterator();
				
				// skip header
				line.next();
				
				while (line.hasNext()) {
					String[] symbolName = line.next().split("\\t");
					allSymbols.add(symbolName[0]);
				}
				
				reader.close();
			}
			catch (IOException e) {
				System.out.println("failed to read symbols list from " + url.getPath());
			}
		}
		
		try {
			Runnable runnable = OnLoad.getDeclaredConstructor(HashSet.class).newInstance(allSymbols);
			
			if (guiThread) {
				Platform.runLater(runnable);
			}
			else {
				new Thread(runnable).start();
			}
		} 
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.out.println("error loading symbols: " + e.getMessage());
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
	 * Open a new training session using the parameters selected in the new training session form.
	 */
	public static boolean newTrainingSession(String symbol, String barWidth, int sampleSize, int sampleCount, int maxLookbackMonths) {
		Security security = null;
		
		// try to fetch security from db
		Security dbSecurity = (Security) 
		dbManager.createQuery(
			"select s from " + Security.DB_TABLE + " s " + 
			"where s." + Security.DB_COL_ID + "." + SecurityId.DB_COL_SYMBOL + " = :symbol"
		)
		.setParameter("symbol", symbol)
		.setMaxResults(1)
		.getSingleResult();
		
		if (dbSecurity != null) {
			security = dbSecurity;
		}
		else {
			// try to use twelvedata api lookup to determine security specs from symbol if not in db
			SecuritySet securitySet = tdclient.symbolLookup(symbol, 10);
			SecuritySet.Security tdSecurity = null;
			
			for (SecuritySet.Security candidate : securitySet.data) {
				if (candidate.symbol.equals(symbol) && (candidate.exchange.equals("NYSE") || candidate.exchange.equals("NASDAQ"))) {
					tdSecurity = candidate;
					break;
				}
				else {
					System.out.println("skip candidate " + candidate);
				}
			}
			
			if (tdSecurity != null) {
				SecurityType securityType = SecurityType.STOCK;
				switch (tdSecurity.instrument_type) {
					case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.ETF:
						securityType = SecurityType.ETF;
						break;
				
					case ogallagher.twelvedata_client_java.TwelvedataInterface.SecurityType.COMMON_STOCK:
						securityType = SecurityType.STOCK;
						break;
				}
				
				security = new Security(tdSecurity.symbol, tdSecurity.exchange, securityType);
			}
		}
		
		if (security != null) {
			TrainingSession session = new TrainingSession(
				person, TrainingSessionType.TBD, 
				security, barWidth, sampleSize, sampleCount, maxLookbackMonths
			);
			
			System.out.println("starting a new training session " + session);
			
			// prepare the database
			if (session.collectMarketUniverse(dbManager,tdclient)) {
				System.out.println("market data universe acquired for lookback of " + session.getMaxLookbackMonths() + " months");
				
				// show training session interface
				Platform.runLater(new MarketSenseGUI.ShowTrainingSession(session));
			}
			else {
				System.out.println("ERROR failed to creake market data universe for training session");
			}
			
			return true;
		}
		else {
			System.out.println("ERROR failed to find security " + symbol + " for training session");
			return false;
		}
	}
	
	/**
	 * Abort the incomplete training session without committing it, and return to the dashboard.
	 */
	public static void abortTrainingSession() {
		Platform.runLater(new MarketSenseGUI.ShowDashboard());
	}
	
	/**
	 * Commit a completed training session to the database and return to the dashboard.
	 * 
	 * @param session The completed training session.
	 */
	public static void commitTrainingSession(TrainingSession session) {
		// complete the training session by attempting to request another sample
		session.nextSample(null, null);
		
		if (session.getCompleteProperty().get()) {
			// commit the completed training session to the database
			TrainingSession.persist(session, dbManager);
			System.out.println("INFO committed new training session of duration " + session.getDuration().toString());
			
			// return to dashboard
			Platform.runLater(new MarketSenseGUI.ShowDashboard());
		}
		else {
			System.out.println("ERROR not allowed to commit an incomplete training session");
		}
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
