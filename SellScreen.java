package pl.autosell;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SellScreen extends Screen {
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int PER_PAGE = 4;
    private static final int W = 340, H = 256;

    private static class Entry {
        String id;
        Map<String, Integer> ench;
        String label;
    }

    private List<Entry> entries;
    private int page = 0;
    private int left, top;

    public SellScreen() {
        super(Text.literal("AutoSell"));
    }

    private List<Entry> buildEntries(SellConfig c) {
        List<Entry> list = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (client != null && client.player != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = client.player.getInventory().getStack(i);
                if (s.isEmpty()) continue;
                Entry e = new Entry();
                e.id = ItemInfo.itemId(s);
                e.ench = ItemInfo.enchants(s);
                e.label = ItemInfo.describe(s);
                if (seen.add(e.id + e.ench)) list.add(e);
            }
        }
        // reguly dla przedmiotow, ktorych aktualnie nie masz
        for (SellConfig.Rule r : c.rules) {
            if (seen.add(r.item + r.enchants)) {
                Entry e = new Entry();
                e.id = r.item;
                e.ench = r.enchants;
                e.label = r.label;
                list.add(e);
            }
        }
        return list;
    }

    private SellConfig.Rule findRule(SellConfig c, Entry e) {
        for (SellConfig.Rule r : c.rules) {
            if (r.item.equals(e.id) && r.enchants.equals(e.ench)) return r;
        }
        return null;
    }

    private void setPrice(Entry e, String text) {
        SellConfig c = SellConfig.get();
        int price = 0;
        try { price = Integer.parseInt(text); } catch (Exception ignored) {}
        SellConfig.Rule r = findRule(c, e);
        if (price > 0) {
            if (r == null) {
                r = new SellConfig.Rule();
                r.item = e.id;
                r.enchants = new LinkedHashMap<>(e.ench);
                r.label = e.label;
                c.rules.add(r);
            }
            r.price = price;
        } else if (r != null) {
            c.rules.remove(r);
        }
    }

    @Override
    protected void init() {
        SellConfig c = SellConfig.get();
        if (entries == null) entries = buildEntries(c);
        left = width / 2 - W / 2;
        top = height / 2 - H / 2;
        int x = left + 14, fw = W - 28;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(c.enabled ? "AutoSell: WŁĄCZONY" : "AutoSell: WYŁĄCZONY")
                        .formatted(c.enabled ? Formatting.GREEN : Formatting.RED),
                b -> { c.enabled = !c.enabled; SellConfig.save(); clearAndInit(); })
                .dimensions(x, top + 30, fw, 20).build());

        TextFieldWidget cmd = new TextFieldWidget(textRenderer, x, top + 68, fw, 18, Text.empty());
        cmd.setMaxLength(100);
        cmd.setText(c.command);
        cmd.setChangedListener(s -> c.command = s);
        addDrawableChild(cmd);

        TextFieldWidget interval = new TextFieldWidget(textRenderer, x, top + 102, 80, 18, Text.empty());
        interval.setMaxLength(4);
        interval.setTextPredicate(s -> s.matches("\\d*"));
        interval.setText(String.valueOf(c.intervalSeconds));
        interval.setChangedListener(s -> {
            try { c.intervalSeconds = Math.max(1, Math.min(3600, Integer.parseInt(s))); } catch (Exception ignored) {}
        });
        addDrawableChild(interval);

        int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= entries.size()) break;
            final Entry e = entries.get(idx);
            SellConfig.Rule r = findRule(c, e);

            TextFieldWidget price = new TextFieldWidget(textRenderer, left + W - 14 - 70, top + 142 + i * 22, 70, 18, Text.empty());
            price.setMaxLength(9);
            price.setTextPredicate(s -> s.matches("\\d*"));
            price.setText(r == null ? "" : String.valueOf(r.price));
            price.setChangedListener(s -> setPrice(e, s));
            addDrawableChild(price);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    if (page > 0) { page--; clearAndInit(); }
                }).dimensions(x, top + H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    if ((page + 1) * PER_PAGE < entries.size()) { page++; clearAndInit(); }
                }).dimensions(x + 24, top + H - 26, 20, 18).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left, top, left + W, top + H, 0xE6151520);
        ctx.drawBorder(left, top, W, H, 0xFF3A3A55);
        ctx.fill(left + 12, top + 8, left + 15, top + 22, ACCENT);
        ctx.fill(left + 12, top + 25, left + W - 12, top + 26, 0xFF3A3A55);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = left + 14;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Auto").formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal("Sell").formatted(Formatting.WHITE)), left + 20, top + 11, 0xFFFFFF);

        ctx.drawTextWithShadow(textRenderer, "KOMENDA  ({price} = cena)", x, top + 57, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "CO ILE SEK.", x, top + 91, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "Wpisz cenę obok przedmiotu. Puste = nie wystawiaj.", x + 96, top + 107, 0x888888);
        ctx.drawTextWithShadow(textRenderer, "PRZEDMIOTY W EKWIPUNKU", x, top + 128, ACCENT);

        SellConfig c = SellConfig.get();
        if (entries.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Ekwipunek jest pusty.", x, top + 146, 0xAAAAAA);
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= entries.size()) break;
            Entry e = entries.get(idx);
            boolean active = findRule(c, e) != null;
            String line = textRenderer.trimToWidth(e.label, W - 28 - 70 - 8);
            ctx.drawTextWithShadow(textRenderer, line, x, top + 147 + i * 22, active ? 0x55FF55 : 0xFFFFFF);
        }

        int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
        ctx.drawTextWithShadow(textRenderer, (page + 1) + "/" + pages, x + 52, top + H - 22, 0xAAAAAA);
        String hint = "K = menu, J = wystaw trzymany";
        ctx.drawTextWithShadow(textRenderer, hint, left + W - 14 - textRenderer.getWidth(hint), top + H - 22, 0x888888);
    }

    @Override
    public void close() {
        SellConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
