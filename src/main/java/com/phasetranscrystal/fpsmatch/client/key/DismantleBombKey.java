package com.phasetranscrystal.fpsmatch.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.client.data.ClientData;
import com.phasetranscrystal.fpsmatch.client.screen.CSGameShopScreen;
import com.phasetranscrystal.fpsmatch.entity.CompositionC4Entity;
import com.phasetranscrystal.fpsmatch.item.CompositionC4;
import com.phasetranscrystal.fpsmatch.net.BombActionC2SPacket;
import icyllis.modernui.mc.forge.MuiForgeApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DismantleBombKey {
    public static final KeyMapping DISMANTLE_BOMB_KEY = new KeyMapping("key.fpsm.dismantle_bomb.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            "key.category.fpsm");

    @SubscribeEvent
    public static void onInspectPress(InputEvent.Key event) {
        boolean isInGame = isInGame();
        boolean check_ = DISMANTLE_BOMB_KEY.matches(event.getKey(), event.getScanCode());
        if(isInGame){
            if (event.getAction() == GLFW.GLFW_REPEAT && check_) {
                check();
            }else if ((event.getAction() == GLFW.GLFW_RELEASE && !check_) || (event.getAction() == GLFW.GLFW_PRESS && !check_) || (event.getAction() == GLFW.GLFW_REPEAT && !check_)) {
                if(ClientData.bombUUID != null) FPSMatch.INSTANCE.sendToServer(new BombActionC2SPacket(0,ClientData.bombUUID));
            }else if (event.getAction() == GLFW.GLFW_PRESS && check_) {
                check();
            }
        }
    }

    public static void check(){
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return;
        }
        HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(player,(entity -> entity instanceof CompositionC4Entity),2);
        if(hitResult instanceof EntityHitResult result){
            if(ClientData.dismantleBombStates != 1) FPSMatch.INSTANCE.sendToServer(new BombActionC2SPacket(1,result.getEntity().getUUID()));
        }else{
            if(ClientData.dismantleBombStates != 0 && ClientData.bombUUID != null) FPSMatch.INSTANCE.sendToServer(new BombActionC2SPacket(0,ClientData.bombUUID));
        }
    }

}
