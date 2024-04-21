package net.salandora.sophisticatedcore.unittests;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class Assertions {
	public static <T> void assertEquals(T expected, T actual) {
		assertEquals(expected, actual, "assertEquals failed");
	}
	public static <T> void assertEquals(T expected, T actual, Object message) {
		if (!Objects.equals(expected, actual)) {
			throw new AssertionError(String.format("%s%nexpected: %s%n but was: %s", message, expected, actual));
		}
	}

	public static void assertStackEquals(ItemStack expected, ItemStack actual, Object message) {
		if (!ItemStack.matches(expected, actual)) {
			throw new AssertionError(String.format("%s%nexpected: %s%n but was: %s", message, expected, actual));
		}
	}

	public static void assertTrue(boolean value) {
		assertTrue(value, null);
	}

	public static void assertTrue(boolean value, Object message) {
		if (!value) {
			throw new AssertionError(String.format("%s%nexpected: true%n but was: false", message));
		}
	}
}
