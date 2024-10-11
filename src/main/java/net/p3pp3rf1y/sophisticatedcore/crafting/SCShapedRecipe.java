package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.ShapedRecipeAccessor;

import java.util.Optional;

// Fabric does not have a NBT storing recipe so we make our own
public class SCShapedRecipe extends ShapedRecipe {
	public SCShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
		super(group, category, pattern, result, showNotification);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.SHAPED_RECIPE_SERIALIZER;
	}

	public static class Serializer implements RecipeSerializer<SCShapedRecipe> {
		public static final Codec<ItemStack> ITEMSTACK_CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
								BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
								ExtraCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
								ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, "nbt").forGetter(itemStack -> Optional.ofNullable(itemStack.getTag()))
						)
						.apply(instance, ItemStack::new)
		);

		public static final Codec<SCShapedRecipe> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
								ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapedRecipe::getGroup),
								CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
								ShapedRecipePattern.MAP_CODEC.forGetter(shapedRecipe -> ((ShapedRecipeAccessor) shapedRecipe).getPattern()),
								ITEMSTACK_CODEC.fieldOf("result").forGetter(shapedRecipe -> ((ShapedRecipeAccessor) shapedRecipe).getResult()),
								ExtraCodecs.strictOptionalField(Codec.BOOL, "show_notification", true).forGetter(ShapedRecipe::showNotification)
						)
						.apply(instance, SCShapedRecipe::new)
		);

		@Override
		public Codec<SCShapedRecipe> codec() {
			return CODEC;
		}

		public SCShapedRecipe fromNetwork(FriendlyByteBuf buffer) {
			String string = buffer.readUtf();
			CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
			ShapedRecipePattern shapedRecipePattern = ShapedRecipePattern.fromNetwork(buffer);
			ItemStack itemStack = buffer.readItem();
			boolean bl = buffer.readBoolean();
			return new SCShapedRecipe(string, craftingBookCategory, shapedRecipePattern, itemStack, bl);
		}

		public void toNetwork(FriendlyByteBuf buffer, SCShapedRecipe recipe) {
			buffer.writeUtf(recipe.getGroup());
			buffer.writeEnum(recipe.category());
			((ShapedRecipeAccessor) recipe).getPattern().toNetwork(buffer);
			buffer.writeItem(((ShapedRecipeAccessor) recipe).getResult());
			buffer.writeBoolean(recipe.showNotification());
		}
	}
}
