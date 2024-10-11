package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class StorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements DraggableStackVisitor<S> {
	@Override
	public DraggedAcceptorResult acceptDraggedStack(DraggingContext<S> context, DraggableStack stack) {
		Stream<BoundsProvider> bounds = getDraggableAcceptingBounds(context, stack);
		Point cursor = context.getCurrentPosition();
		if (cursor != null) {
			int x = cursor.getX();
			int y = cursor.getY();
			Optional<BoundsProvider> target = bounds.filter(b -> {
				AABB box = b.bounds().bounds();
				double minX = box.minX;
				double minY = box.minY;
				double maxX = box.maxX;
				double maxY = box.maxY;
				return x >= minX && x <= maxX && y >= minY && y <= maxY && b instanceof GhostTarget;
			}).findFirst();
			if (target.isPresent()) {
				//noinspection unchecked
				GhostTarget<ItemStack, ? extends StorageScreenBase<?>> ghost = (GhostTarget<ItemStack, ? extends StorageScreenBase<?>>) target.get();
				Object held = stack.getStack().getValue();
				if (held instanceof ItemStack item) {
					ghost.accept(item);
					return DraggedAcceptorResult.CONSUMED;
				}
			}
		}
		return DraggableStackVisitor.super.acceptDraggedStack(context, stack);
	}

	@Override
	public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<S> context, DraggableStack stack) {
		List<BoundsProvider> targets = new ArrayList<>();
		StorageContainerMenuBase<?> screen = context.getScreen().getMenu();

		if (stack.getStack().getValue() instanceof ItemStack ghostStack) {
			screen.getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
				if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
					targets.add(new GhostTarget<>(context.getScreen(), ghostStack, s));
				}
			}));
		}

		return targets.stream();
	}

	private static class GhostTarget<I, S extends StorageScreenBase<?>> implements BoundsProvider {
		private final Rectangle area;
		private final Slot slot;
		private final ItemStack stack;

		public GhostTarget(S screen, ItemStack stack, Slot slot) {
			this.slot = slot;
			this.stack = stack;
			this.area = new Rectangle(((AbstractContainerScreenAccessor) screen).getGuiLeft() + slot.x, ((AbstractContainerScreenAccessor) screen).getGuiTop() + slot.y, 16, 16);
		}

		public void accept(I ingredient) {
			PacketHelper.sendToServer(new SetGhostSlotMessage(stack, slot.index));
		}

		@Override
		public VoxelShape bounds() {
			return BoundsProvider.fromRectangle(area);
		}
	}
}
