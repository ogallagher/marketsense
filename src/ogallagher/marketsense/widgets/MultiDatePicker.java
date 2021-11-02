package ogallagher.marketsense.widgets;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import java.time.temporal.ChronoUnit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

/**
 * 
 * Derived from 
 * <a href="https://stackoverflow.com/a/60641108/10200417">this stackoverflow answer</a>.
 * 
 * @author Owen Gallagher
 * @since 2021-011-01
 *
 */
public class MultiDatePicker extends DatePicker {
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static StringConverter<LocalDate> dateConverter;
	
	private MultiDateSelectionMode selectionMode;
	private MultiDateValueMode valueMode;
	
	/**
	 * Observable set of dates currently selected.
	 * 
	 * @see #valuesSrc
	 */
	private final ObservableSet<LocalDate> values;
	/**
	 * Underlying ordered set for {@link #values}.
	 */
	private TreeSet<LocalDate> valuesSrc;
	
	static {
		dateConverter = new StringConverter<LocalDate>() {
			@Override
			public String toString(LocalDate date) {
				return (date == null) 
					? "" 
					: DATE_FORMAT.format(date);
			}
	
			@Override
			public LocalDate fromString(String string) {
				return ((string == null) || string.isEmpty()) 
					? null 
					: LocalDate.parse(string, DATE_FORMAT);
			}
		};
	}
	
	public MultiDatePicker(MultiDateSelectionMode selectionMode, MultiDateValueMode valueMode) {
		super();
		
		// init values
		valuesSrc = new TreeSet<>();
		values = FXCollections.observableSet(valuesSrc);
		
		// text field editor appearance
		setEditable(false);
		
		// date-text converter
		setConverter(dateConverter);
		
		// handle interaction
		setSelectionMode(selectionMode);
	
		// handle singular value
		setValueMode(valueMode);
	}
	
	/**
	 * Default constructor, creating a picker with scatter selection, and using the most recently changed
	 * date for the singular value.
	 */
	public MultiDatePicker() {
		this(MultiDateSelectionMode.SCATTER, MultiDateValueMode.CHANGED);
	}
	
	/**
	 * Handle interaction according to the given selection mode.
	 * 
	 * @param selectionMode Selection mode.
	 */
	public void setSelectionMode(MultiDateSelectionMode selectionMode) {
		EventHandler<MouseEvent> onClick;
		Color outerColor;
		Color innerColor;
		
		switch (selectionMode) {
			case INTERVAL:
				// handle interval click per cell
				onClick = (MouseEvent clickEvent) -> {
					if (clickEvent.getButton() == MouseButton.PRIMARY) {
						LocalDate d = getValue();
						
						if (!values.contains(d)) {
							values.add(d);
							values.addAll(getDateRange(valuesSrc.first(),valuesSrc.last()));
						} 
						else {
							values.remove(d);
							values.removeAll(getSmallerTail(values, d));
						}
						
						setValue(getValueFromValues(d));
					}
					
					show();
					clickEvent.consume();
				};
				
				innerColor = new Color(0.01, 0.5, 0.95, 1);
				outerColor = new Color(0.01, 0.5, 0.01, 1);
				break;
				
			case SCATTER:
			default:
				onClick = (MouseEvent clickEvent) -> {
					if (clickEvent.getButton() == MouseButton.PRIMARY) {
						LocalDate d = getValue();
						
						if (!values.contains(d)) {
							values.add(d);
						}
						else {
							values.remove(getValue());
						}
						
						setValue(getValueFromValues(d));
					}
					
					show();
					clickEvent.consume();
				};
				
				innerColor = new Color(0.01, 0.5, 0.95, 1);
				outerColor = innerColor;
				break;
		}
		
		// set cell factory per date in calendar widget
		setDayCellFactory((DatePicker param) -> new DateCell() {
			@Override
			public void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				
				if (item != null && !empty) {
					addEventHandler(MouseEvent.MOUSE_CLICKED, onClick);
				} 
				else {
					removeEventHandler(MouseEvent.MOUSE_CLICKED, onClick);
				}
				
				// highlight
				if (!values.isEmpty() && values.contains(item)) {
					// highlight first and last
					if (item.equals(valuesSrc.first()) || item.equals(valuesSrc.last())) {
						setBackground(new Background(new BackgroundFill(outerColor, null, null)));
					}
					// highlight others
					else {
						setBackground(new Background(new BackgroundFill(innerColor, null, null)));
					}
				} 
				// no highlight
				else {
					setBackground(null);
				}
			}
		});
		
		this.selectionMode = selectionMode;
	}
	
	/**
	 * Handle selection of the singular value according to the given value mode.
	 * 
	 * @param valueMode Value mode.
	 */
	public void setValueMode(MultiDateValueMode valueMode) {
		this.valueMode = valueMode;
	}
	
	public MultiDateSelectionMode getSelectionMode() {
		return this.selectionMode;
	}
	
	public MultiDateValueMode getValueMode() {
		return this.valueMode;
	}
	
	/**
	 * @return A copy of {@link #valuesSrc}.
	 */
	public ArrayList<LocalDate> getValues() {
		return new ArrayList<LocalDate>(valuesSrc);
	}

	/**
	 * @param min Low end of the range.
	 * @param max High end of the range.
	 * 
	 * @return All dates between <code>min</code> and <code>max</code>, exclusive.
	 * 
	 * @apiNote If the params are backwards, this method will swap them accordingly.
	 */
	private static Set<LocalDate> getDateRange(LocalDate min, LocalDate max) {
		Set<LocalDate> rangeGaps = new LinkedHashSet<>();
		
		if (min == null || max == null) {
			return rangeGaps;
		}
		else if (min.isAfter(max)) {
			// swap dates if wrong order
			LocalDate temp = max;
			max = min;
			min = temp;
		}
		
		LocalDate lastDate = min.plusDays(1);
		while (lastDate.isBefore(max)) {
			rangeGaps.add(lastDate);
			lastDate = lastDate.plusDays(1);
		}
		return rangeGaps;
	}
	
	/**
	 * Uses <code>date</code> as a partition and returns the smaller part.
	 * 
	 * @param dates
	 * @param date
	 * 
	 * @return The smaller of the two parts.
	 */
	private static Set<LocalDate> getSmallerTail(Set<LocalDate> dates, LocalDate date) {
		TreeSet<LocalDate> tempTree = new TreeSet<>(dates);
		tempTree.add(date);
		
		int higher = tempTree.tailSet(date).size();
		int lower = tempTree.headSet(date).size();

		if (lower <= higher) {
			return tempTree.headSet(date);
		} 
		else if (lower > higher) {
			return tempTree.tailSet(date);
		} 
		else {
			return new TreeSet<>();
		}

	}
	
	private LocalDate getValueFromValues(LocalDate changed) {
		switch (valueMode) {
			case FIRST:
				return valuesSrc.isEmpty() ? null : valuesSrc.first();
				
			case LAST:
				return valuesSrc.isEmpty() ? null : valuesSrc.last();
				
			case CHANGED:
			default:
				return getClosestDateInTree(valuesSrc, changed);
		}
	}
	
	/**
	 * Used with value mode {@link MultiDateValueMode#CHANGED CHANGED}.
	 * 
	 * @param dates Ordered set of dates.
	 * @param date Date from which to search.
	 * 
	 * @return Chronologically closest date in the tree to <code>date</code>, inclusive.
	 */
	private static LocalDate getClosestDateInTree(TreeSet<LocalDate> dates, LocalDate date) {
		if (dates.isEmpty()) {
			return null;
		}
		
		if (dates.size() == 1) {
			return dates.first();
		}
		
		if (dates.contains(date)) {
			return date;
		}
		
		LocalDate lowerDate = dates.lower(date);
		LocalDate higherDate = dates.higher(date);
		Long lower = null;
		Long higher = null;
		
		if (lowerDate != null) {
			lower = Math.abs(ChronoUnit.DAYS.between(date, lowerDate));
		}
		if (higherDate != null) {
			higher = Math.abs(ChronoUnit.DAYS.between(date, dates.higher(date)));
		}
		
		if (lower == null) {
			return higherDate;
		} 
		else if (higher == null) {
			return lowerDate;
		} 
		else if (lower <= higher) {
			return lowerDate;
		} 
		else {
			return higherDate;
		}
	}
	
	
	/**
	 * The type of selection interaction for a {@link MultiDatePicker} instance.
	 * 
	 * @author Owen Gallagher
	 * @since 2021-11-01
	 */
	public static enum MultiDateSelectionMode {
		/**
		 * Select dates as a collection of days.
		 */
		SCATTER, 
		/**
		 * Select dates as a continuous interval/period between two ends.
		 */
		INTERVAL
	}
	
	/**
	 * How the individual value for {@link MultiDatePicker#getValue()} is chosen,
	 * based on {@link MultiDatePicker#getValues()}.
	 * 
	 * @author Owen Gallagher
	 * @since 2021-11-01
	 */
	public static enum MultiDateValueMode {
		CHANGED,
		FIRST,
		LAST
	}
}
