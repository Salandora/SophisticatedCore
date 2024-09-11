package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.ShapedRecipeAccessor;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.ShapelessRecipeAccessor;

import java.util.Optional;

// Fabric does not have a NBT storing recipe so we make our own
public class SCShapelessRecipe extends ShapelessRecipe {
	public SCShapelessRecipe(String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
		super(group, category, result, ingredients);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.SHAPLESS_RECIPE_SERIALIZER;
	}

	public static class Serializer implements RecipeSerializer<SCShapelessRecipe> {
		public static final Codec<ItemStack> ITEMSTACK_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
							ExtraCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
							ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, "nbt").forGetter(itemStack -> Optional.ofNullable(itemStack.getTag()))
					)
					.apply(instance, ItemStack::new)
		);

		public static final Codec<SCShapelessRecipe> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
								ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapelessRecipe::getGroup),
								CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
								ITEMSTACK_CODEC.fieldOf("result").forGetter(shapelessRecipe -> ((ShapelessRecipeAccessor) shapelessRecipe).getResult()),
								Ingredient.CODEC_NONEMPTY
										.listOf()
										.fieldOf("ingredients")
										.flatXmap(
												list -> {
													Ingredient[] ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
													if (ingredients.length == 0) {
														return DataResult.error(() -> "No ingredients for shapeless recipe");
													} else {
														return ingredients.length > 9
																? DataResult.error(() -> "Too many ingredients for shapeless recipe")
																: DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
													}
												},
												DataResult::success
										)
										.forGetter(ShapelessRecipe::getIngredients)
						)
						.apply(instance, SCShapelessRecipe::new)
		);

		@Override
		public Codec<SCShapelessRecipe> codec() {
			return CODEC;
		}

		public SCShapelessRecipe fromNetwork(FriendlyByteBuf buffer) {
			String string = buffer.readUtf();
			CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);

			NonNullList<Ingredient> ingredients = NonNullList.withSize(buffer.readVarInt(), Ingredient.EMPTY);
			ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));

			ItemStack itemStack = buffer.readItem();
			return new SCShapelessRecipe(string, craftingBookCategory, itemStack, ingredients);
		}

		public void toNetwork(FriendlyByteBuf buffer, SCShapelessRecipe recipe) {
			buffer.writeUtf(recipe.getGroup());
			buffer.writeEnum(recipe.category());
			recipe.getIngredients().forEach(ingredient -> ingredient.toNetwork(buffer));
			buffer.writeItem(((ShapedRecipeAccessor) recipe).getResult());
		}
	}
}
