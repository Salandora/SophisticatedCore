package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
	@Inject(method = "itemStackFromJson", at = @At("HEAD"), cancellable = true)
	private static void sophisticatedCore$useNbtData(JsonObject json, CallbackInfoReturnable<ItemStack> cir) {
		if (json.has("nbt")) {
			CompoundTag tag = new CompoundTag();
			tag.putString("id", GsonHelper.getAsString(json, "item"));
			tag.putInt("Count", GsonHelper.getAsInt(json, "count", 1));

			try {
				JsonElement element = json.get("nbt");
				CompoundTag nbt = TagParser.parseTag(GsonHelper.convertToString(element, "nbt"));
				tag.put("tag", nbt);
			}
			catch (CommandSyntaxException e) {
				throw new JsonSyntaxException("Could not parse NBT data " + e);
			}

			cir.setReturnValue(ItemStack.of(tag));
		}
	}
}
