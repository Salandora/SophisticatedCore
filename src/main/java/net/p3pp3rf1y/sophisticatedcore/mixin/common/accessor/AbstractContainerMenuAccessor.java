package net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Accessor
	List<ContainerListener> getContainerListeners();

	@Accessor
	NonNullList<ItemStack> getLastSlots();

	@Accessor
	int getQuickcraftType();
	@Accessor
	void setQuickcraftType(int quickcraftType);
	@Accessor
	int getQuickcraftStatus();
	@Accessor
	void setQuickcraftStatus(int quickcraftStatus);
	@Accessor
	Set<Slot> getQuickcraftSlots();

	@Accessor
	ItemStack getRemoteCarried();
	@Accessor
	void setRemoteCarried(ItemStack remoteCarried);
	@Accessor
	NonNullList<ItemStack> getRemoteSlots();

	@Accessor
	boolean getSuppressRemoteUpdates();
	@Accessor
	void setSuppressRemoteUpdates(boolean suppressRemoteUpdates);

	@Nullable
	@Accessor
	ContainerSynchronizer getSynchronizer();

	@Invoker
	SlotAccess callCreateCarriedSlotAccess();

	@Invoker
	void callSynchronizeCarriedToRemote();
}
