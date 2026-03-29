package com.skyblockutils.utils;

import net.minecraft.client.MinecraftClient;

import java.io.File;

public class ClipboardUtils {
    public static void copyImageToClipboard(String filename) {
        File file = new File(MinecraftClient.getInstance().runDirectory, "screenshots/" + filename);
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                String[] cmd = {
                        "powershell", "-command",
                        "Add-Type -AssemblyName System.Windows.Forms; " +
                                "[System.Windows.Forms.Clipboard]::SetImage(" +
                                "[System.Drawing.Image]::FromFile('" + file.getAbsolutePath().replace("'", "''") + "'))"
                };
                Runtime.getRuntime().exec(cmd);
            } else if (os.contains("mac")) {
                String[] cmd = {
                        "osascript", "-e",
                        "set the clipboard to (read (POSIX file \"" + file.getAbsolutePath() + "\") as JPEG picture)"
                };
                Runtime.getRuntime().exec(cmd);
            } else {
                String path = file.getAbsolutePath();
                try {
                    String[] cmd = {"bash", "-c", "xclip -selection clipboard -t image/png -i \"" + path + "\""};
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    if (p.exitValue() != 0) throw new Exception("xclip failed");
                } catch (Exception e) {
                    String[] cmd = {"bash", "-c", "xsel --clipboard --input < \"" + path + "\""};
                    Runtime.getRuntime().exec(cmd);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
