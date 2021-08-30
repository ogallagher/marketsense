package ogallagher.marketsense.persistent;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * See 
 * <href a="https://docs.jboss.org/hibernate/orm/5.0/mappingGuide/en-US/html_single/#d5e678}">AttributeConverter example</href>.
 * 
 * If {@code null} is encountered in a db column, <b>0</b> is used.
 * 
 * @author Owen Gallagher
 * @since 2021-08-13
 * 
 */
@Converter
public class DoublePropertyPersister implements AttributeConverter<DoubleProperty, Double> {
	@Override
	public Double convertToDatabaseColumn(DoubleProperty attribute) {
		return attribute.doubleValue();
	}

	@Override
	public DoubleProperty convertToEntityAttribute(Double dbData) {
		// handle nulls as zero
		if (dbData == null) {
			dbData = 0d;
		}
		
		return new SimpleDoubleProperty(dbData);
	}
}
