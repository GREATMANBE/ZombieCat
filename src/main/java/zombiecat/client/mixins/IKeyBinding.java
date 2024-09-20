package zombiecat.client.mixins;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({KeyBinding.class})
public interface IKeyBinding {
   @Accessor("pressTime")
   void setPressTime(int var1);
}
