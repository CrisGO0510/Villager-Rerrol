package com.tumod.villagerreroll;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    public static KeyBinding TOGGLE;

    public static void register() {
        TOGGLE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.villagerreroll.toggle",
                GLFW.GLFW_KEY_R,
                Category.MISC
            )
        );
    }
}
