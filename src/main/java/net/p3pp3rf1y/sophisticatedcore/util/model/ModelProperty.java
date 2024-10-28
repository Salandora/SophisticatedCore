package net.p3pp3rf1y.sophisticatedcore.util.model;

import com.google.common.base.Predicates;

import java.util.function.Predicate;

public class ModelProperty<T> implements Predicate<T> {
	private final Predicate<T> predicate;

	public ModelProperty() { this(Predicates.alwaysTrue()); }

	public ModelProperty(Predicate<T> predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(T t) {
		return false;
	}
}
