package net.p3pp3rf1y.sophisticatedcore.client.model;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UnbakedGeometryHelper {
	private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

	public static List<BlockElement> createUnbakedItemElements(int layer, SpriteContents sprite) {
		return ITEM_MODEL_GENERATOR.processFrames(layer, "layer" + layer, sprite);
	}

	public static List<BlockElement> createUnbakedItemMaskElements(int layer, SpriteContents maskSprite) {
		List<BlockElement> elements = Lists.newArrayList();

		int width = maskSprite.width();
		int height = maskSprite.height();
		var transparentBitMask = createBitMask(width, height, maskSprite);

		for (int y = 0; y < height; y++) {
			int runStart = -1;
			for (int x = 0; x <= width; x++) { // include grid to flush last run
				boolean visible = (x < width) && transparentBitMask.get(x + y * width);
				if (visible) {
					if (runStart == -1) {
						runStart = x;
					}
				} else if (runStart != -1) {
					Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
					for (Direction dir : Direction.values()) {
						faces.put(dir, new BlockElementFace(
								null,
								layer,
								"layer" + layer,
								new BlockFaceUV(null, 0)));
					}

					elements.add(new BlockElement(
							new Vector3f(16 * (float) runStart / width, 16 - 16.0f * (y + 1) / height, 7.5f),
							new Vector3f(16 * (float) x / width, 16 - 16.0f * y / height, 8.5f),
							faces,
							null,
							true
					));

					for (int cx = runStart; cx < x; cx++) {
						transparentBitMask.clear(y * width + x);
					}

					runStart = -1;
				}
			}
		}

		return elements;
	}

	public static List<BakedQuad> bakeElements(List<BlockElement> elements, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
		if (elements.isEmpty()) {
			return List.of();
		}

		var quads = Lists.<BakedQuad>newArrayList();
		for (BlockElement element : elements) {
			for (var entry : element.faces.entrySet()) {
				BlockElementFace face = entry.getValue();

				Material material = new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(face.texture()));
				TextureAtlasSprite sprite = spriteGetter.apply(material);
				quads.add(BlockModel.bakeFace(element, face, sprite, entry.getKey(), state));
			}
		}
		return quads;
	}


	private static BitSet createBitMask(int width, int height, SpriteContents maskSprite) {
		BitSet transparent = new BitSet(width * height);
		maskSprite.getUniqueFrames().forEach(frame -> {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					if (!maskSprite.isTransparent(frame, x, y)) {
						transparent.set(x+ y * width);
					}
				}
			}
		});

		return transparent;
	}
}
