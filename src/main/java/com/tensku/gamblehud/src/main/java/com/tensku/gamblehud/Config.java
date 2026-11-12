package com.tensku.gamblehud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class Config {
    public static Config I = new Config();

    public boolean paymentAlerts = true;
    public boolean alertSound = true;
    public boolean profitCard = true;
    public boolean payConfirm = false;
    public boolean chatColor = true;
    public int theme = 3; // Desert
    public double alertThreshold = 1_000_000;
    public double payThreshold = 1_000_000;
    public double dailyGoal = 0;

    public boolean auctionEnabled = false;
    public int winnerBannerSeconds = 5;
    public int resultDisplaySeconds = 20;
    public String host = "";
    public java.util.List<String> favourites = new java.util.ArrayList<>();

    public double balance = -1; // -1 = unknown (type /bal once)
    public double received = 0;
    public double sent = 0;
    public String day = LocalDate.now().toString();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("tenskugamblehud.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) I = c;
            }
        } catch (Exception ignored) { }
        I.rollDay();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(I));
        } catch (Exception ignored) { }
    }

    public void rollDay() {
        String today = LocalDate.now().toString();
        if (!today.equals(day)) {
            day = today;
            received = 0;
            sent = 0;
            save();
        }
    }

    public double net() {
        rollDay();
        return received - sent;
    }
}
