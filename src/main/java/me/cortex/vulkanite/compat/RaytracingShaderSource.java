package me.cortex.vulkanite.compat;

import net.irisshaders.iris.properties.ProgramDirectives;
import net.irisshaders.iris.programs.ProgramSet;

public class RaytracingShaderSource {
    public record RayHitSource(String close, String any, String intersection) {}
    public final String name;
    public final String raygen;
    public final String[] raymiss;
    public final RayHitSource[] rayhit;

    public RaytracingShaderSource(String name, String raygen, String[] raymiss, RayHitSource... rayhit) {
        this.name = name;
        this.raygen = raygen;
        this.raymiss = raymiss;
        this.rayhit = rayhit;
    }
}
