package com.phasetranscrystal.fpsmatch.mixin;

import com.phasetranscrystal.fpsmatch.core.BaseMap;
import com.phasetranscrystal.fpsmatch.core.BaseTeam;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BlastModeMap;
import com.phasetranscrystal.fpsmatch.item.CompositionC4;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {
    @Shadow public abstract ItemStack getItem();

    @Inject(at = @At("HEAD"), method = "playerTouch", cancellable = true)
    public void fpsMatch$playerTouch$CustomC4(Player player, CallbackInfo ci) {
        if(!player.isCreative() && !player.level().isClientSide){
            if(this.getItem().getItem() instanceof CompositionC4){
                BaseMap map = FPSMCore.getInstance().getMapByPlayer(player);
                if (map == null) {
                    ci.cancel();
                    return;
                }
                BaseTeam team = map.getMapTeams().getTeamByPlayer(player);
                if(team != null && map instanceof BlastModeMap<?> blastModeMap){
                    if(!blastModeMap.checkCanPlacingBombs(team.getName())){
                        ci.cancel();
                        return;
                    }
                }else{
                    ci.cancel();
                }
            }
        }
    }
}
