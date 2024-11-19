package com.phasetranscrystal.fpsmatch.core;

import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseMap{
    public final String mapName;
    public String gameType = "error";
    public boolean isStart = false;
    private boolean isDebug = false;
    private final ServerLevel serverLevel;
    private MapTeams mapTeams;

    public BaseMap(ServerLevel serverLevel, String mapName) {
        this.serverLevel = serverLevel;
        this.mapName = mapName;
        this.setMapTeams(new MapTeams(this.getServerLevel(),this.getTeams()));
    }

    public Map<String,Integer> getTeams(){
        Map<String,Integer> teams = new HashMap<>();
        teams.put("teamA",5);
        teams.put("teamB",5);
        return teams;
    }

    public final void setMapTeams(MapTeams teams){
        this.mapTeams = teams;
    }

    public final void mapTick(){
        checkForVictory();
        tick();
        syncToClient();
    }

   public abstract void syncToClient();

    public void tick(){
    }

    // 检查胜利条件
    public final void checkForVictory() {
        if (this.victoryGoal()) {
            this.victory();
        }
    }

    public abstract void startGame();

    public boolean checkGameHasPlayer(Player player){
        boolean flag = false;
        if(!this.getMapTeams().getJoinedPlayers().contains(player.getUUID()) || getMapTeams().getTeamByPlayer(player) != null){
            flag = true;
        }else if(this.getMapTeams().getJoinedPlayers().contains(player.getUUID())){
            flag = true;
        }
        return flag;
    }
    public  void startNewRound(){}
    public abstract void victory();
    public abstract boolean victoryGoal();
    public abstract void cleanupMap();
    public abstract void resetGame();

    public MapTeams getMapTeams() {
        return mapTeams;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean switchDebugMode(){
        this.isDebug = !this.isDebug;
        return this.isDebug;
    }


    public String getMapName(){
        return mapName;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getGameType() {
        return gameType;
    }

    public boolean isStart() {
        return isStart;
    }

}