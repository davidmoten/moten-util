package moten.david.util.expression;

import com.google.inject.Provider;

public class Bool implements BooleanExpression, Provided<Boolean> {

	private final Provider<Boolean> provider;

	public static final Bool TRUE = new Bool(true);

	public static final Bool FALSE = new Bool(false);

	public Bool(Provider<Boolean> provider) {
		this.provider = provider;
	}

	public Bool(final boolean value) {
		this(new ConstantProvider<Boolean>(value));
	}

	@Override
	public boolean evaluate() {
		return provider.get();
	}

	@Override
	public Provider<Boolean> getProvider() {
		return provider;
	}

}
