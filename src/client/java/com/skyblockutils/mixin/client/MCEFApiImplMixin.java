package com.skyblockutils.mixin.client;

import org.cef.CefSettings;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Pseudo
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFApiImpl", remap = false)
public class MCEFApiImplMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "FIELD", target = "Lorg/cef/CefSettings;user_agent_product:Ljava/lang/String;", opcode = Opcodes.PUTFIELD),
            require = 0
    )
    private void ssu$keepChromeUserAgent(CefSettings settings, String product) {
    }

    @ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lme/friwi/jcefmaven/CefAppBuilder;addJcefArgs([Ljava/lang/String;)V"),
            require = 0
    )
    private String[] ssu$removeBotFlags(String[] args) {
        return Arrays.stream(args).filter(arg -> !arg.equals("--disable-web-security")).toArray(String[]::new);
    }
}