package net.salandora.sophisticatedcore.unittests;

import org.mockito.Mockito;

import net.minecraft.nbt.CompoundTag;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartitioner;

import java.util.Optional;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.when;

class InventoryPartitionerTest {
	private static InventoryHandler getInventoryHandler(int slots) {
		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getSlotCount()).thenReturn(slots);
		return inventoryHandler;
	}

	public static void runTests() {
		addInventoryPartAtTheBeginningProperlyUpdatesParts();
		addTwoAndRemoveInventoryPartProperlyUpdatesParts();
		AddTwoAndRemoveOneProperlyShowsSlotAfterFirstAsReplaceable();
		addTwoAndRemoveFirstProperlyUpdatesFirstsSlots();
		addTwoThanRemoveFirstAndThenSecondShowsAllSlotsAsReplaceable();
		addPartReplacingAllSlotsAndRemovingThatProperlyUpdatesToDefault();
		addPartReplacingAllSlotsReturnsEmptyPartFromMaxSlotPlusOne();

		for (int slot = 1; slot <= 4; slot++) {
			getFirstSpaceReturnsCorrectRangeForSmallInventories(slot);
		}

		LoggerFactory.getLogger("sophisticatedcore testmod").info("InventoryPartitionerTests successful.");
	}

	private static void addInventoryPartAtTheBeginningProperlyUpdatesParts() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);

		Assertions.assertEquals(partitioner.getPartBySlot(0), dummyPartHandler);
		Assertions.assertTrue(partitioner.getPartBySlot(9) instanceof IInventoryPartHandler.Default);
	}

	private static void addTwoAndRemoveInventoryPartProperlyUpdatesParts() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(9);

		Assertions.assertEquals(dummyPartHandler, partitioner.getPartBySlot(0));
		Assertions.assertTrue(partitioner.getPartBySlot(9) instanceof IInventoryPartHandler.Default);
		Assertions.assertEquals(partitioner.getPartBySlot(9), partitioner.getPartBySlot(80));
		Assertions.assertEquals(72, partitioner.getPartBySlot(9).getSlots());
	}

	private static void AddTwoAndRemoveOneProperlyShowsSlotAfterFirstAsReplaceable() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(9);

		Optional<InventoryPartitioner.SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(firstSpace.get().firstSlot(), 9);
	}

	private static void addTwoAndRemoveFirstProperlyUpdatesFirstsSlots() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(0);

		Assertions.assertEquals(9, partitioner.getPartBySlot(0).getSlots());
	}

	private static void addTwoThanRemoveFirstAndThenSecondShowsAllSlotsAsReplaceable() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 9, dummyPartHandler);
		partitioner.addInventoryPart(9, 9, () -> "dummy2");
		partitioner.removeInventoryPart(0);
		partitioner.removeInventoryPart(9);

		Optional<InventoryPartitioner.SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(firstSpace.get().firstSlot(), 0);
		Assertions.assertEquals(partitioner.getPartBySlot(0), partitioner.getPartBySlot(80));
		Assertions.assertEquals(81, partitioner.getPartBySlot(0).getSlots());
	}

	private static void addPartReplacingAllSlotsAndRemovingThatProperlyUpdatesToDefault() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 81, dummyPartHandler);
		partitioner.removeInventoryPart(0);

		Assertions.assertTrue(partitioner.getPartBySlot(0) instanceof IInventoryPartHandler.Default);
		Assertions.assertTrue(partitioner.getPartBySlot(80) instanceof IInventoryPartHandler.Default);
		Assertions.assertEquals(partitioner.getPartBySlot(0), partitioner.getPartBySlot(80));
		Assertions.assertEquals(81, partitioner.getPartBySlot(0).getSlots());
	}

	private static void addPartReplacingAllSlotsReturnsEmptyPartFromMaxSlotPlusOne() {
		InventoryHandler invHandler = getInventoryHandler(81);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		IInventoryPartHandler dummyPartHandler = () -> "dummy";
		partitioner.addInventoryPart(0, 81, dummyPartHandler);

		Assertions.assertEquals(IInventoryPartHandler.EMPTY, partitioner.getPartBySlot(81));
	}

	private static void getFirstSpaceReturnsCorrectRangeForSmallInventories(int slots) {
		InventoryHandler invHandler = getInventoryHandler(slots);

		InventoryPartitioner partitioner = new InventoryPartitioner(new CompoundTag(), invHandler, () -> null);

		Optional<InventoryPartitioner.SlotRange> firstSpace = partitioner.getFirstSpace(9);
		Assertions.assertTrue(firstSpace.isPresent());
		Assertions.assertEquals(slots, firstSpace.get().numberOfSlots());
		Assertions.assertEquals(0, firstSpace.get().firstSlot());
	}
}