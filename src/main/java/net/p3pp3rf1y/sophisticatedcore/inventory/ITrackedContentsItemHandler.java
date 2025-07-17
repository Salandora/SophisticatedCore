package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;

import java.util.Set;
import java.util.function.Consumer;

public interface ITrackedContentsItemHandler extends SlottedStackStorage {

	Set<ItemStackKey> getTrackedStacks();

	void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();

	int getInternalSlotLimit(int slot);
}
