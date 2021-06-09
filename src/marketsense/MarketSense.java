package marketsense;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

import twelvedata_client_java.TwelvedataClient;

import temp_fx_logger.System;

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
		
		// launch gui; calls start
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		System.out.println("MarketSense.start start");
		
		mainWindow = primaryStage;
		mainWindow.setWidth(MAIN_WINDOW_WIDTH_INIT);
		mainWindow.setHeight(MAIN_WINDOW_HEIGHT_INIT);
		mainWindow.setTitle(NAME);
		mainWindow.centerOnScreen();
	}
}
