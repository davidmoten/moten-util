package moten.david.ete.memory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import moten.david.ete.Engine;
import moten.david.ete.Entity;
import moten.david.ete.Identifier;
import moten.david.ete.Util;
import moten.david.util.collections.CollectionsUtil;
import moten.david.util.controller.Controller;

import com.google.inject.Inject;

public class MyEngine implements Engine {

	// TODO do fast lookup of entities from identifiers
	private final Map<Identifier, Entity> identifiers = new ConcurrentHashMap<Identifier, Entity>();

	private final Set<Entity> entities = Collections
			.synchronizedSet(new HashSet<Entity>());

	private final MyEntityFactory entityFactory;

	private final Controller controller;

	@Inject
	public MyEngine(MyEntityFactory entityFactory, Controller controller) {
		this.entityFactory = entityFactory;
		this.controller = controller;
	}

	@Override
	public Entity createEntity(SortedSet<Identifier> ids) {
		synchronized (entities) {
			final MyEntity entity = entityFactory.create(ids);
			entities.add(entity);
			return entity;
		}
	}

	@Override
	public Entity findEntity(SortedSet<Identifier> identifiers) {
		synchronized (entities) {
			for (Entity entity : entities) {
				if (Util.haveCommonIdentifier(entity.getIdentifiers().set(),
						identifiers))
					return entity;
			}
			return null;
		}
	}

	@Override
	public void removeEntity(Entity entity) {
		synchronized (entities) {
			entities.remove(entity);
		}
	}

	@Override
	public Enumeration<Entity> getEntities() {
		synchronized (entities) {
			return CollectionsUtil.toEnumeration(entities.iterator());
		}
	}

	public Enumeration<MyFix> getLatestFixes() {
		return new Enumeration<MyFix>() {

			private Iterator<MyFix> iterator;

			{
				List<MyFix> list = new ArrayList<MyFix>();
				for (Entity e : CollectionsUtil.toList(getEntities())) {
					list.add((MyFix) e.getLatestFix());
				}
				iterator = list.iterator();
			}

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public MyFix nextElement() {
				return iterator.next();
			}

		};
	}

	/**
	 * Saves all fixes to an OutputStream
	 * 
	 * @param os
	 */
	public long saveFixes(OutputStream os) {
		try {
			long count = 0;
			ObjectOutputStream oos = new ObjectOutputStream(os);
			Enumeration<Entity> en = getEntities();
			while (en.hasMoreElements()) {
				MyEntity entity = (MyEntity) en.nextElement();
				Enumeration<MyFix> enFixes = entity.getFixes();
				while (enFixes.hasMoreElements()) {
					MyFix fix = enFixes.nextElement();
					oos.writeObject(fix);
					count++;
				}
			}
			oos.close();
			return count;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
