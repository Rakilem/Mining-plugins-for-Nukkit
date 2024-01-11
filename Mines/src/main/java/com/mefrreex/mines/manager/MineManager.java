package com.mefrreex.mines.manager;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mefrreex.mines.Mines;
import com.mefrreex.mines.event.MineLoadEvent;
import com.mefrreex.mines.event.MineRemoveEvent;
import com.mefrreex.mines.event.MineSaveEvent;
import com.mefrreex.mines.mine.Mine;
import com.mefrreex.mines.utils.Point;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MineManager {
    
    @Getter
    private static final Map<Level, List<Mine>> mines = new HashMap<>();

    private static final Gson gson = new Gson();

    public static File getMinesFolder() {
        return new File(Mines.get().getDataFolder() + "/mines/");
    }

    public static Mine get(String name) {
        for (List<Mine> mines : MineManager.mines.values()) {
            for (Mine mine : mines) {
                if (mine.getName().equals(name)) return mine;
            }
        }
        return null;
    }

    public static List<Mine> get(Level level) {
        return mines.get(level);
    }

    public static List<Mine> getMinesAt(Position position) {
        List<Mine> list = new ArrayList<>();
        List<Mine> mines = MineManager.mines.get(position.getLevel());
        if (mines == null) {
            return list;
        }
        for (Mine mine : mines) {
            if (mine.isInMine(new Point(position))) list.add(mine);
        }
        return list;
    }

    /**
     * Load mine
     * @param file Mine file
     */
    @SneakyThrows
    public static boolean load(File file) {
        Mine mine = gson.fromJson(new JsonReader(new FileReader(file)), Mine.class);
        
        MineLoadEvent event = new MineLoadEvent(mine, file);
        Server.getInstance().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        mine.init(Mines.get());
        return true;
    }

    /**
     * Load all mines
     */
    public static void loadAll() {
        File dir = MineManager.getMinesFolder();
        for(File file : Objects.requireNonNull(dir.listFiles())){
            if(!file.isFile()) {
                continue;
            }
            MineManager.load(file);
        }
    }

    /**
     * Save mine
     * @param mine Mine class
     */
    @SneakyThrows
    public static boolean save(Mine mine) {
        String json = gson.toJson(mine);

        File file = new File(getMinesFolder() + "/" + mine.getName() + ".json");

        MineSaveEvent event = new MineSaveEvent(mine, file);
        Server.getInstance().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        if (!file.exists()) file.createNewFile();

        try (FileWriter writer = new FileWriter(file)){
            writer.write(json);
        }
        return true;
    }

    /**
     * Save all mines
     */
    public static void saveAll() {
        for (List<Mine> mines : mines.values()) {
            for (Mine mine : mines) MineManager.save(mine);
        }
    }

    /**
     * Remove mine
     * @param mine Mine class
     */
    public static boolean remove(Mine mine) {
        if (!mines.containsKey(mine.getLevel())) {
            return false;
        }
        File file = new File(getMinesFolder() + "/" + mine.getName() + ".json");

        MineRemoveEvent event = new MineRemoveEvent(mine, file);
        Server.getInstance().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        mines.get(mine.getLevel()).remove(mine);
        return file.delete();
    }
}
