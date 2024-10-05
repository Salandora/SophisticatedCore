package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;

public abstract class SimpleIdentifiablePrepareableReloadListener<T> extends SimplePreparableReloadListener<T> implements IdentifiableResourceReloadListener {
    private final ResourceLocation id;

    public SimpleIdentifiablePrepareableReloadListener(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getFabricId() {
        return id;
    }

    @Override
    protected T prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }
}
