package pl.autosell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SellConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autosell.json");
    private static SellConfig instance = new SellConfig();

    public boolean enabled = false;
    /** {price} zostanie zamienione na cenę. Dopasuj do komendy swojego serwera. */
    public String command = "ah sell {price}";
    public int intervalSeconds = 30;
    public List<Rule> rules = new ArrayList<>();

    public static class Rule {
        public String item;                 // np. minecraft:diamond_leggings
        public String label;                // opis do wyświetlania
        public int price;
        public Map<String, Integer> enchants = new LinkedHashMap<>(); // minimalne wymagane poziomy

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !ItemInfo.itemId(stack).equals(item)) return false;
            Map<String, Integer> have = ItemInfo.enchants(stack);
            for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                if (have.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
            }
            return true;
        }
    }

    public static SellConfig get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                SellConfig c = GSON.fromJson(Files.readString(FILE), SellConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
