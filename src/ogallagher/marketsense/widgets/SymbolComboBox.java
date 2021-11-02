package ogallagher.marketsense.widgets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import ogallagher.marketsense.MarketSense;
import ogallagher.temp_fx_logger.System;

/**
 * Security symbol chooser, which automatically populates valid choices on load. 
 * 
 * @author Owen Gallagher
 * @since 2021-11-01
 */
public class SymbolComboBox extends ComboBox<String> {
	private static HashSet<String> allSymbols;
	
	private SymbolComboBox self; 
	
	/**
	 * Default constructor.
	 */
	public SymbolComboBox() {
		super();
		
		if (allSymbols == null) {
			// load symbol options
			loadSymbols(true);
		}
		else {
			Platform.runLater(new ShowSymbols());
		}
		
		self = this;
	}
	
	/**
	 * Constructor with defined initial value.
	 * 
	 * @param value Initial selected symbol.
	 */
	public SymbolComboBox(String value) {
		this();
		
		setValue(value);
	}
	
	/**
	 * Symbols load from files is done on separate thread, and load into dropdown options
	 * is done in this callback, on the gui thread.
	 * 
	 * @author Owen Gallagher
	 *
	 */
	private class ShowSymbols implements Runnable {
		public ShowSymbols() {
			super();
		}
		
		@Override
		public void run() {
			self.setItems(FXCollections.observableList(
				// convert to list
				new ArrayList<String>(
					// sort with TreeSet constructor
					new TreeSet<String>(allSymbols)
				)
			));
		}
	}
	
	/**
	 * Loads all supported asset symbols from resource files: {@link SYMBOLS_FILE_NYSE}, {@link SYMBOLS_FILE_NASDAQ},
	 * and uses them to populate this combo box.
	 * 
	 * @param guiThread Whether the callback needs to be executed on the gui thread.
	 */
	private void loadSymbols(boolean guiThread) {
		System.out.println("loading symbols from resource files");
		
		// init empty static allSymbols
		allSymbols = new HashSet<>();
		
		// read symbols from resources
		for (File file : new File[] {
			MarketSense.getSymbolsFileNasdaq(),
			MarketSense.getSymbolsFileNyse()
		}) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
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
				System.out.println("failed to read symbols list from " + file.getPath());
			}
		}
		
		try {
			Runnable showSymbols = new ShowSymbols();
			
			if (guiThread) {
				Platform.runLater(showSymbols);
			}
			else {
				new Thread(showSymbols).start();
			}
		} 
		catch (IllegalArgumentException | SecurityException e) {
			System.out.println("error loading symbols: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
