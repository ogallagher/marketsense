package ogallagher.marketsense.widgets;

import java.util.ArrayList;
import java.util.HashSet;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import ogallagher.twelvedata_client_java.TwelvedataInterface.BarInterval;

/**
 * 
 * @author Owen Gallagher
 * @since 2021-11-01
 *
 */
public class BarWidthComboBox extends ComboBox<String> {
	/**
	 * Allowed bar width options.
	 */
	private static HashSet<String> barWidths;
	
	static {
		// current allowed bar widths between 1 hour and 1 day
		barWidths = new HashSet<String>();
		for (String width : new String[] {
			BarInterval.HR_1,
			BarInterval.HR_2,
			BarInterval.HR_4,
			BarInterval.HR_8,
			BarInterval.DY_1
		}) {
			barWidths.add(width);
		}
	}
	
	/**
	 * Default constructor.
	 */
	public BarWidthComboBox() {
		super();
		
		setItems(FXCollections.observableList(new ArrayList<String>(barWidths)));
	}
}
