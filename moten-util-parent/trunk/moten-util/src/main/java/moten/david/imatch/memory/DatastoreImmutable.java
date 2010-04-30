package moten.david.imatch.memory;

import static moten.david.imatch.memory.Util.ids;
import static moten.david.imatch.memory.Util.types;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import moten.david.imatch.Identifier;
import moten.david.imatch.IdentifierSetStrictComparator;
import moten.david.imatch.IdentifierType;
import moten.david.imatch.IdentifierTypeStrictComparator;
import moten.david.imatch.TimedIdentifier;
import moten.david.util.functional.Fold;
import moten.david.util.functional.Function;
import moten.david.util.functional.Functional;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Immutable Collection of timed identifier sets in combination with methods for
 * adding more identifier sets.
 * 
 * @author dave
 * 
 */
public class DatastoreImmutable {

	/**
	 * Logger.
	 */
	private static Logger log = Logger.getLogger(DatastoreImmutable.class
			.getName());

	/**
	 * z contains the current set of timed identifier sets.
	 */
	private final ImmutableSet<Set<TimedIdentifier>> z;
	/**
	 * Strictly compares identifier types.
	 */
	private final IdentifierTypeStrictComparator strictTypeComparator;
	/**
	 * Strictly compares sets of identifiers.
	 */
	private final IdentifierSetStrictComparator strictSetComparator;

	/**
	 * Constructor.
	 * 
	 * @param strictTypeComparator
	 * @param strictSetComparator
	 * @param sets
	 */
	@Inject
	public DatastoreImmutable(
			IdentifierTypeStrictComparator strictTypeComparator,
			IdentifierSetStrictComparator strictSetComparator,
			@Assisted Set<Set<TimedIdentifier>> sets) {
		this.strictTypeComparator = strictTypeComparator;
		this.strictSetComparator = strictSetComparator;
		Preconditions.checkNotNull(sets);
		this.z = ImmutableSet.copyOf(sets);
	}

	/**
	 * Returns z.
	 * 
	 * @return
	 */
	public ImmutableSet<Set<TimedIdentifier>> sets() {
		return z;
	}

	/**
	 * The empty set of TimedIdentifier.
	 */
	private static ImmutableSet<TimedIdentifier> empty = ImmutableSet.of();

	/**
	 * Accessor method for the empty set.
	 * 
	 * @return
	 */
	private static ImmutableSet<TimedIdentifier> empty() {

		return empty;
	}

	/**
	 * Returns the primary match for x.
	 * 
	 * @param x
	 * @return
	 */
	private Set<TimedIdentifier> pm(final Set<TimedIdentifier> x) {
		if (z.size() == 0)
			return empty();
		else {
			Boolean noIntersectInZ = Functional.fold(z,
					new Fold<Set<TimedIdentifier>, Boolean>() {
						@Override
						public Boolean fold(Boolean lastValue,
								Set<TimedIdentifier> t) {
							return lastValue
									&& Sets.intersection(ids(x), ids(t)).size() == 0;
						}

					}, true);
			if (noIntersectInZ)
				return Collections.EMPTY_SET;
			else {
				Set<TimedIdentifier> y = Collections.max(Sets.filter(z,
						new Predicate<Set<TimedIdentifier>>() {
							@Override
							public boolean apply(Set<TimedIdentifier> i) {
								return Sets.intersection(ids(x), ids(i)).size() > 0;
							}
						}), strictSetComparator);
				return y;
			}
		}
	}

	/**
	 * Returns the result of the function g on the parameters.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private Set<TimedIdentifier> g(final Set<TimedIdentifier> x,
			final Set<TimedIdentifier> y) {
		return Sets.filter(y, new Predicate<TimedIdentifier>() {
			@Override
			public boolean apply(TimedIdentifier i) {
				TimedIdentifier j = getIdentifierOfType(x, i.getIdentifier()
						.getIdentifierType());
				return j == null || (i.getTime() > j.getTime());
			}
		});
	}

	/**
	 * Returns the identifier of given IdentifierType if one exists else returns
	 * null.
	 * 
	 * @param x
	 * @param identifierType
	 * @return
	 */
	private static TimedIdentifier getIdentifierOfType(Set<TimedIdentifier> x,
			IdentifierType identifierType) {
		for (TimedIdentifier i : x)
			if (i.getIdentifier().getIdentifierType().equals(identifierType))
				return i;
		// not found
		return null;
	}

	/**
	 * Returns the product of x and y given the new set r.
	 * 
	 * @param x
	 * @param y
	 * @param r
	 * @return
	 */
	public Set<TimedIdentifier> product(final Set<TimedIdentifier> x,
			final Set<TimedIdentifier> y, final Set<TimedIdentifier> r) {
		if (y.size() == 0 || r.size() == 0)
			return x;
		else if (strictSetComparator.compare(r, y) < 0) {
			final Set<Identifier> yIds = ids(y);
			return Sets.filter(x, new Predicate<TimedIdentifier>() {
				@Override
				public boolean apply(TimedIdentifier i) {
					return !yIds.contains(i);
				}
			});
		} else {
			final Set<TimedIdentifier> g = g(x, y);
			final Set<IdentifierType> gTypes = types(g);
			Set<TimedIdentifier> a = Sets.filter(x,
					new Predicate<TimedIdentifier>() {
						@Override
						public boolean apply(final TimedIdentifier i) {
							return !gTypes.contains(i.getIdentifier()
									.getIdentifierType());
						}
					});
			return Sets.union(a, g);
		}
	}

	/**
	 * Returns the result of merging a new set of timed identifiers with the
	 * current z.
	 * 
	 * @param a
	 * @return
	 */
	public DatastoreImmutable add(final Set<TimedIdentifier> a) {
		final Set<TimedIdentifier> pmza = pm(a);
		if (pmza.isEmpty())
			return new DatastoreImmutable(strictTypeComparator,
					strictSetComparator, Sets.union(z, ImmutableSet.of(a)));
		else {
			final Set<Set<TimedIdentifier>> intersecting = Sets.filter(z,
					new Predicate<Set<TimedIdentifier>>() {
						@Override
						public boolean apply(Set<TimedIdentifier> y) {
							return Sets.intersection(ids(y), ids(a)).size() > 0;
						}
					});
			final Set<TimedIdentifier> fold = Functional.fold(intersecting,
					new Fold<Set<TimedIdentifier>, Set<TimedIdentifier>>() {
						@Override
						public Set<TimedIdentifier> fold(
								Set<TimedIdentifier> previous,
								Set<TimedIdentifier> current) {
							Set<TimedIdentifier> result = product(previous,
									current, a);
							return result;
						}
					}, product(pmza, a, a));
			final Set<Identifier> foldIds = ids(fold);

			Set<Set<TimedIdentifier>> foldComplement = Functional.apply(z,
					new Function<Set<TimedIdentifier>, Set<TimedIdentifier>>() {
						@Override
						public Set<TimedIdentifier> apply(Set<TimedIdentifier> s) {
							return Sets.filter(s,
									new Predicate<TimedIdentifier>() {
										@Override
										public boolean apply(TimedIdentifier i) {
											return !foldIds.contains(i
													.getIdentifier());
										}
									});
						}
					});
			SetView<Set<TimedIdentifier>> newZ = Sets.union(foldComplement,
					ImmutableSet.of(fold));
			// remove empty sets
			newZ = Sets.difference(newZ, ImmutableSet.of(ImmutableSet.of()));
			return new DatastoreImmutable(strictTypeComparator,
					strictSetComparator, newZ);
		}
	}

	@Override
	public String toString() {
		if (z.size() == 0)
			return "empty";
		StringBuffer s = new StringBuffer();
		for (Set<TimedIdentifier> set : z) {
			if (s.length() > 0)
				s.append("\n");
			s.append(set.toString());
		}
		return s.toString();
	}
}
