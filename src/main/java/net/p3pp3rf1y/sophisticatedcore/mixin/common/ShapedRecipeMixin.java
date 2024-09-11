package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.crafting.ShapedRecipe;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
	/*@Inject(method = "itemStackFromJson", at = @At("HEAD"), cancellable = true)
	private static void sophisticatedCore$useNbtData(JsonObject json, CallbackInfoReturnable<ItemStack> cir) {
		if (json.has("nbt") && json.get("nbt").isJsonPrimitive()) {
			CompoundTag tag = new CompoundTag();
			tag.putString("id", GsonHelper.getAsString(json, "item"));
			tag.putInt("Count", GsonHelper.getAsInt(json, "count", 1));

			try {
				String nbtString = GsonHelper.getAsString(json, "nbt");
				CompoundTag nbtTag = TagParser.parseTag(nbtString);
				tag.put("tag", nbtTag);
			}
			catch (CommandSyntaxException e) {
				throw new JsonSyntaxException("Could not parse NBT data " + e);
			}

			cir.setReturnValue(ItemStack.of(tag));
		}
	}*/
}
