package com.codiform.moo.translator;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import com.codiform.moo.configuration.Configuration;
import com.codiform.moo.property.CollectionProperty;
import com.codiform.moo.session.TranslationSession;
import com.codiform.moo.session.TranslationSource;

public class CollectionTranslatorTest {

	private Configuration configuration = new Configuration();

	private CollectionTranslator translator = new CollectionTranslator(
			configuration);

	private TranslationSource session = new TranslationSession(configuration);
	
	private CollectionProperty property = mock(CollectionProperty.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testCollectionTranslatorPerformsDefensiveCopyByDefault() {
		Set<String> rhyme = new HashSet<String>();
		rhyme.add("one");
		rhyme.add("two");
		rhyme.add("buckle my shoe");

		Set<String> translated = (Set<String>) translator.translate(rhyme,
				property, session);

		Assert.assertNotNull(translated);
		Assert.assertEquals(rhyme, translated);
		Assert.assertNotSame(rhyme, translated);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCollectionTranslatorRetainsSortWhenTranslating() {
		SortedSet<String> rhyme = new TreeSet<String>();
		rhyme.add("Zed");
		rhyme.add("Ay");
		rhyme.add("Pee");

		assertOrder(rhyme, "Ay", "Pee", "Zed");

		SortedSet<String> translated = (SortedSet<String>) translator
				.translate(rhyme, property, session);

		Assert.assertNotNull(translated);
		Assert.assertEquals(rhyme, translated);
		Assert.assertNotSame(rhyme, translated);

		assertOrder(translated, "Ay", "Pee", "Zed");

		translated.add("Cee");

		assertOrder(translated, "Ay", "Cee", "Pee", "Zed");
	}

	private void assertOrder(SortedSet<String> set, String... orderedStrings) {
		Iterator<String> iterator = set.iterator();
		for (String item : orderedStrings) {
			Assert.assertEquals(item, iterator.next());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCollectionTranslatorCanBeConfiguredToNotPerformDefensiveCopy() {
		List<String> rhyme = new ArrayList<String>();
		rhyme.add("one");
		rhyme.add("two");
		rhyme.add("buckle my shoe");

		configuration.setPerformingDefensiveCopies(false);

		List<String> translated = (List<String>) translator.translate(rhyme,
				property, session);

		Assert.assertNotNull(translated);
		Assert.assertEquals(rhyme, translated);
		Assert.assertSame(rhyme, translated);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCollectionTranslatorTranslatesContentsOfCollection() {
		Collection<Foo> rhyme = new ArrayList<Foo>();
		rhyme.add(new Foo("this"));
		rhyme.add(new Foo("that"));

		when( property.shouldItemsBeTranslated() ).thenReturn( true );
		// Mockito doesn't love Class<?> return types.
		doReturn( Bar.class ).when( property ).getItemClass();
		
		Collection<Bar> translated = (Collection<Bar>) translator.translate(
				rhyme, property, session);

		Assert.assertEquals(2, translated.size());
		Iterator<Bar> iterator = translated.iterator();
		Assert.assertEquals("this", iterator.next().getValue());
		Assert.assertEquals("that", iterator.next().getValue());
	}

	public static class Foo {
		private String value;

		public Foo(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public static class Bar {
		private String value;

		public String getValue() {
			return value;
		}
	}

}
