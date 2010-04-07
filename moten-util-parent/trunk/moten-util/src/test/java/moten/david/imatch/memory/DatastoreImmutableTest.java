package moten.david.imatch.memory;

import java.util.HashMap;
import java.util.logging.Logger;

import junit.framework.Assert;
import moten.david.imatch.Datastore;
import moten.david.imatch.Identifier;
import moten.david.imatch.IdentifierSet;
import moten.david.imatch.IdentifierSetFactory;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class DatastoreImmutableTest {

	private long millis = System.currentTimeMillis();

	private static Logger log = Logger.getLogger(DatastoreImmutableTest.class
			.getName());

	private final Injector injector = Guice
			.createInjector(new InjectorModule());

	@Test
	public void test() {

		DatastoreImmutableFactory factory = injector
				.getInstance(DatastoreImmutableFactory.class);

		Datastore ds = factory.create(ImmutableMap
				.copyOf(new HashMap<Identifier, IdentifierSet>()), ImmutableMap
				.copyOf(new HashMap<IdentifierSet, Double>()));

		IdentifierSet ids = createSet();
		{
			ids = ids.add(createIdentifier("name1", "fred", 10, 10));
			ds = ds.add(ids, millis++);
			Assert.assertEquals(1, ds.identifiers().size());

			// no change on readding it
			ds = ds.add(ids, millis++);
			Assert.assertEquals(1, ds.identifiers().size());

			// ids = ids.add(createIdentifier("name2", "joe", 10, 11));
			// ds = ds.add(ids, millis++);
			// Assert.assertEquals(2, ds.identifiers().size());
			//
			// ids = ids.add(createIdentifier("name2", "keith", 10, 11));
			// ds = ds.add(ids, millis++);
			// Assert.assertEquals(3, ds.identifiers().size());

		}

	}

	private Identifier createIdentifier(String name, String value,
			int strength, int order) {
		MyIdentifierType type = new MyIdentifierType(name, strength, order);
		return new MyIdentifier(type, value);
	}

	private IdentifierSet createSet() {
		IdentifierSetFactory idSetFactory = injector
				.getInstance(IdentifierSetFactory.class);
		IdentifierSet ids = idSetFactory.create();
		return ids;
	}
}