package com.github.salandora.sophisticatedcorebuild;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.github.salandora.sophisticatedcorebuild.processor.DeduplicationProcessor;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SophisticatedCoreBuildPlugin implements Plugin<Project> {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final List<ProjectProcessor> lateProcessors = List.of(
			new DeduplicationProcessor()
	);

	@Override
	public void apply(@NotNull Project project) {
		project.afterEvaluate(p -> this.lateProcessors.forEach(processor -> processor.apply(p)));
	}
}