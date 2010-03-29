package moten.david.ete;

import java.util.Set;

public interface Entity {
	Set<Identifier> getIdentifiers();

	void addFix(Fix fix);

	boolean isPrimaryIdentifier(Identifier identifier);
}
