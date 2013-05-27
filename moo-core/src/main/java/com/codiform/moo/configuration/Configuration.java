package com.codiform.moo.configuration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codiform.moo.MissingSourcePropertyException;
import com.codiform.moo.annotation.AccessMode;
import com.codiform.moo.property.Property;
import com.codiform.moo.property.source.SourceProperty;
import com.codiform.moo.property.source.SourcePropertyFactory;
import com.codiform.moo.property.source.ReflectionSourcePropertyFactory;
import com.codiform.moo.translator.ArrayTranslator;
import com.codiform.moo.translator.CollectionTranslator;
import com.codiform.moo.translator.Translator;
import com.codiform.moo.translator.TranslatorFactory;

/**
 * Represents a configuration of Moo; this can contain no information, at which point Moo work from
 * convention and use reflection, or it can use outside configuration, which is of particular merit
 * when you'd like to do complex object mapping without marking up the objects you intend to map.
 */
public class Configuration implements TranslatorFactory {

	private CollectionTranslator collectionTranslator;
	private ArrayTranslator arrayTranslator;
	private boolean performingDefensiveCopies = true;
	private boolean sourcePropertyRequired = true;
	private AccessMode defaultAccessMode = AccessMode.FIELD;
	private List<SourcePropertyFactory> originSources = new ArrayList<SourcePropertyFactory>();
	private Logger log = LoggerFactory.getLogger( getClass() );

	/**
	 * Creates a default configuration.
	 */
	public Configuration() {
		collectionTranslator = new CollectionTranslator( this );
		arrayTranslator = new ArrayTranslator( this );
		originSources = new ArrayList<SourcePropertyFactory>();
		originSources.add( new ReflectionSourcePropertyFactory() );
		configureExtensions();
	}

	/**
	 * Adding optional extensions to the core configuration.
	 */
	private void configureExtensions() {
		try {
			Class<?> originClass = Class.forName( "com.codiform.moo.property.source.MvelSourcePropertyFactory" );
			originSources.add( (SourcePropertyFactory)originClass.newInstance() );
		} catch ( ClassNotFoundException e ) {
			// No MVEL Extension. That's ok. In fact, to be expected.
		} catch ( InstantiationException exception ) {
			log.warn( "Instantiation exception while configuring extensions.", exception );
		} catch ( IllegalAccessException exception ) {
			log.warn( "Instantiation exception while configuring extensions.", exception );
		}
	}

	/**
	 * Used to control whether or not arrays and collections should be copied as a defensive
	 * measure.
	 * 
	 * <p>
	 * This defaults to true. If you know that you will not modify the array or collection (e.g. if
	 * you're going to serialize it right away) this might introduce a small amount of additional
	 * performance overhead which you can remove by disabling.
	 * </p>
	 * 
	 * @param performingDefensiveCopies
	 * @see #isPerformingDefensiveCopies()
	 */
	public void setPerformingDefensiveCopies( boolean performingDefensiveCopies ) {
		this.performingDefensiveCopies = performingDefensiveCopies;
	}

	/**
	 * Indicates if arrays and collections should be copied as a defensive measure to ensure that
	 * the source arrays and collections aren't accidentally modified by modifying the arrays and
	 * collections in the translation.
	 * 
	 * @see #setPerformingDefensiveCopies(boolean)
	 */
	public boolean isPerformingDefensiveCopies() {
		return this.performingDefensiveCopies;
	}

	public <T> Translator<T> getTranslator( Class<T> destinationClass ) {
		return new Translator<T>( destinationClass, this );
	}

	public CollectionTranslator getCollectionTranslator() {
		return collectionTranslator;
	}

	public ArrayTranslator getArrayTranslator() {
		return arrayTranslator;
	}

	/**
	 * Controls whether a source property is required for a translation to succeed. If you wish
	 * translation to fail with a {@link com.codiform.moo.MissingSourcePropertyException} when a property in
	 * the source object cannot be found to correspond to a destination property, set this value to
	 * true.
	 * 
	 * @param sourcePropertyRequired
	 *            true if a source property is required for each destination property
	 */
	public void setSourcePropertiesRequired( boolean sourcePropertyRequired ) {
		this.sourcePropertyRequired = sourcePropertyRequired;
	}

	/**
	 * Indicates if source properties are required for translation to succeed. The default value,
	 * false, allows the translation to continue even if source properties can't be found to match
	 * all of the destination fields.
	 * 
	 * @return true if source properties are required, false otherwise
	 */
	public boolean isSourcePropertyRequired() {
		return sourcePropertyRequired;
	}

	public AccessMode getDefaultAccessMode() {
		return defaultAccessMode;
	}

	public void setDefaultAccessMode( AccessMode accessMode ) {
		if ( accessMode == null ) {
			throw new NullPointerException( "Default access mode cannot be null." );
		} else {
			this.defaultAccessMode = accessMode;
		}
	}

	public SourceProperty getSourceProperty( Property property ) {
		String expression = property.getOriginExpression().trim();
		String prefix = getPrefix( expression );
		if ( prefix == null ) {
			return getSourceProperty( expression );
		} else {
			return getSourceProperty( prefix, expression );
		}
	}

	private SourceProperty getSourceProperty( String prefix, String expression ) {
		String unprefixed = expression.substring( prefix.length() + 1 );
		for ( SourcePropertyFactory item : originSources ) {
			if ( item.supportsPrefix( prefix ) ) {
				SourceProperty property = item.getSourceProperty( prefix, unprefixed );
				if ( property != null ) {
					return property;
				}
			}
		}
		throw new MissingSourcePropertyException( expression );
	}

	private SourceProperty getSourceProperty( String expression ) {
		for ( SourcePropertyFactory item : originSources ) {
			SourceProperty property = item.getSourceProperty( expression );
			if ( property != null ) {
				return property;
			}
		}
		throw new MissingSourcePropertyException( expression );
	}

	private String getPrefix( String expression ) {
		int colonIndex = expression.indexOf( ':' );
		if ( colonIndex > 0 && colonIndex < ( expression.length() - 1 ) ) {
			return expression.substring( 0, colonIndex );
		} else {
			return null;
		}
	}

}