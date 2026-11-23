package com.skyblockutils.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFBrowserImpl", remap = false)
public class MCEFBrowserImplMixin {
    @Unique
    private static Unsafe ssu$unsafe;
    @Unique
    private static long ssu$scancodeOffset = -1;

    @ModifyArg(
            method = {"onKeyPressed", "onKeyReleased"},
            at = @At(value = "INVOKE", target = "Lnet/dimaskama/mcef/impl/MCEFBrowserImpl;sendKeyEvent(Ljava/awt/event/KeyEvent;)V"),
            require = 0
    )
    private java.awt.event.KeyEvent ssu$addScancode(java.awt.event.KeyEvent awtEvent, @Local(argsOnly = true, name = "event") KeyEvent event) {
        ssu$setScancode(awtEvent, event.scancode() & 0xFF);
        return awtEvent;
    }

    @Unique
    private static void ssu$setScancode(java.awt.event.KeyEvent awtEvent, long scancode) {
        if (ssu$scancodeOffset == -2) return;
        try {
            if (ssu$scancodeOffset == -1) {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                ssu$unsafe = (Unsafe) theUnsafe.get(null);
                ssu$scancodeOffset = ssu$unsafe.objectFieldOffset(java.awt.event.KeyEvent.class.getDeclaredField("scancode"));
            }
            ssu$unsafe.putLong(awtEvent, ssu$scancodeOffset, scancode);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            ssu$scancodeOffset = -2;
        }
    }
}
