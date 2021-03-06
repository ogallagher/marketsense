package ogallagher.marketsense;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.sound.sampled.AudioInputStream;

import com.fxgraph.cells.CartesianPoint;
import com.fxgraph.graph.CartesianGraph;
import com.fxgraph.graph.PannableCanvas;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import ogallagher.twelvedata_client_java.TwelvedataClient;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;
import ogallagher.twelvedata_client_java.TwelvedataInterface.Failure;
import ogallagher.twelvedata_client_java.TwelvedataInterface.SecuritySet;
import ogallagher.marketsense.PerformanceSample.PerformancePoint;
import ogallagher.marketsense.persistent.Person;
import ogallagher.marketsense.persistent.Security;
import ogallagher.marketsense.persistent.SecurityId;
import ogallagher.marketsense.persistent.SecurityType;
import ogallagher.marketsense.persistent.TradeBar;
import ogallagher.marketsense.persistent.TrainingSession;
import ogallagher.marketsense.persistent.TrainingSessionId;
import ogallagher.marketsense.persistent.TrainingSessionType;
import ogallagher.marketsense.test.Test;
import ogallagher.marketsense.test.TestDatabase;
import ogallagher.marketsense.test.TestDatetimeUtils;
import ogallagher.marketsense.test.TestMarketSynth;
import ogallagher.marketsense.util.HasCallback;
import ogallagher.marketsense.util.PointFilter;
import ogallagher.marketsense.widgets.AccountComboBox;
import ogallagher.marketsense.widgets.BarWidthComboBox;
import ogallagher.marketsense.widgets.MultiDatePicker;
import ogallagher.marketsense.widgets.SampleSizeComboBox;
import ogallagher.marketsense.widgets.SymbolComboBox;
import ogallagher.temp_fx_logger.System;

/**
 * Main program entrypoint for marketsense.
 * 
 * @author <a href="https://github.com/ogallagher">Owen Gallagher</a>
 * @since 9 June 2021
 * @version {@value #VERSION}
 */
public class MarketSense {
	private static final String NAME = "MarketSense";
	public static boolean runTests = false;
	
	/**
	 * Program version string.
	 */
	public static final String VERSION = "0.1.4";
	
	/**
	 * Path to program parent directory. In development, this is the parent folder of the one containing 
	 * the {@code .class} files. In production, this is the folder containing the {@code .jar} file.
	 */
	private static File PARENT_DIR;
	/**
	 * The read-only resources directory.
	 * 
	 * @see #RESOURCES_DIR_RW
	 */
	private final static String RESOURCES_DIR_R = "resources/";
	/**
	 * The read-write resources directory.<br><br>
	 * 
	 * In development, the {@code resources/} directory with program input and output files is in the same location as
	 * the generated class files. In production, however, these will be packaged in a {@code .jar} file, which is read-only,
	 * so the files that marketsense can write to must be in a different location (jar's parent directory).
	 * 
	 * @see #RESOURCES_DIR_R
	 */
	private static File RESOURCES_DIR_RW;
	
	/**
	 * Path to program properties file.
	 */
	private static File PROPERTIES_FILE;
	/**
	 * The markets resources directory name.
	 */
	private static final String MARKETS_DIR = "markets/";
	/**
	 * Path to NYSE symbols list.
	 */
	private static File SYMBOLS_NYSE_FILE;
	/**
	 * Path to NASDAQ symbols list.
	 */
	private static File SYMBOLS_NASDAQ_FILE;
	
	/**
	 * Program properties, loaded from the {@link #PROPERTIES_FILE properties file}.
	 */
	private static Properties properties;
	
	/**
	 * Defines how float numeric values should be displayed, namely with a certain number of digits after
	 * the decimal point. 
	 */
	public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat(); 
	
	/**
	 * Program properties key for a twelvedata api key.
	 */
	public static final String PROP_TWELVEDATA_API_KEY = "twelvedata_api_key";
	/**
	 * Program properties key for the name of the persistence unit, which should usually be left alone.
	 */
	public static final String PROP_PERSIST_UNIT = "persistence_unit";
	
	/**
	 * Program properties key for whether to run unit tests prior to running the program.
	 */
	public static final String PROP_RUN_TESTS = "run_tests";
	
	/**
	 * Program properties key for whether to save sounds generated with the {@link #marketSynth}.
	 */
	public static final String PROP_SAVE_SOUNDS = "save_sounds";
	
	/**
	 * Program properties key for the default training session asset symbol.
	 */
	public static final String PROP_TRAIN_SYMBOL = "train_symbol";
	/**
	 * Program properties key for the default training session tradebar width.
	 */
	public static final String PROP_TRAIN_BAR_WIDTH = "train_bar_width";
	/**
	 * Program properties key for the default training session sample size.
	 */
	public static final String PROP_TRAIN_SAMPLE_SIZE = "train_sample_size";
	/**
	 * Program properties key for the default training session sample count.
	 */
	public static final String PROP_TRAIN_SAMPLE_COUNT = "train_sample_count";
	/**
	 * Program properties key for the default training session lookback, in months.
	 */
	public static final String PROP_TRAIN_LOOKBACK_MAX_MONTHS = "train_lookback_max_months";
	
	/**
	 * The program twelvedata api client.
	 */
	private static TwelvedataClient tdclient = null;
	
	/**
	 * The program database/persistence entity manager.
	 */
	public static EntityManager dbManager = null;
	
	/**
	 * The current active account.
	 */
	private static Person person = null;
	
	/**
	 * The program market data sound synthesizer.
	 */
	private static MarketSynth marketSynth = null;
	
	// static member initialization
	static {
		FLOAT_FORMAT.setMaximumFractionDigits(2);
		
		try {
			// determine resource locations
			PARENT_DIR = new File(MarketSense.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			System.out.println("DEBUG marketsense parent dir = " + PARENT_DIR.getPath());
			
			RESOURCES_DIR_RW = new File(PARENT_DIR, RESOURCES_DIR_R);
			// ensure rw resources exists
			if (RESOURCES_DIR_RW.mkdir()) {
				System.out.println("INFO created new rw resources dir at " + RESOURCES_DIR_RW.getPath());
			}
			else {
				System.out.println("DEBUG found existing rw resources dir at " + RESOURCES_DIR_RW.getPath());
			}
			
			// rw resource locations
			String propertiesFile = "config.properties";
			
			if (MarketSense.class.getResource(RESOURCES_DIR_R + propertiesFile) != null) {
				// readable config.properties exists; create writable copy
				createWritableCopy(propertiesFile);
			}
			else {
				// config.properties not found in readable resources; copy from config_dummy.properties
				String dummyPropertiesFile = "config_dummy.properties";
				System.out.println(
					"WARNING read-only " + propertiesFile + " not found, attempting creation from " + dummyPropertiesFile
				);
				
				createWritableCopy(dummyPropertiesFile, propertiesFile);
			}
			
			// result should have a writable config properties file in resources/
			PROPERTIES_FILE = new File(RESOURCES_DIR_RW, propertiesFile);
			
			// create rw resources/markets
			File marketsDirRW = new File(RESOURCES_DIR_RW, MARKETS_DIR);
			marketsDirRW.mkdir();
			
			// asset symbols lists
			String symbolsNYSEFile = MARKETS_DIR + "NYSE_symbols.txt";
			createWritableCopy(symbolsNYSEFile);
			SYMBOLS_NYSE_FILE = new File(RESOURCES_DIR_RW, symbolsNYSEFile);
			
			String symbolsNASDAQFile = MARKETS_DIR + "NASDAQ_symbols.txt";
			createWritableCopy(symbolsNASDAQFile);
			SYMBOLS_NASDAQ_FILE = new File(RESOURCES_DIR_RW, symbolsNASDAQFile);
		}
		catch (URISyntaxException e) {
			System.out.println("ERROR failed to load resource dir: " + e.getMessage());
			Platform.exit();
		}
	}
	
	/**
	 * Create a writable copy of a read-only file at {@code resources/<relReadablePath>}.
	 * 
	 * @param relReadablePath Relative path within readable res dir to file source.
	 * @param relWritablePath Relative path within writable res dir to file destination.
	 * 
	 * @return {@code true} if the copy was created or if the copy already exists.
	 */
	private static boolean createWritableCopy(String relReadablePath, String relWritablePath) {
		InputStream readable = MarketSense.class.getResourceAsStream(RESOURCES_DIR_R + relReadablePath);
		File writable = new File(RESOURCES_DIR_RW, relWritablePath);
		
		try {
			if (!writable.exists()) {
				FileOutputStream writer = new FileOutputStream(writable);
				byte[] buffer = new byte[1024];
				int len = readable.read(buffer);
				
				while (len != -1) {
					writer.write(buffer, 0, len);
					len = readable.read(buffer);
				}
				
				writer.close();
				readable.close();
				
				System.out.println("INFO created new writable copy at " + writable.getPath());
			}
			else {
				System.out.println("DEBUG writable already exists at " + writable.getPath());
			}
			
			return true;
		}
		catch (IOException e) {
			System.out.println("ERROR failed to find original to create writable copy of resources/" + relReadablePath);
			System.out.println("ERROR " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Convenience method for {@link #createWritableCopy(String, String) createWritableCopy(relPath,relPath)}.
	 * 
	 * @param relPath Relative path within resources dir.
	 * 
	 * @return {@code true} if the copy was created or if the copy already exists.
	 */
	private static boolean createWritableCopy(String relPath) {
		return createWritableCopy(relPath, relPath);
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
	 */
	public static class MarketSenseInit implements Runnable {
		@Override
		public void run() {
			// load properties
			try {
				properties = getProperties();
				System.out.println("DEBUG found " + properties.size() + " program properties");
			}
			catch (FileNotFoundException e) {
				properties = new Properties();
				System.out.println("ERROR " + e.getMessage());
			}
			
			// decide whether to run tests
			runTests = Boolean.valueOf(properties.getProperty(PROP_RUN_TESTS, "false"));
			
			// connect twelvedata client
			tdclient = new TwelvedataClient(properties.getProperty(PROP_TWELVEDATA_API_KEY, null));
			
			// create market synth with selected timbre and amplitude formulas
			marketSynth = new MarketSynth(MarketSynth.TimbreFormula.HARMONIC_REDOI, MarketSynth.AmplitudeFormula.CONST);
		}
	}
	
	/**
	 * Encapsulates the application's graphical interface, including a number of runnables designed to be run
	 * on the javafx app thread.
	 * 
	 * @author Owen Gallagher
	 * @since 9 June 2021
	 */
	public static class MarketSenseGUI extends Application {
		private static Stage mainWindow;
		private static int MAIN_WINDOW_WIDTH_INIT = 800;
		private static int MAIN_WINDOW_HEIGHT_INIT = 510;
		
		private static MenuItem consoleMenuItem;
		private static MenuItem dashboardMenuItem;
		private static MenuItem performanceViewMenuItem;
		
		/**
		 * Provide static access to the {@code HostServices} instance of the latest {@code MarketSenseGUI} launched.
		 */
		private static HostServices hostServices;
		
		public static void main(String[] args) {
			launch(args);
		}
		
		@Override
		public void start(Stage primaryStage) throws Exception {
			System.out.println("MarketSense.start start");
			
			// non-gui initialization TODO move outside gui
			new Thread(new MarketSenseInit()).start();
			
			// main window
			mainWindow = primaryStage;			
			mainWindow.setWidth(MAIN_WINDOW_WIDTH_INIT);
			mainWindow.setHeight(MAIN_WINDOW_HEIGHT_INIT);
			mainWindow.setTitle(NAME);
			mainWindow.centerOnScreen();
			mainWindow.show();
			
			Scene mainScene = new Scene(
				(Parent) FXMLLoader.load(MarketSense.class.getResource(RESOURCES_DIR_R + "MainWindow.fxml"))
			);
			mainWindow.setScene(mainScene);
			
			// end program on main window close
			mainWindow.setOnHidden(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					Platform.exit();
				}
			});
			
			// menu bar controls
			Menu viewMenu = new Menu("view");
			
			consoleMenuItem = new MenuItem("console window");
			consoleMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					Stage consoleWindow = System.getConsoleWindow();
					
					if (consoleWindow.isShowing()) {
						consoleWindow.hide();
					}
					else {
						consoleWindow.show();
					}
				}
			});
			
			// navigate to dashboard
			dashboardMenuItem = new MenuItem("dashboard");
			dashboardMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					Platform.runLater(new ShowDashboard());
				}
			});
			
			// navigate to performance view
			performanceViewMenuItem = new MenuItem("performance");
			performanceViewMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					Platform.runLater(new ShowPerformanceView());
				}
			});
			
			viewMenu.getItems().addAll(
				consoleMenuItem, 
				dashboardMenuItem, 
				performanceViewMenuItem
			);
			
			MenuBar menuBar = (MenuBar) mainScene.lookup("#mainMenuBar");
			menuBar.getMenus().add(viewMenu);
			
			// host services
			hostServices = getHostServices();
			
			// connect db entity manager
			dbManager = Persistence
			.createEntityManagerFactory(properties.getProperty(PROP_PERSIST_UNIT))
			.createEntityManager();
			
			// run tests
			if (runTests) {
				runTests(false, false, false, false);
			}
			
			// load login form
			loadPeopleCallback(ShowLogin.class, true);
		}
		
		@Override
		public void stop() {
			// TODO handle program exit
		}
		
		/**
		 * Open login form in main window. Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 * @since 2 June 2021
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
					
					// update main menubar navigation
					dashboardMenuItem.setDisable(true);
					performanceViewMenuItem.setDisable(false);
					
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
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Open the dashboard in the main window. Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 * @since June 2021
		 */
		public static class ShowDashboard implements Runnable {
			private static ComboBox<String> symbolDropdown = null;
			private static ListView<TrainingSession> trainingSessionsList = null;
			
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				if (person != null) {
					Scene mainScene = mainWindow.getScene();
					
					try {
						Pane contentPane = (Pane) mainScene.lookup("#content");
						ObservableList<Node> content = contentPane.getChildren();
						content.clear();
						
						Node dashboard = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/Dashboard.fxml")); 
						content.add(dashboard);
						
						// update main menubar navigation
						dashboardMenuItem.setDisable(true);
						performanceViewMenuItem.setDisable(false);
						
						// trim text fields
						for (Node tfn : dashboard.lookupAll("TextField")) {
							TextField tf = (TextField) tfn;
							tf.setPrefColumnCount(4);
						}
						
						// fill in user stats
						Label nameLabel = (Label) dashboard.lookup("#userName");
						nameLabel.setText(person.getUsername());
						
						Label accuracyLabel = (Label) dashboard.lookup("#userAccuracy");
						accuracyLabel.setText(String.valueOf(person.getAccuracy()));
						
						Label sinceLabel = (Label) dashboard.lookup("#userSince");
						sinceLabel.setText(person.getSince().toString());
						
						// enable logout
						Button logoutButton = (Button) dashboard.lookup("#logout");
						logoutButton.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent event) {
								logout();
							}
						});
						
						// enable app settings
						loadAppSettings(dashboard);
						
						// fill in training session history
						trainingSessionsList = (ListView<TrainingSession>) dashboard.lookup("#sessionHistory");
						loadTrainingSessions(ShowTrainingSessions.class, true);
						
						// fill in new training session form
						loadNewTrainingSessionForm(dashboard);
					} 
					catch (IOException e) {
						System.out.println("error showing dashboard: " + e.getMessage());
					}
				}
				else {
					// route to login
					loadPeopleCallback(ShowLogin.class, true);
				}
			}
			
			private void loadAppSettings(Node dashboard) {
				// save sounds
				ToggleButton saveSounds = (ToggleButton) dashboard.lookup("#saveSounds");
				
				// handle update
				saveSounds.selectedProperty().addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(ObservableValue<? extends Boolean> v, Boolean ov, Boolean nv) {
						properties.setProperty(PROP_SAVE_SOUNDS, nv.toString());
					}
				});
				
				// initial value from properties
				saveSounds.setSelected(Boolean.valueOf(properties.getProperty(PROP_SAVE_SOUNDS, "false")));
			}
			
			private void loadNewTrainingSessionForm(Node dashboard) {
				// symbol
				symbolDropdown = (SymbolComboBox) dashboard.lookup("#trainSymbol");
				symbolDropdown.setValue(properties.getProperty(PROP_TRAIN_SYMBOL, "AAPL"));
				
				// bar widths
				BarWidthComboBox barWidthDropdown = (BarWidthComboBox) dashboard.lookup("#trainBarWidth");
				barWidthDropdown.setValue(properties.getProperty(PROP_TRAIN_BAR_WIDTH, BarInterval.DY_1));
				
				// sample sizes, number of trade bars per sample
				// initial value from property and valid values handled in constructor.
				SampleSizeComboBox sampleSizeDropdown = (SampleSizeComboBox) dashboard.lookup("#trainSampleSize");
				
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
			
			public static class ShowTrainingSessions implements Runnable {
				private List<TrainingSession> sessions;
				
				public ShowTrainingSessions(List<TrainingSession> sessions) {
					this.sessions = sessions;
				}
				
				@Override
				public void run() {
					ObservableList<TrainingSession> observableSessions = FXCollections.observableList(sessions);
					trainingSessionsList.setItems(observableSessions);
					trainingSessionsList.setCellFactory(new Callback<ListView<TrainingSession>, ListCell<TrainingSession>>() {
						@Override
						public ListCell<TrainingSession> call(ListView<TrainingSession> param) {
							return new TrainingSession.TrainingSessionListCell();
						}
					});
				}
			}
		}
		
		/**
		 * Open a new training session in main window. Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 * @since August 2021
		 */
		public static class ShowTrainingSession implements Runnable {
			private static final int SAMPLE_GRAPH_WIDTH = 600;
			private static final int SAMPLE_GRAPH_HEIGHT = 300;
			
			private TrainingSession session;
			
			private CartesianGraph currentSampleGraph;
			private PointFilter dataFilter;
			
			public ShowTrainingSession(TrainingSession session) {
				this.session = session;
				this.currentSampleGraph = null;
			}
			
			@Override
			public void run() {
				// switch to training session scene
				Scene mainScene = mainWindow.getScene();
				
				Pane contentPane = (Pane) mainScene.lookup("#content");
				ObservableList<Node> content = contentPane.getChildren();
				content.clear();
				
				// update main menubar navigation
				dashboardMenuItem.setDisable(true);
				performanceViewMenuItem.setDisable(true);
				
				try {
					Node sessionRoot = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/TrainingSession.fxml"));
					content.add(sessionRoot);
					System.out.println("debug added training session root to content");
					
					// enable quit/abort
					Button abort = (Button) sessionRoot.lookup("#quit");
					abort.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							abortTrainingSession();
						}
					});
					System.out.println("debug enabled abort button");
					
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
					System.out.println("debug bound sample id");
					
					// load average score (should be 0.5) and score interval
					Label score = (Label) sessionRoot.lookup("#score");
					
					session.getScoreProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
							float scorePct = nv.floatValue() * 100;
							
							score.setText(String.valueOf(scorePct));
						}
					});
					System.out.println("debug loaded average score");
					
					// load score confidence interval
					Label scoreLow = (Label) sessionRoot.lookup("#scoreIntervalLow");
					Label scoreHigh = (Label) sessionRoot.lookup("#scoreIntervalHigh");
					
					session.getScoreDeviationProperty().addListener(new ChangeListener<Number>() {
						@Override
						public void changed(ObservableValue<? extends Number> v, Number ov, Number nv) {
							float scorePct = session.getScoreProperty().floatValue() * 100;
							float intervalRadius = (float) session.getScoreIntervalRadius() * 100;
							
							if (intervalRadius == 0) {
								scoreLow.setText("0");
								scoreHigh.setText("100");
							}
							else {
								float low = scorePct - intervalRadius;
								if (low < 0) {
									low = 0;
								}
								float high = scorePct + intervalRadius;
								if (high > 100) {
									high = 100;
								}
								
								scoreLow.setText(String.valueOf(low));
								scoreHigh.setText(String.valueOf(high));
							}
						}
					});
					
					// load config params
					loadConfig(sessionRoot);
					
					// load training controls
					loadControls(sessionRoot);
					
					// prepare first sample
					((Button) sessionRoot.lookup("#nextSample")).fire();
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
						System.out.println(session.getSample());
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
				
				// sample graph container
				BorderPane graphContainer = (BorderPane) sessionRoot.lookup("#sampleGraph");
				graphContainer.setPadding(Insets.EMPTY);
				
				// next sample (enabled on color guess)
				Button nextSample = (Button) sessionRoot.lookup("#nextSample");
				nextSample.setDisable(false);
				
				nextSample.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						// next sample
						MarketSample sample = session.nextSample(dbManager, marketSynth);
						if (sample != null) {
							// reset last score
							((Label) sessionRoot.lookup("#scoreLast")).setText("0.0");
							
							// disable next button
							nextSample.setDisable(true);
							
							// enable color guess submit
							((Button) sessionRoot.lookup("#colorGuessSubmit")).setDisable(false);
							
							// reset color guess
							colorGuess.setValue(0.5);
							
							// reset true color
							((Slider) sessionRoot.lookup("#colorTrue")).setValue(0.5);
							
							((Region) sessionRoot.lookup("#colorTrueTile")).setBackground(new Background(new BackgroundFill(
								javafx.scene.paint.Color.WHITE, 
								CornerRadii.EMPTY, 
								Insets.EMPTY
							)));
							
							// reload market data graph
							currentSampleGraph = loadSampleGraph(sample, SAMPLE_GRAPH_WIDTH, SAMPLE_GRAPH_HEIGHT);
							PannableCanvas canvas = currentSampleGraph.getCanvas();
							
							graphContainer.getChildren().clear();
							graphContainer.setCenter(canvas);
							graphContainer.setClip(new Rectangle(
									canvas.getPrefWidth(), 
									canvas.getPrefHeight()
							));
							
							// TODO fix CartesianGraph.layout
							// sampleGraph.layout();
							
							// save sample sound
							if (Boolean.valueOf(properties.getProperty(PROP_SAVE_SOUNDS, "false"))) {
								marketSynth.save(sample.getSound(), sample.idString());
							}
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
						
						// add future to sample graph
						dataFilter.setInput(new Point2D(dataFilter.getMaxX(), sample.getFuture().getClose()));
						try {
							Point2D futurePoint = dataFilter.call().get(0);
							
							currentSampleGraph.addPoint(
								futurePoint, 
								null, 
								CartesianPoint.RADIUS_DEFAULT, 
								CartesianPoint.BulletType.CIRCLE, 
								javafx.scene.paint.Color.BLUEVIOLET
							);
							
							currentSampleGraph.addLastEdge();
						}
						catch (Exception e) {
							System.out.println("ERROR failed to use data filter for future point: " + e.getMessage());
						}
						
						// enable next sample button
						nextSample.setDisable(false);
					}
				});
				
				System.out.println("DEBUG loaded session controls");
			}
			
			/**
			 * Load a managed visual graph for the given market data sample. Note that as of 2021-08-21 the
			 * {@code CartesianGraph} doesn't allow for flipping the y axis, so positive is down. Also, 
			 * {@code CartesianGraph.layout} isn't working right, so for now viewport adjustment is done by
			 * altering the points' coordinates.
			 * 
			 * @param sample Market data sample.
			 * @return The graph instance, whose canvas can be loaded into the gui.
			 */
			private CartesianGraph loadSampleGraph(MarketSample sample, int graphWidth, int graphHeight) {
				CartesianGraph graph = new CartesianGraph(CartesianGraph.PlotMode.CONNECTED_POINTS, graphWidth, graphHeight);
				graph.getUseViewportGestures().set(false);
				graph.getUseNodeGestures().set(false);
				
				List<Point2D> points = new LinkedList<>();
				int i=0;
				double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE, minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
				double x, y;
				Point2D p;
				for (TradeBar bar : sample.getBars()) {
					p = new Point2D(i++, bar.getClose());
					x = p.getX(); y = p.getY();
					
					if (x < minX) {
						minX = x;
					}
					if (x > maxX) {
						maxX = x;
					}
					if (y < minY) {
						minY = y;
					}
					if (y > maxY) {
						maxY = y;
					}
					
					points.add(p);
				}
				
				// reserve space for future point
				double postmaxX = maxX + (maxX-minX)/points.size();
				
				// define data filter
				dataFilter = new PointFilter(minX,postmaxX,minY,maxY,graphWidth,graphHeight) {
					@Override
					public List<Point2D> call() {
						List<Point2D> output = new ArrayList<>(this.input.size());
						
						for (Point2D point : input) {
							output.add(new Point2D(
								(point.getX()-this.minX)/(this.maxX-minX) * (this.graphWidth-50),
								// 1 - ... flips y axis
								(1 - (point.getY()-this.minY)/(this.maxY-this.minY)) * (this.graphHeight-50)
							));
						}
						
						return output;
					}
				};
				
				try {
					// filter data
					dataFilter.setInput(points);
					points = dataFilter.call();
					
					// add filtered data to graph
					graph.addDataset(
						points, 
						"sample " + session.getSampleIdProperty().get(), 
						5, CartesianPoint.BulletType.CIRCLE, javafx.scene.paint.Color.CRIMSON
					);
				} 
				catch (Exception e) {
					System.out.println("ERROR error using data filter: " + e.getMessage());
				}
				
				return graph;
			}
		}
		
		/**
		 * Open the api key input form in its own window. Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 * @since August 2021
		 */
		public static class ShowApiKeyForm implements Runnable {
			private static final String WINDOW_TITLE = "API Key Form";
			private static final int WINDOW_WIDTH = 500;
			private static final int WINDOW_HEIGHT = 240;
			
			private Stage apiKeyFormWindow;
			private String keyOld;
			
			public ShowApiKeyForm(String keyOld) {
				apiKeyFormWindow = new Stage();
				apiKeyFormWindow.setTitle(WINDOW_TITLE);
				apiKeyFormWindow.setWidth(WINDOW_WIDTH);
				apiKeyFormWindow.setHeight(WINDOW_HEIGHT);
				
				this.keyOld = keyOld;
			}
			
			@Override
			public void run() {
				try {
					Parent root = (Parent) FXMLLoader.load(MarketSense.class.getResource("resources/APIKeyForm.fxml"));
					Scene windowScene = new Scene(root);
					
					apiKeyFormWindow.setScene(windowScene);
					
					enableHyperlinks(root);
					
					// show old key
					((Text) root.lookup("#apiKeyOld")).setText(keyOld);
					
					// handle new key
					TextField keyField = (TextField) root.lookup("#apiKeyNew");
					keyField.setOnKeyReleased(new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							if (event.getCode().equals(KeyCode.ENTER)) {
								String keyNew = keyField.getText();
								
								if (keyNew.length() != 0) {
									// set api key of twelvedata client
									tdclient.setKey(keyNew);
									System.out.println("INFO set api key to " + keyNew);
									
									// close window
									apiKeyFormWindow.close();
								}
								else {
									System.out.println("ERROR api key not given");
									keyField.setText("");
									keyField.setPromptText("blank or invalid key given");
								}
							}
						}
					});
					
					apiKeyFormWindow.show();
				}
				catch (IOException e) {
					System.out.println("ERROR showing api key form window: " + e.getMessage());
					e.printStackTrace();
				}
			}
			
			/**
			 * Enable hyperlinks in the given gui fragment, given that they store their urls in the 
			 * {@link Hyperlink#tooltipProperty() tooltip}.
			 * 
			 * @param root The gui fragment root node.
			 */
			private void enableHyperlinks(Node root) {
				Set<Node> hyperlinks = root.lookupAll("Hyperlink");
				System.out.println("DEBUG found " + hyperlinks.size() + " hyperlinks");
				
				for (Node hln : hyperlinks) {
					Hyperlink hl = (Hyperlink) hln;
					hl.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							// assumes href is in tooltip
							hostServices.showDocument(hl.getTooltip().getText());
						}
					});
				}
			}
		}
		
		/**
		 * Open performance view in main window. Should be run on javafx thread.
		 * 
		 * @author Owen Gallagher
		 * @since 2021-11-01
		 */
		public static class ShowPerformanceView implements Runnable {
			private static final int PERFORMANCE_GRAPH_WIDTH = 600;
			private static final int PERFORMANCE_GRAPH_HEIGHT = 150;
			
			/**
			 * Collection of performance rows (performance graph with controls) bound to a container widget in
			 * the performance view.
			 */
			private ObservableList<Node> performanceRows;
			
			@Override
			public void run() {
				Scene mainScene = mainWindow.getScene();
				
				Pane contentPane = (Pane) mainScene.lookup("#content");
				ObservableList<Node> content = contentPane.getChildren();
				content.clear();
				
				try {
					Node performanceView = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/PerformanceView.fxml"));
					content.add(performanceView);
					
					// update main menubar navigation
					dashboardMenuItem.setDisable(false);
					performanceViewMenuItem.setDisable(true);
					
					// call layout on parent scroll pane to enable content lookup
					// see https://stackoverflow.com/a/40563331/10200417
					ScrollPane performanceRowsScroll = (ScrollPane) performanceView.lookup("#performanceRowsScroll");
					performanceRowsScroll.applyCss();
					performanceRowsScroll.layout();
					
					Pane performanceRowsContainer = (Pane) performanceView.lookup("#performanceRows");
					performanceRows = performanceRowsContainer.getChildren();
					
					// enable add graph button
					Button addGraphButton = (Button) performanceView.lookup("#addGraphButton");
					addGraphButton.setOnAction((ActionEvent addGraphEvent) -> {
						try {
							// add performance row
							Node performanceRow = (Node) FXMLLoader.load(MarketSense.class.getResource("resources/PerformanceRow.fxml"));
							performanceRows.add(performanceRow);
							
							// bind end date text field to date picker
							MultiDatePicker datePicker = (MultiDatePicker) performanceRow.lookup(".dates-dropdown");
							TextField lastDateField = (TextField) performanceRow.lookup(".dates-last");
							datePicker.getLastValueProperty().addListener(new ChangeListener<LocalDate>() {
								@Override
								public void changed(
									ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
									lastDateField.setText(newValue.format(MultiDatePicker.DATE_FORMAT));
								}
							});
							
							// enable graph refresh button
							BorderPane graphContainer = (BorderPane) performanceRow.lookup(".graph-container");
							
							((Button) performanceRow.lookup(".refresh-button")).setOnAction((ActionEvent refreshEvent) -> {
								String symbol = ((SymbolComboBox) performanceRow.lookup(".symbol-dropdown")).getValue();
								String sampleCount = ((TextField) performanceRow.lookup(".sample-count-dropdown")).getText();
								
								// create new performance sample
								PerformanceSample sample = new PerformanceSample(
									// person
									((AccountComboBox) performanceRow.lookup(".account-dropdown")).getValue(),
									// security
									symbol != null && symbol.length() != 0 
										? Security.loadSecurity(symbol,dbManager,tdclient)
										: null,
									// date range
									datePicker.getFirstValueProperty().getValue(), 
									datePicker.getLastValueProperty().getValue(),
									// bar width
									((BarWidthComboBox) performanceRow.lookup(".bar-width-dropdown")).getValue(),
									// sample size
									((SampleSizeComboBox) performanceRow.lookup(".sample-size-dropdown")).getValue(),
									// sample count
									sampleCount != null && sampleCount.length() != 0 
										? Integer.parseInt(sampleCount) 
										: -1
								);
								
								// TODO move sample data fetch off gui thread
								// fetch sample points from database
								sample.prepare(dbManager);
								
								// replace graph with data from new sample
								CartesianGraph graph = loadPerformanceGraph(
									sample, 
									PERFORMANCE_GRAPH_WIDTH, 
									PERFORMANCE_GRAPH_HEIGHT,
									false
								);
								PannableCanvas canvas = graph.getCanvas();
								
								// place in graph container
								graphContainer.getChildren().clear();
								graphContainer.setCenter(canvas);
								graphContainer.setClip(new Rectangle(
										canvas.getPrefWidth(), 
										canvas.getPrefHeight()
								));
								
								// TODO fix CartesianGraph.layout
								// graph.layout();
							});
						}
						catch (IOException e) {
							System.out.println("error performance row load failed: " + e.getMessage());
							e.printStackTrace();
						}
					});
				} 
				catch (IOException e) {
					System.out.println("error performance view load failed: " + e.getMessage());
					e.printStackTrace();
				}
			}
			
			private CartesianGraph loadPerformanceGraph(PerformanceSample sample, int graphWidth, int graphHeight, boolean showIntervals) {
				CartesianGraph graph = new CartesianGraph(CartesianGraph.PlotMode.CONNECTED_POINTS, graphWidth, graphHeight);
				graph.getUseViewportGestures().set(false);
				graph.getUseNodeGestures().set(false);
				
				List<Point2D> points = new LinkedList<>();
				int i=0;
				double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE, minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
				double x, y;
				Point2D p;
				for (PerformancePoint point : sample.getPoints()) {
					p = new Point2D(i++, point.getScore());
					x = p.getX(); y = p.getY();
					
					if (x < minX) {
						minX = x;
					}
					if (x > maxX) {
						maxX = x;
					}
					if (y < minY) {
						minY = y;
					}
					if (y > maxY) {
						maxY = y;
					}
					
					points.add(p);
				}
				
				// define data filter
				PointFilter dataFilter = new PointFilter(minX,maxX,minY,maxY,graphWidth,graphHeight) {
					@Override
					public List<Point2D> call() {
						List<Point2D> output = new ArrayList<>(this.input.size());
						
						for (Point2D point : input) {
							output.add(new Point2D(
								(point.getX()-this.minX)/(this.maxX-minX) * (this.graphWidth-50),
								// 1 - ... flips y axis
								(1 - (point.getY()-this.minY)/(this.maxY-this.minY)) * (this.graphHeight-50)
							));
						}
						
						return output;
					}
				};
				
				try {
					// filter data
					dataFilter.setInput(points);
					points = dataFilter.call();
					
					// add filtered data to graph
					graph.addDataset(
						points, 
						null, 
						5, CartesianPoint.BulletType.CIRCLE, javafx.scene.paint.Color.CRIMSON
					);
				} 
				catch (Exception e) {
					System.out.println("ERROR error using data filter: " + e.getMessage());
				}
				
				return graph;
			}
		}
	}
	
	private static <T extends Runnable, U extends HasCallback<T>> void loadPeople(Class<T> OnLoad, U hasOnLoad, boolean guiThread) {
		System.out.println("loading people from local db");
		
		@SuppressWarnings("unchecked")
		List<Person> people = (List<Person>) dbManager
		.createQuery("select p from " + Person.DB_TABLE + " p")
		.getResultList();
		System.out.println("loaded " + people.size() + " people from db");
		
		try {
			Runnable runnable;
			if (OnLoad != null) {
				runnable = OnLoad.getDeclaredConstructor(List.class).newInstance(people);
			}
			else if (hasOnLoad != null) {
				runnable = hasOnLoad.getCallback(people);
			}
			else {
				throw new NoSuchMethodException("no callback was defined");
			}
			
			if (guiThread) {
				Platform.runLater(runnable);
			}
			else {
				new Thread(runnable).start();
			}
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.out.println("error loading people: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the entities from the person table to compile a list of known accounts.
	 * 
	 * @param <T> Callback type.
	 * 
	 * @param OnLoad The callback to which the list of people is passed.
	 * @param guiThread Whether to use the javafx thread for the callback.
	 */
	public static <T extends Runnable> void loadPeopleCallback(Class<T> OnLoad, boolean guiThread) {
		loadPeople(OnLoad, null, guiThread);
	}
	
	/**
	 * Loads the entities from the person table to compile a list of known accounts.
	 * 
	 * @param <T> Callback owner type.
	 * @param <U> Callback type.
	 * 
	 * @param hasOnLoad <code>HasCallback</code> instance.
	 * @param guiThread Whether to use the javafx thread for the callback.
	 */
	public static <T extends HasCallback<U>, U extends Runnable> void loadPeopleHasCallback(T hasOnLoad, boolean guiThread) {
		loadPeople(null, hasOnLoad, guiThread);
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
	
	public static <T extends Runnable> void loadTrainingSessions(Class<T> OnLoad, boolean guiThread) {
		System.out.println("loading people from local db");
		
		@SuppressWarnings("unchecked")
		List<TrainingSession> sessions = (List<TrainingSession>) dbManager
		.createQuery(
			"select t from " + TrainingSession.DB_TABLE + " t " + 
			"where t." + TrainingSession.DB_COL_ID + "." + TrainingSessionId.DB_COL_PERSON + "." + Person.DB_COL_USERNAME + " = :username"
		)
		.setParameter("username", person.getUsername())
		.getResultList();
		System.out.println("INFO loaded " + sessions.size() + " training sessions from db");
		
		try {
			Runnable runnable = OnLoad.getDeclaredConstructor(List.class).newInstance(sessions);
			
			if (guiThread) {
				Platform.runLater(runnable);
			}
			else {
				new Thread(runnable).start();
			}
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.out.println("error loading training sessions: " + e.getMessage());
		}
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
		loadPeopleCallback(MarketSenseGUI.ShowLogin.class, true);
	}
	
	/**
	 * Open a new training session using the parameters selected in the new training session form.
	 * 
	 * @param symbol
	 * @param barWidth
	 * @param sampleSize
	 * @param sampleCount
	 * @param maxLookbackMonths
	 * 
	 * @return
	 */
	public static boolean newTrainingSession(String symbol, String barWidth, int sampleSize, int sampleCount, int maxLookbackMonths) {
		Security security = Security.loadSecurity(symbol, dbManager, tdclient);
		
		// commit training session config to properties file
		properties.setProperty(PROP_TRAIN_SYMBOL, symbol);
		properties.setProperty(PROP_TRAIN_BAR_WIDTH, barWidth);
		properties.setProperty(PROP_TRAIN_SAMPLE_SIZE, Integer.toString(sampleSize));
		properties.setProperty(PROP_TRAIN_SAMPLE_COUNT, Integer.toString(sampleCount));
		properties.setProperty(PROP_TRAIN_LOOKBACK_MAX_MONTHS, Integer.toString(maxLookbackMonths));
		saveProperties();
		
		if (security != null) {
			TrainingSession session = new TrainingSession(
				person, TrainingSessionType.TBD, 
				security, barWidth, sampleSize, sampleCount, maxLookbackMonths
			);
			
			System.out.println("starting a new training session " + session);
			
			// prepare the database
			Failure failure = session.collectMarketUniverse(dbManager,tdclient);
			
			if (failure == null) {
				System.out.println("market data universe acquired for lookback of " + session.getMaxLookbackMonths() + " months");
				
				// show training session interface
				Platform.runLater(new MarketSenseGUI.ShowTrainingSession(session));
			}
			else if (failure.code == Failure.ErrorCode.API_KEY) {
				// show api key input form
				Platform.runLater(new MarketSenseGUI.ShowApiKeyForm(tdclient.getKey()));
			}
			else {
				System.out.println("ERROR failed to creake market data universe for training session: " + failure.message);
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
	 * @param datetimeUtils Datetime utility methods test.
	 */
	public static void runTests(boolean base, boolean database, boolean marketSynth, boolean datetimeUtils) {
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
		if (datetimeUtils) {
			new TestDatetimeUtils().evaluate(false);
		}
	}
	
	/**
	 * Read from the properties file {@link #PROPERTIES_FILE}.
	 * 
	 * @return Project properties.
	 * 
	 * @throws FileNotFoundException If the properties file was not found.
	 */
	private static Properties getProperties() throws FileNotFoundException {
		Properties properties = new Properties();
		FileInputStream istream = new FileInputStream(PROPERTIES_FILE);
		
		try {
			properties.load(istream);
			return properties;
		} 
		catch (IOException e) {
			throw new FileNotFoundException("failed to read properties file " + PROPERTIES_FILE);
		}
	}
	
	/**
	 * Save project properties to the properties file {@link #PROPERTIES_FILE}.
	 */
	private static void saveProperties() {
		try {
			PROPERTIES_FILE.createNewFile();
			
			String comments = 
				"last updated by marketsense " + LocalDateTime.now();
			
			properties.store(new FileOutputStream(PROPERTIES_FILE), comments);
		} 
		catch (IOException e) {
			System.out.println("ERROR unable to save to properties file");
		}
	}
	
	public static File getParentDir() {
		return PARENT_DIR;
	}
	
	/**
	 * @return {@link #SYMBOLS_NASDAQ_FILE}.
	 */
	public static File getSymbolsFileNasdaq() {
		return SYMBOLS_NASDAQ_FILE;
	}
	
	/**
	 * @return {@link #SYMBOLS_NYSE_FILE}.
	 */
	public static File getSymbolsFileNyse() {
		return SYMBOLS_NYSE_FILE;
	}
	
	/**
	 * @param key The property key.
	 * 
	 * @return Property from {@link #properties}.
	 */
	public static String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * 
	 * @param key
	 * @param defaultValue
	 * 
	 * @return Property from {@link #properties}.
	 */
	public static String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	/**
	 * @return Current active account, {@link #person}.
	 */
	public static Person getAccount() {
		return person;
	}
}
