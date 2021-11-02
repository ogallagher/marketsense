package ogallagher.marketsense.widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import ogallagher.marketsense.MarketSense;
import ogallagher.temp_fx_logger.System;

/**
 * Combo box with allowed market data sample sizes (number of bars per sample), along with the sample size
 * defined in the program properties on launch.
 * 
 * @author Owen Gallagher
 * @since 2021-11-01
 *
 */
public class SampleSizeComboBox extends ComboBox<Integer> {
	/**
	 * Default constructor.
	 */
	public SampleSizeComboBox() {
		super();
		
		// current supported sample sizes
		Integer sampleSize = Integer.valueOf(MarketSense.getProperty(MarketSense.PROP_TRAIN_SAMPLE_SIZE, "7"));
		
		HashSet<Integer> sampleSizes = new HashSet<Integer>();
		for (Integer size : new Integer[] {
			-1, sampleSize, 7, 10, 15, 20, 30
		}) {
			sampleSizes.add(size);
		}
		
		setItems(FXCollections.observableList(
			// convert to list
			new ArrayList<Integer>(
				// sort with TreeSet constructor
				new TreeSet<Integer>(sampleSizes)
			)));
		
		setValue(-1);
	}
}
