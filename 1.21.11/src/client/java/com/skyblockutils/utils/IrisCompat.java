package com.skyblockutils.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class IrisCompat {
    private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
    private static final MethodHandle GET_IRIS_API = getIrisApiHandle();
    private static final MethodHandle REGISTER_PIPELINE = getRegisterPipelineHandle();
    private static final MethodHandle GET_IRIS_PROGRAM = getIrisProgramHandle();

    private static ThreadLocal<Boolean> skipExtension = null;
    private static boolean skipInitialized = false;

    public static void assignPipeline(RenderPipeline pipeline, String irisProgramName) {
        if (!IRIS_LOADED || GET_IRIS_API == null || REGISTER_PIPELINE == null || GET_IRIS_PROGRAM == null) return;
        try {
            REGISTER_PIPELINE.invoke(GET_IRIS_API.invoke(), pipeline, GET_IRIS_PROGRAM.invoke(irisProgramName));
        } catch (IllegalStateException ignored) {
        } catch (Throwable e) {
            System.out.println("[SSU] Failed to assign pipeline to Iris: " + e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void initSkipExtension() {
        if (skipInitialized) return;
        skipInitialized = true;
        try {
            Class<?> immediateState = Class.forName("net.irisshaders.iris.vertices.ImmediateState");
            java.lang.reflect.Field f = immediateState.getDeclaredField("skipExtension");
            f.setAccessible(true);
            skipExtension = (ThreadLocal<Boolean>) f.get(null);
        } catch (Throwable ignored) {
        }
    }

    public static void beginCustomDraw() {
        initSkipExtension();
        if (skipExtension != null) skipExtension.set(true);
    }

    public static void endCustomDraw() {
        if (skipExtension != null) skipExtension.set(false);
    }

    public static void assignPipelines() {
        assignPipeline(WaypointRenderer.BADGE_PIPELINE, "BASIC_COLOR");
    }

    private static MethodHandle getIrisApiHandle() {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return MethodHandles.publicLookup().findStatic(irisApiClass, "getInstance", MethodType.methodType(irisApiClass));
        } catch (Exception e) {
            return null;
        }
    }

    private static MethodHandle getRegisterPipelineHandle() {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Class<?> irisProgramClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            return MethodHandles.publicLookup().findVirtual(irisApiClass, "assignPipeline", MethodType.methodType(void.class, RenderPipeline.class, irisProgramClass));
        } catch (Exception e) {
            return null;
        }
    }

    private static MethodHandle getIrisProgramHandle() {
        try {
            Class<?> irisProgramClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            MethodHandle enumValueOf = MethodHandles.publicLookup().findStatic(Enum.class, "valueOf", MethodType.methodType(Enum.class, Class.class, String.class));
            return MethodHandles.insertArguments(enumValueOf, 0, irisProgramClass);
        } catch (Exception e) {
            return null;
        }
    }
}