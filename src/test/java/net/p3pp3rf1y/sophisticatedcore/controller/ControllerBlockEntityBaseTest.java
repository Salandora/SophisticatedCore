package net.p3pp3rf1y.sophisticatedcore.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

class ControllerBlockEntityBaseTest {

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/**
	 * Minimal concrete controller. Its level stays null, which is exactly what
	 * {@link net.p3pp3rf1y.sophisticatedcore.util.WorldHelper#getLoadedBlockEntity} sees when a linked storage is
	 * unavailable - chunk unloaded or block entity gone - so getHandlerFromIndex resolves to EmptyItemHandler through
	 * the real production path instead of a stub.
	 */
	private static final class TestController extends ControllerBlockEntityBase {
		private TestController() {
			super(Mockito.mock(BlockEntityType.class), BlockPos.ZERO, Blocks.AIR.defaultBlockState());
		}
	}

	/**
	 * Builds a controller whose persisted slot bookkeeping advertises slots that no reachable handler backs, which is
	 * the state a controller is legitimately in while one of its storages sits in an unloaded chunk.
	 */
	private static ControllerBlockEntityBase controllerWith(int totalSlots, int[] baseIndexes, BlockPos... storagePositions) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("totalSlots", totalSlots);

		if (baseIndexes.length > 0) {
			ListTag indexes = new ListTag();
			for (int baseIndex : baseIndexes) {
				indexes.add(IntTag.valueOf(baseIndex));
			}
			tag.put("baseIndexes", indexes);
		}

		if (storagePositions.length > 0) {
			ListTag positions = new ListTag();
			for (BlockPos storagePosition : storagePositions) {
				positions.add(LongTag.valueOf(storagePosition.asLong()));
			}
			tag.put("storagePositions", positions);
		}

		ControllerBlockEntityBase controller = new TestController();
		controller.load(tag);
		return controller;
	}

	private static ControllerBlockEntityBase controllerWithUnavailableStorage() {
		return controllerWith(27, new int[] {27}, new BlockPos(1, 0, 0));
	}

	@Test
	void iteratingSlotsDoesNotThrowWhenBackingStorageIsUnavailable() {
		ControllerBlockEntityBase controller = controllerWithUnavailableStorage();

		Assertions.assertEquals(27, controller.getSlotCount());

		SingleSlotStorage<ItemVariant> slot = Assertions.assertDoesNotThrow(
				() -> controller.getSlots().iterator().next(),
				"Iterating a controller's slots must not throw while one of its storages is unavailable");

		Assertions.assertTrue(slot.isResourceBlank());
		Assertions.assertEquals(0, slot.getAmount());
	}

	@Test
	void slotOfUnavailableStorageNeitherAcceptsNorYieldsItems() {
		ControllerBlockEntityBase controller = controllerWithUnavailableStorage();

		SingleSlotStorage<ItemVariant> slot = Assertions.assertDoesNotThrow(() -> controller.getSlot(0));

		ItemVariant stone = ItemVariant.of(Items.STONE);
		try (Transaction transaction = Transaction.openOuter()) {
			Assertions.assertEquals(0L, slot.insert(stone, 1, transaction));
			Assertions.assertEquals(0L, slot.extract(stone, 1, transaction));
			transaction.commit();
		}

		Assertions.assertTrue(slot.isResourceBlank());
	}

	@Test
	void getSlotDoesNotThrowWhenBaseIndexesCannotResolveTheSlot() {
		ControllerBlockEntityBase controller = controllerWith(1, new int[0]);

		SingleSlotStorage<ItemVariant> slot = Assertions.assertDoesNotThrow(
				() -> controller.getSlot(0),
				"An unresolvable handler index must degrade to an empty slot rather than throw");

		Assertions.assertTrue(slot.isResourceBlank());
		Assertions.assertEquals(0, slot.getAmount());
	}

	@Test
	void getSlotStillThrowsForIndexesOutsideTotalSlots() {
		ControllerBlockEntityBase controller = controllerWithUnavailableStorage();

		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> controller.getSlot(27));
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> controller.getSlot(-1));
	}

	@Test
	void siblingSlotAccessorsKeepDegradingGracefully() {
		ControllerBlockEntityBase controller = controllerWithUnavailableStorage();

		Assertions.assertEquals(ItemStack.EMPTY, controller.getStackInSlot(0));
		Assertions.assertEquals(0, controller.getSlotLimit(0));
		Assertions.assertFalse(controller.isItemValid(0, ItemVariant.of(Items.STONE), 1));
	}
}
