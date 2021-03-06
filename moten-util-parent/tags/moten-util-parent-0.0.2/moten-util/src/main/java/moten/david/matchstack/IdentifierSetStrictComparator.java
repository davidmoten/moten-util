package moten.david.matchstack;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import moten.david.util.functional.Functional;

import com.google.inject.Inject;

public class IdentifierSetStrictComparator implements
		Comparator<Set<TimedIdentifier>> {

	private final IdentifierTypeStrictComparator strictComparator;

	@Inject
	public IdentifierSetStrictComparator(
			IdentifierTypeStrictComparator strictComparator) {
		this.strictComparator = strictComparator;
	}

	@Override
	public int compare(Set<TimedIdentifier> o1, Set<TimedIdentifier> o2) {
		if (o1.isEmpty() && o2.isEmpty())
			return 0;
		else if (o1.isEmpty())
			return -1;
		else if (o2.isEmpty())
			return 1;
		else {
			IdentifierType t1 = Collections.max(Functional.apply(o1,
					Constants.identifierTypeExtractor), strictComparator);
			IdentifierType t2 = Collections.max(Functional.apply(o2,
					Constants.identifierTypeExtractor), strictComparator);
			return strictComparator.compare(t1, t2);
		}
	}

}
