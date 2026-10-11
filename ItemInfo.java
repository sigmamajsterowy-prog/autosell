package pl.autosell;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemInfo {
    private ItemInfo() {}

    private static final Map<String, String> PL = Map.ofEntries(
            Map.entry("protection", "Ochrona"),
            Map.entry("fire_protection", "Ochrona przed ogniem"),
            Map.entry("blast_protection", "Ochrona przed wybuchami"),
            Map.entry("projectile_protection", "Ochrona przed pociskami"),
            Map.entry("feather_falling", "Lekkie lądowanie"),
            Map.entry("thorns", "Ciernie"),
            Map.entry("respiration", "Oddychanie"),
            Map.entry("aqua_affinity", "Powinowactwo wodne"),
            Map.entry("depth_strider", "Wodny chodziarz"),
            Map.entry("frost_walker", "Lodowy chodziarz"),
            Map.entry("soul_speed", "Szybkość duszy"),
            Map.entry("swift_sneak", "Szybkie skradanie"),
            Map.entry("unbreaking", "Niezniszczalność"),
            Map.entry("mending", "Naprawa"),
            Map.entry("sharpness", "Ostrość"),
            Map.entry("smite", "Pogromca nieumarłych"),
            Map.entry("bane_of_arthropods", "Zmora stawonogów"),
            Map.entry("knockback", "Odrzut"),
            Map.entry("fire_aspect", "Aspekt ognia"),
            Map.entry("looting", "Grabież"),
            Map.entry("sweeping_edge", "Szerokie cięcie"),
            Map.entry("efficiency", "Wydajność"),
            Map.entry("silk_touch", "Jedwabny dotyk"),
            Map.entry("fortune", "Szczęście"),
            Map.entry("power", "Moc"),
            Map.entry("punch", "Uderzenie"),
            Map.entry("flame", "Płomień"),
            Map.entry("infinity", "Nieskończoność"),
            Map.entry("luck_of_the_sea", "Szczęście morza"),
            Map.entry("lure", "Przynęta"),
            Map.entry("loyalty", "Lojalność"),
            Map.entry("riptide", "Torpeda"),
            Map.entry("channeling", "Przewodnictwo"),
            Map.entry("multishot", "Wielostrzał"),
            Map.entry("piercing", "Przebicie"),
            Map.entry("quick_charge", "Szybkie ładowanie")
    );

    public static String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    public static Map<String, Integer> enchants(ItemStack stack) {
        Map<String, Integer> m = new LinkedHashMap<>();
        add(m, stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        add(m, stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        return m;
    }

    private static void add(Map<String, Integer> m, ItemEnchantmentsComponent c) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : c.getEnchantmentEntries()) {
            String id = e.getKey().getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            m.merge(id, e.getIntValue(), Math::max);
        }
    }

    /** np. "Diamentowe spodnie, Niezniszczalność 3, Ochrona 3" */
    public static String describe(String name, Map<String, Integer> enchants) {
        StringBuilder sb = new StringBuilder(name);
        for (Map.Entry<String, Integer> e : enchants.entrySet()) {
            String n = PL.getOrDefault(e.getKey(), e.getKey().replace('_', ' '));
            sb.append(", ").append(n).append(' ').append(e.getValue());
        }
        return sb.toString();
    }

    public static String describe(ItemStack stack) {
        return describe(stack.getName().getString(), enchants(stack));
    }
}
