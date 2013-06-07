package com.codiform.moo.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codiform.moo.NoDestinationException;
import com.codiform.moo.configuration.Configuration;
import com.codiform.moo.property.source.SourceProperty;
import com.codiform.moo.translator.Translator;
import com.codiform.moo.translator.ValueTypeTranslator;

/**
 * A translation session contains information relevant to a particular translation pass. This
 * typically corresponds to a single invocation of the translator, although you could retain a
 * translation session if you wished to re-use it.
 * 
 * <p>
 * The TranslationSession is not intended to be thread-safe; it is a stateful construct, and several
 * threads running on it at once would have an undefined effect.
 * </p>
 */
public class TranslationSession implements TranslationSource {

	private TranslationCache translationCache;
	private Configuration configuration;
	private Map<String, Object> variables;

	/**
	 * Creates a translation session with a known configuration.
	 * 
	 * @param configuration
	 *            the {@link Configuration} of the translation session
	 */
	public TranslationSession( Configuration configuration ) {
		translationCache = new TranslationCache();
		this.configuration = configuration;
	}

	/**
	 * Creates a translation session with a known configuration and with context variables.
	 * 
	 * @param configuration
	 *            the {@link Configuration} of the translation session
	 * @param variables
	 *            variables which can be used by translations to get external data or perform
	 *            external logic
	 */
	public TranslationSession( Configuration configuration, Map<String, Object> variables ) {
		this( configuration );
		this.variables = variables;
	}

	public <T> T getTranslation( Object source, Class<T> destinationClass ) {
		T translated = translationCache.getTranslation( source, destinationClass );
		if ( translated == null )
			translated = translate( source, destinationClass );
		return translated;
	}

	/**
	 * Updates an object from a source object, performing any necessary translations along the way.
	 * 
	 * @param source
	 *            the object from which the values should be retrieved (and, if necessary,
	 *            translated)
	 * @param destination
	 *            the object to which the values should be applied
	 */
	@Override
	public void update( Object source, Object destination ) {
		assureDestination( destination );
		configuration.getTranslator( destination.getClass() ).castAndUpdate( source, destination, this, variables );
	}

	private void assureDestination( Object destination ) {
		if ( destination == null ) {
			throw new NoDestinationException();
		}
	}

	private <T> T translate( Object source, Class<T> destinationClass ) {
		if ( source == null ) {
			return null;
		} else {
			// Is it a value type or an object?
			ValueTypeTranslator<T> vtt = configuration.getValueTypeTranslator( destinationClass );
			if ( vtt != null ) {
				return vtt.getTranslation( source, destinationClass );
			} else {
				return getObjectTranslation( source, destinationClass );
			}
		}
	}

	private <T> T getObjectTranslation( Object source, Class<T> destinationClass ) {
		Translator<T> translator = getTranslator( destinationClass );
		T translated = translator.create();
		translationCache.putTranslation( source, translated );
		translator.update( source, translated, this, variables );
		return translated;
	}

	private <T> Translator<T> getTranslator( Class<T> destination ) {
		return configuration.getTranslator( destination );
	}

	/* package */void setTranslationCache( TranslationCache cache ) {
		this.translationCache = cache;
	}

	@Override
	public <T> List<T> getEachTranslation( List<?> sources, Class<T> destinationClass, String itemExpression ) {
		List<T> results = new ArrayList<T>();
		for ( Object item : sources ) {
			Object source = applyExpression( item, itemExpression );
			results.add( getTranslation( source, destinationClass ) );
		}
		return results;
	}

	private Object applyExpression( Object item, String itemExpression ) {
		if( itemExpression == null )
			return item;
		
		SourceProperty property = configuration.getSourceProperty( itemExpression );
		return property.getValue( item );
	}

	@Override
	public <T> Set<T> getEachTranslation( Set<?> sources, Class<T> destinationClass, String itemExpression ) {
		Set<T> results = new HashSet<T>();
		for ( Object item : sources ) {
			Object source = applyExpression( item, itemExpression );
			results.add( getTranslation( source, destinationClass ) );
		}
		return results;
	}

	@Override
	public <T> Collection<T> getEachTranslation( Collection<?> sources, Class<T> destinationClass, String itemExpression ) {
		List<T> results = new ArrayList<T>();
		for ( Object item : sources ) {
			Object source = applyExpression( item, itemExpression );
			results.add( getTranslation( source, destinationClass ) );
		}
		return results;
	}

	public <T> List<T> getEachTranslation( List<?> sources, Class<T> destinationClass ) {
		return getEachTranslation( sources, destinationClass, null );
	}

	public <T> Collection<T> getEachTranslation( Collection<?> sources, Class<T> destinationClass ) {
		return getEachTranslation( sources, destinationClass, null );
	}

	public <T> Set<T> getEachTranslation( Set<?> sources, Class<T> destinationClass ) {
		return getEachTranslation( sources, destinationClass, null );
	}

}
