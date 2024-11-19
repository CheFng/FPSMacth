package com.phasetranscrystal.fpsmatch.cs;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.*;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.data.save.FileHelper;
import com.phasetranscrystal.fpsmatch.core.map.BlastModeMap;
import com.phasetranscrystal.fpsmatch.core.map.ShopMap;
import com.phasetranscrystal.fpsmatch.net.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.fpsmatch.net.CSGameSettingsS2CPacket;
import icyllis.modernui.ModernUI;
import icyllis.modernui.mc.forge.ModernUIForge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CSGameMap extends BaseMap implements BlastModeMap , ShopMap {
    public static final int WINNER_ROUND = 13;
    public static final int PAUSE_TIME = 2400;
    public static final int WINNER_WAITING_TIME = 160;
    public static final int WARM_UP_TIME = 1200;
    private final int waittingTime = 400;
    private int currentPauseTime = 0;
    private final int roundTimeLimit = 115 * 20;
    private int currentRoundTime = 0;
    private boolean isError = false;
    private boolean isPause = false;
    private boolean isWaiting = false;
    private boolean isWarmTime = false;
    private boolean isWaitingWinner = false;
    private final Map<String,Integer> teamScores = new HashMap<>();
    private int isBlasting = 0; // 是否放置炸弹 0 = 未放置 | 1 = 已放置 | 2 = 已拆除
    private boolean isExploded = false; // 炸弹是否爆炸
    private final List<AreaData> bombAreaData = new ArrayList<>();
    private String blastTeam;
    private final FPSMShop shop;

    public CSGameMap(ServerLevel serverLevel,String mapName) {
        super(serverLevel,mapName);
        this.shop = FileHelper.loadShopData(FPSMCore.getInstance().archiveName, this);
    }

    public Map<String,Integer> getTeams(){
        Map<String,Integer> teams = new HashMap<>();
        teams.put("ct",5);
        teams.put("t",5);
        this.setBlastTeam("t");
        return teams;
    }

    @Override
    public FPSMShop getShop() {
        return shop;
    }

    @Override
    public @Nullable ShopData defineShopData() {
        return null;
    }

    @Override
    public void tick() {
        if(isStart){
            if (!checkPauseTime() & !checkWarmUpTime() & !checkWaitingTime()) {
                if(!isRoundTimeEnd()){
                    if(!this.isDebug()){
                        switch (this.isBlasting()){
                            case 1 : this.checkBlastingVictory(); break;
                            case 2 : if(!isWaitingWinner) this.roundVictory("ct"); break;
                            default : this.checkRoundVictory(); break;
                        }

                        if(this.isWaitingWinner){
                            checkWinnerTime();

                            if(this.currentPauseTime >= WINNER_WAITING_TIME){
                                this.startNewRound();
                            }
                        }
                    }
                }else{
                    if(!checkWinnerTime()){
                        this.roundVictory("ct");
                    }else if(this.currentPauseTime >= WINNER_WAITING_TIME){
                        this.startNewRound();
                    }
                }
            }
        }
    }

    public void startGame(){
        AtomicBoolean checkFlag = new AtomicBoolean(true);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player = this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if (player != null){
                BaseTeam team = this.getMapTeams().getTeamByPlayer(player);
                if(team == null) checkFlag.set(false);
            }else{
                checkFlag.set(false);
            }
        }));

        if (!checkFlag.get() && !this.isError) return;
        startNewRound();
    }

    public boolean canRestTime(){
        return !this.isPause && !this.isWarmTime && !this.isWaiting && !this.isWaitingWinner;
    }
    public boolean checkPauseTime(){
        if(this.isPause && currentPauseTime < PAUSE_TIME){
            this.currentPauseTime++;
        }else{
            if(this.canRestTime()) currentPauseTime = 0;
            isPause = false;
        }
        return this.isPause;
    }

    public boolean checkWarmUpTime(){
        if(this.isWarmTime && currentPauseTime < WARM_UP_TIME){
            this.currentPauseTime++;
        }else {
            if(this.canRestTime()) currentPauseTime = 0;
            isWarmTime = false;
        }
        return this.isWarmTime;
    }

    public boolean checkWaitingTime(){
        if(this.isWaiting && currentPauseTime < waittingTime){
            this.currentPauseTime++;
        }else {
            if(this.canRestTime()) currentPauseTime = 0;
            isWaiting = false;
        }
        return this.isWaiting;
    }

    public boolean checkWinnerTime(){
        if(this.isWaitingWinner && currentPauseTime < WINNER_WAITING_TIME){
            this.currentPauseTime++;
        }else{
            if(this.canRestTime()) currentPauseTime = 0;
        }
        return this.isWaitingWinner;
    }

    public void checkRoundVictory(){
        if(isWaitingWinner) return;
        Map<String, List<UUID>> teamsLiving = this.getMapTeams().getTeamsLiving();
        if(teamsLiving.size() == 1){
            String winnerTeam = teamsLiving.keySet().stream().findFirst().get();
            this.roundVictory(winnerTeam);
        }
    }
    public void checkBlastingVictory(){
        if(isWaitingWinner) return;
        if(this.isExploded()) {
            this.roundVictory("t");
        }
    }
    public boolean isRoundTimeEnd(){
        if(this.isBlasting() > 0){
            this.currentRoundTime = -1;
            return false;
        }
        if(this.currentRoundTime < this.roundTimeLimit){
            this.currentRoundTime++;
        }
        return this.currentRoundTime >= this.roundTimeLimit;
    }

    private void roundVictory(String teamName) {
        if(isWaitingWinner) return;
        this.isWaitingWinner = true;
        int currentScore = this.teamScores.getOrDefault(teamName, 0);
        this.teamScores.put(teamName, currentScore + 1);
    }

    public void startNewRound() {
        this.isStart = true;
        this.isWaiting = true;
        this.isWaitingWinner = false;
        this.getMapTeams().resetLivingPlayers();
        this.getMapTeams().setTeamsSpawnPoints();
        this.cleanupMap();
    }

    @Override
    public void victory() {
        resetGame();
    }
    @Override
    public boolean victoryGoal() {
        AtomicBoolean isVictory = new AtomicBoolean(false);
        teamScores.values().forEach((integer -> {
            isVictory.set(integer >= WINNER_ROUND);
        }));
        return isVictory.get() && !this.isDebug();
    }

    @Override
    public void cleanupMap() {
        this.setBlasting(0);
        this.setExploded(false);
        this.currentRoundTime = 0;
        this.currentPauseTime = 0;
        this.getMapTeams().setTeamsSpawnPoints();
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player =  this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if(player != null){
                player.heal(player.getMaxHealth());
                player.setGameMode(GameType.ADVENTURE);
                this.clearPlayerInventory(player);
                this.teleportPlayerToReSpawnPoint(player);
            }
        }));

    }

    public void teleportPlayerToReSpawnPoint(ServerPlayer player){
        BaseTeam team = this.getMapTeams().getTeamByPlayer(player);
        if (team == null) return;
        SpawnPointData data = Objects.requireNonNull(team.getPlayerData(player.getUUID())).getSpawnPointsData();
        BlockPos pos = data.getPosition();
        float f = Mth.wrapDegrees(data.getYaw());
        float f1 = Mth.wrapDegrees(data.getPitch());
        player.teleportTo(Objects.requireNonNullElse(this.getServerLevel().getServer().getLevel(data.getDimension()),this.getServerLevel()),pos.getX(),pos.getY(),pos.getZ(),f, f1);
    }

    public void clearPlayerInventory(ServerPlayer player){
        player.getInventory().clearOrCountMatchingItems((p_180029_) -> true, -1, player.inventoryMenu.getCraftSlots());
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.slotsChanged(player.getInventory());
    }

    public void resetGame() {
        this.teamScores.clear();
        this.isError = false;
        this.isStart = false;
        this.isWaiting = false;
        this.isWaitingWinner = false;
        this.isWarmTime = false;
        this.currentRoundTime = 0;
        this.currentPauseTime = 0;
        this.getMapTeams().reset();
    }


    public final void setBlastTeam(String team){
        this.blastTeam = team;
    }

    public boolean checkCanPlacingBombs(String team){
        if(this.blastTeam == null) return false;
        return this.blastTeam.equals(team);
    }

    public boolean checkPlayerIsInBombArea(Player player){
        AtomicBoolean a = new AtomicBoolean(false);
        this.bombAreaData.forEach(area->{
            if(!a.get()) a.set(area.isPlayerInArea(player));
        });
        return a.get();
    }

    public void addBombArea(AreaData area){
        this.bombAreaData.add(area);
    }

    public List<AreaData> getBombAreaData() {
        return bombAreaData;
    }

    public void setBlasting(int blasting) {
        isBlasting = blasting;
    }
    public void setExploded(boolean exploded) {
        isExploded = exploded;
    }

    public int isBlasting() {
        return isBlasting;
    }

    public boolean isExploded() {
        return isExploded;
    }
    public void syncToClient() {
        CSGameSettingsS2CPacket packet = new CSGameSettingsS2CPacket(this.teamScores.getOrDefault("ct",0),this.teamScores.getOrDefault("t",0), this.currentPauseTime,this.currentRoundTime,this.isDebug(),this.isStart,this.isError,this.isPause,this.isWaiting,this.isWaitingWinner);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player = (ServerPlayer) this.getServerLevel().getPlayerByUUID(uuid);
            if(player != null){
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> player), packet);
            }
        }));
    }

}
