package ogallagher.marketsense;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

import ogallagher.twelvedata_client_java.TwelvedataClient;

import ogallagher.temp_fx_logger.System;

/**
 * @author Owen Gallagher <github.com/ogallagher>
 * @since 9 June 2021
 * @version 0.0.1
 */
public class MarketSense extends Application {
	/**
	 * Prevents "Java Toolkit Not Initialized Error".
	 * I don't really get it, but an extra line doesn't do much harm anyway.
	 */
	@SuppressWarnings("unused") 
	private static JFXPanel dummyPanel = new JFXPanel();
	
	private static final String NAME = "MarketSense";
	
	private static final URL PROPERTIES_FILE = MarketSense.class.getResource("resources/config.properties");
	private static Properties properties;
	
	private static final String PROP_TWELVEDATA_API_KEY = "twelvedata_api_key";
	
	private static Stage mainWindow = null;
	private static int MAIN_WINDOW_WIDTH_INIT = 600;
	private static int MAIN_WINDOW_HEIGHT_INIT = 500;
	
	private static TwelvedataClient tdclient = null;
	
	/**
	 * Program entrypoint.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("MarketSense.main start");
		
		try {
			properties = getProperties();
		}
		catch (FileNotFoundException e) {
			properties = new Properties();
			System.out.println(e.getMessage());
		}
		
		// launch gui; calls start
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		System.out.println("MarketSense.start start");
		
		// load twelvedata client
		tdclient = new TwelvedataClient(properties.getProperty(PROP_TWELVEDATA_API_KEY, null));
		
		mainWindow = primaryStage;
		mainWindow.setWidth(MAIN_WINDOW_WIDTH_INIT);
		mainWindow.setHeight(MAIN_WINDOW_HEIGHT_INIT);
		mainWindow.setTitle(NAME);
		mainWindow.centerOnScreen();
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
