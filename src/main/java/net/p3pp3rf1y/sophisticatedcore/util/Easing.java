package net.p3pp3rf1y.sophisticatedcore.util;

import java.util.function.Function;

public class Easing {
	public static final Easing EASE_IN_OUT_CUBIC = new Easing(number -> (float) (number < 0.5 ? 4 * number * number * number : (1 - Math.pow(-2 * number + 2, 3) / 2)));

	private final Function<Float, Float> easingFunction;

	private Easing(Function<Float, Float> easingFunction) {
		this.easingFunction = easingFunction;
	}

	public float ease(float number) {
		return easingFunction.apply(number);
	}
}
