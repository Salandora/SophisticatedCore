package net.p3pp3rf1y.sophisticatedcore.upgrades.feeding;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class FeedingUpgradeWrapper extends UpgradeWrapperBase<FeedingUpgradeWrapper, FeedingUpgradeItem> implements ITickableUpgrade, IFilteredUpgrade {
	private static final int COOLDOWN = 100;
	private static final int STILL_HUNGRY_COOLDOWN = 10;
	private static final int FEEDING_RANGE = 3;
	private final FilterLogic filterLogic;

	public FeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ItemStack::isEdible);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
		if (isInCooldown(world) || (entity != null && !(entity instanceof Player))) {
			return;
		}

		boolean hungryPlayer = false;
		if (entity == null) {
			AtomicBoolean stillHungryPlayer = new AtomicBoolean(false);
			world.getEntities(EntityType.PLAYER, new AABB(pos).inflate(FEEDING_RANGE), p -> true).forEach(p -> stillHungryPlayer.set(stillHungryPlayer.get() || feedPlayerAndGetHungry(p, world)));
			hungryPlayer = stillHungryPlayer.get();
		} else {
			if (feedPlayerAndGetHungry((Player) entity, world)) {
				hungryPlayer = true;
			}
		}
		if (hungryPlayer) {
			setCooldown(world, STILL_HUNGRY_COOLDOWN);
			return;
		}

		setCooldown(world, COOLDOWN);
	}

	private boolean feedPlayerAndGetHungry(Player player, Level world) {
		int hungerLevel = 20 - player.getFoodData().getFoodLevel();
		if (hungerLevel == 0) {
			return false;
		}
		return tryFeedingFoodFromStorage(world, hungerLevel, player) && player.getFoodData().getFoodLevel() < 20;
	}

	private boolean tryFeedingFoodFromStorage(Level world, int hungerLevel, Player player) {
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		SlottedStackStorage inventory = storageWrapper.getInventoryForUpgradeProcessing();
		AtomicBoolean fedPlayer = new AtomicBoolean(false);
		InventoryHelper.iterate(inventory, (slot, stack) -> {
			if (isEdible(stack) && filterLogic.matchesFilter(stack) && (isHungryEnoughForFood(hungerLevel, stack) || shouldFeedImmediatelyWhenHurt() && hungerLevel > 0 && isHurt)) {
				ItemStack mainHandItem = player.getMainHandItem();
				// Changed for compatibility with rpg inventory
				player.setItemInHand(InteractionHand.MAIN_HAND, stack); // player.getInventory().items.set(player.getInventory().selected, stack);
				if (stack.use(world, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
					InteractionResultHolder<ItemStack> result = UseItemCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND);
					ItemStack containerItem = result.getObject();
					if (result.getResult() == InteractionResult.PASS) {
						containerItem = stack.getItem().finishUsingItem(stack, world, player);
					}

					// Changed for compatibility with rpg inventory
					player.setItemInHand(InteractionHand.MAIN_HAND, mainHandItem); //player.getInventory().items.set(player.getInventory().selected, mainHandItem);
					inventory.setStackInSlot(slot, stack);
					if (!ItemStack.matches(containerItem, stack)) {
						InventoryHelper.insertOrDropItem(player, containerItem, inventory, PlayerInventoryStorage.of(player));
					}
					fedPlayer.set(true);
					return true;
				}
				// Changed for compatibility with rpg inventory
				player.setItemInHand(InteractionHand.MAIN_HAND, mainHandItem); //player.getInventory().items.set(player.getInventory().selected, mainHandItem);
			}
			return false;
		}, () -> false, ret -> ret);
		return fedPlayer.get();
	}

	private static boolean isEdible(ItemStack stack) {
		if (!stack.isEdible()) {
			return false;
		}
		FoodProperties foodProperties = stack.getItem().getFoodProperties();
		return foodProperties != null && foodProperties.getNutrition() >= 1;
	}

	private boolean isHungryEnoughForFood(int hungerLevel, ItemStack stack) {
		FoodProperties foodProperties = stack.getItem().getFoodProperties();
		if (foodProperties == null) {
			return false;
		}

		HungerLevel feedAtHungerLevel = getFeedAtHungerLevel();
		if (feedAtHungerLevel == HungerLevel.ANY) {
			return true;
		}

		int nutrition = foodProperties.getNutrition();
		return (feedAtHungerLevel == HungerLevel.HALF ? (nutrition / 2) : nutrition) <= hungerLevel;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public HungerLevel getFeedAtHungerLevel() {
		return NBTHelper.getEnumConstant(upgrade, "feedAtHungerLevel", HungerLevel::fromName).orElse(HungerLevel.HALF);
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		NBTHelper.setEnumConstant(upgrade, "feedAtHungerLevel", hungerLevel);
		save();
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		return NBTHelper.getBoolean(upgrade, "feedImmediatelyWhenHurt").orElse(true);
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		NBTHelper.setBoolean(upgrade, "feedImmediatelyWhenHurt", feedImmediatelyWhenHurt);
		save();
	}
}
