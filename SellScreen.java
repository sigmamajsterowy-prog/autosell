package pl.autosell;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SellScreen extends Screen {
    private static final int PURPLE = 0xFF8B5CF6;
    private static final int NAVY = 0xFF3B5FE0;
    private static final int NAVY_LIGHT = 0xFF7C9CFF;
    private static final int PANEL = 0xE60A0F1E;
    private static final int TEXT = 0xFFE8EDFA;
    private static final int MUTED = 0xFF8791AB;
    private static final int PW = 360;

    private static final int T_SECTION = 0, T_TOGGLE = 1, T_FIELD = 2, T_HINT = 3, T_ENTRY = 4, T_INTERVAL = 5;

    private static class Entry {
        String id;
        Map<String, Integer> ench;
        String label;
    }

    private static class Item {
        int type, y, h, color, fieldW;
        String text;
        TextFieldWidget field;
        Entry entry;
    }

    private final List<Item> items = new ArrayList<>();
    private List<Entry> entries;
    private int left, top, panelH, vx, vy, vw, vh, cw, contentH, maxScroll;
    private float scroll, targetScroll, toggleAnim = -1f;
    private boolean dragBar;

    public SellScreen() {
        super(Text.literal("AutoSell"));
    }

    // ---------- dane ----------

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

    // ---------- budowa układu ----------

    private TextFieldWidget makeField(int w, int maxLen, boolean digits, String text, Consumer<String> listener) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, 0, 0, w, 10, Text.empty());
        f.setDrawsBackground(false);
        f.setEditableColor(TEXT);
        f.setMaxLength(maxLen);
        if (digits) f.setTextPredicate(s -> s.matches("\\d*"));
        f.setText(text);
        f.setChangedListener(listener);
        addSelectableChild(f);
        return f;
    }

    private int add(int type, int y, int h, int gap, String text, int color, TextFieldWidget f, int fw, Entry e) {
        Item it = new Item();
        it.type = type;
        it.y = y;
        it.h = h;
        it.text = text;
        it.color = color;
        it.field = f;
        it.fieldW = fw;
        it.entry = e;
        items.add(it);
        return y + h + gap;
    }

    private int section(int y, String text, int color) {
        if (y > 0) y += 8;
        return add(T_SECTION, y, 10, 5, text, color, null, 0, null);
    }

    @Override
    protected void init() {
        SellConfig c = SellConfig.get();
        if (entries == null) entries = buildEntries(c);
        if (toggleAnim < 0) toggleAnim = c.enabled ? 1f : 0f;

        panelH = Math.max(160, Math.min(height - 16, 340));
        left = width / 2 - PW / 2;
        top = height / 2 - panelH / 2;
        vx = left + 12;
        vy = top + 42;
        vw = PW - 24;
        vh = panelH - 42 - 12;
        cw = vw - 8;

        items.clear();
        int y = 0;

        y = section(y, "USTAWIENIA", PURPLE);
        y = add(T_TOGGLE, y, 26, 6, "Automatyczne wystawianie", PURPLE, null, 0, null);

        y = section(y, "KOMENDA WYSTAWIANIA", PURPLE);
        TextFieldWidget cmd = makeField(cw - 24, 100, false, c.command, s -> c.command = s);
        y = add(T_FIELD, y, 26, 4, null, PURPLE, cmd, cw - 24, null);
        y = add(T_HINT, y, 10, 4, "{price} zamieni się na cenę. Dopasuj komendę do swojego serwera.", MUTED, null, 0, null);

        y = section(y, "INTERWAŁ", NAVY_LIGHT);
        TextFieldWidget interval = makeField(40, 4, true, String.valueOf(c.intervalSeconds), s -> {
            try { c.intervalSeconds = Math.max(1, Math.min(3600, Integer.parseInt(s))); } catch (Exception ignored) {}
        });
        y = add(T_INTERVAL, y, 26, 6, "Sekundy między wystawieniami", NAVY_LIGHT, interval, 40, null);

        y = section(y, "PRZEDMIOTY W EKWIPUNKU", NAVY_LIGHT);
        y = add(T_HINT, y, 10, 4, "Wpisz cenę obok przedmiotu. Puste pole = nie wystawiaj.", MUTED, null, 0, null);
        if (entries.isEmpty()) {
            y = add(T_HINT, y, 10, 4, "Ekwipunek jest pusty.", MUTED, null, 0, null);
        }
        for (Entry e : entries) {
            SellConfig.Rule r = findRule(c, e);
            TextFieldWidget f = makeField(62, 9, true, r == null ? "" : String.valueOf(r.price), s -> setPrice(e, s));
            y = add(T_ENTRY, y, 24, 4, e.label, NAVY_LIGHT, f, 62, e);
        }

        contentH = y + 6;
        maxScroll = Math.max(0, contentH - vh);
        targetScroll = Math.min(targetScroll, maxScroll);
        scroll = Math.min(scroll, maxScroll);
    }

    // ---------- rysowanie ----------

    private static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        c.fill(x + r, y, x + w - r, y + r, col);
        c.fill(x + r, y + h - r, x + w - r, y + h, col);
        for (int i = 0; i < r; i++) {
            double cy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - cy * cy));
            c.fill(x + inset, y + i, x + r, y + i + 1, col);
            c.fill(x + w - r, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + r, y + h - i, col);
            c.fill(x + w - r, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    private static void rline(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x + r, y, x + w - r, y + 1, col);
        c.fill(x + r, y + h - 1, x + w - r, y + h, col);
        c.fill(x, y + r, x + 1, y + h - r, col);
        c.fill(x + w - 1, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double cy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - cy * cy));
            c.fill(x + inset, y + i, x + inset + 1, y + i + 1, col);
            c.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + inset + 1, y + h - i, col);
            c.fill(x + w - inset - 1, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int thumbH() {
        return Math.max(18, (int) ((long) vh * vh / Math.max(1, contentH)));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        SellConfig c = SellConfig.get();

        scroll += (targetScroll - scroll) * 0.3f;
        if (Math.abs(targetScroll - scroll) < 0.4f) scroll = targetScroll;
        toggleAnim += ((c.enabled ? 1f : 0f) - toggleAnim) * 0.3f;
        int sy = Math.round(scroll);

        // panel
        rrect(ctx, left - 1, top - 1, PW + 2, panelH + 2, 8, 0x30000000);
        rrect(ctx, left, top, PW, panelH, 7, PANEL);
        ctx.fillGradient(left + 4, top + 1, left + PW - 4, top + 40, 0x338B5CF6, 0x008B5CF6);
        ctx.fillGradient(left + 4, top + panelH - 34, left + PW - 4, top + panelH - 1, 0x003B5FE0, 0x2A3B5FE0);
        rline(ctx, left, top, PW, panelH, 7, 0x33FFFFFF);

        // naglowek
        ctx.fill(left + 12, top + 11, left + 15, top + 25, PURPLE);
        ctx.drawTextWithShadow(textRenderer, "Auto", left + 22, top + 14, 0xFFB79CFF);
        ctx.drawTextWithShadow(textRenderer, "Sell", left + 22 + textRenderer.getWidth("Auto"), top + 14, 0xFFFFFFFF);
        String ver = "1.0.0";
        ctx.drawTextWithShadow(textRenderer, ver, left + PW - 34 - textRenderer.getWidth(ver), top + 15, MUTED);
        boolean closeHover = inRect(mx, my, left + PW - 26, top + 9, 16, 16);
        rrect(ctx, left + PW - 26, top + 9, 16, 16, 4, closeHover ? 0x40FFFFFF : 0x1AFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "X", left + PW - 18, top + 13, TEXT);
        ctx.fill(left + 10, top + 34, left + PW - 10, top + 35, 0x22FFFFFF);

        // pozycje pol
        for (Item it : items) {
            if (it.field == null) continue;
            int iy = vy + it.y - sy;
            int fx;
            if (it.type == T_FIELD) fx = vx + 12;
            else fx = vx + cw - 8 - (it.fieldW + 10) + 5;
            it.field.setX(fx);
            it.field.setY(iy + (it.h - 8) / 2);
        }

        boolean mouseInView = inRect(mx, my, vx, vy, vw, vh);

        ctx.enableScissor(vx, vy, vx + vw, vy + vh);
        for (Item it : items) {
            int iy = vy + it.y - sy;
            if (iy > vy + vh || iy + it.h < vy) continue;

            if (it.type == T_SECTION) {
                ctx.fill(vx, iy + 1, vx + 2, iy + 9, it.color);
                ctx.drawTextWithShadow(textRenderer, it.text, vx + 7, iy + 1, it.color);
                continue;
            }
            if (it.type == T_HINT) {
                ctx.drawTextWithShadow(textRenderer, it.text, vx + 2, iy, MUTED);
                continue;
            }

            // karta (szklana)
            boolean hover = mouseInView && inRect(mx, my, vx, iy, cw, it.h);
            boolean active = false;
            if (it.type == T_ENTRY) active = findRule(c, it.entry) != null;
            rrect(ctx, vx, iy, cw, it.h, 5, active ? 0x333B5FE0 : 0x14FFFFFF);
            if (hover) rrect(ctx, vx, iy, cw, it.h, 5, 0x12FFFFFF);
            if (hover || active) rline(ctx, vx, iy, cw, it.h, 5, active ? 0x663B5FE0 : 0x22FFFFFF);
            if (active) ctx.fill(vx + 1, iy + 4, vx + 3, iy + it.h - 4, NAVY_LIGHT);

            int ty = iy + (it.h - 8) / 2;

            if (it.type == T_TOGGLE) {
                ctx.drawTextWithShadow(textRenderer, it.text, vx + 10, ty, TEXT);
                int sw = 30, sh = 14;
                int sx = vx + cw - 10 - sw, sY = iy + (it.h - sh) / 2;
                rrect(ctx, sx, sY, sw, sh, 7, c.enabled ? PURPLE : 0x33FFFFFF);
                if (c.enabled) rline(ctx, sx - 1, sY - 1, sw + 2, sh + 2, 8, 0x558B5CF6);
                int kx = sx + 2 + Math.round(toggleAnim * (sw - 14));
                rrect(ctx, kx, sY + 2, 10, 10, 5, 0xFFFFFFFF);
                continue;
            }

            if (it.type == T_FIELD) {
                int px = vx + 6, pw = cw - 12;
                boolean f = it.field.isFocused();
                rrect(ctx, px, iy + 4, pw, it.h - 8, 4, f ? 0x55000000 : 0x33000000);
                if (f) rline(ctx, px, iy + 4, pw, it.h - 8, 4, PURPLE);
                else if (hover) rline(ctx, px, iy + 4, pw, it.h - 8, 4, 0x33FFFFFF);
                it.field.render(ctx, mx, my, delta);
                continue;
            }

            // T_INTERVAL i T_ENTRY: etykieta po lewej, pole po prawej
            String label = it.text;
            if (it.type == T_ENTRY) {
                label = textRenderer.trimToWidth(label, cw - 20 - (it.fieldW + 10) - 6);
                ctx.drawTextWithShadow(textRenderer, label, vx + 10, ty, active ? 0xFFFFFFFF : 0xFFC9D2EA);
            } else {
                ctx.drawTextWithShadow(textRenderer, label, vx + 10, ty, TEXT);
            }
            int pillW = it.fieldW + 10;
            int px = vx + cw - 8 - pillW;
            boolean f = it.field.isFocused();
            rrect(ctx, px, iy + 4, pillW, it.h - 8, 4, f ? 0x55000000 : 0x2A000000);
            if (f) rline(ctx, px, iy + 4, pillW, it.h - 8, 4, NAVY_LIGHT);
            else if (hover) rline(ctx, px, iy + 4, pillW, it.h - 8, 4, 0x33FFFFFF);
            it.field.render(ctx, mx, my, delta);
            if (it.type == T_ENTRY && it.field.getText().isEmpty() && !f) {
                ctx.drawTextWithShadow(textRenderer, "cena", px + 5, ty, 0xFF59627A);
            }
        }

        // wygaszanie krawedzi listy
        if (sy > 0) ctx.fillGradient(vx, vy, vx + vw, vy + 8, 0xE60A0F1E, 0x000A0F1E);
        if (sy < maxScroll) ctx.fillGradient(vx, vy + vh - 8, vx + vw, vy + vh, 0x000A0F1E, 0xE60A0F1E);
        ctx.disableScissor();

        // pasek przewijania
        if (maxScroll > 0) {
            int tx = vx + vw - 4;
            rrect(ctx, tx, vy, 3, vh, 1, 0x14FFFFFF);
            int th = thumbH();
            int ty = vy + (int) ((scroll / maxScroll) * (vh - th));
            rrect(ctx, tx, ty, 3, th, 1, dragBar ? NAVY_LIGHT : NAVY);
        }
    }

    // ---------- mysz i klawiatura ----------

    private void dragTo(double my) {
        int th = thumbH();
        double ratio = (my - vy - th / 2.0) / Math.max(1, vh - th);
        float v = (float) Math.max(0, Math.min(1, ratio)) * maxScroll;
        scroll = v;
        targetScroll = v;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && inRect(mx, my, left + PW - 26, top + 9, 16, 16)) {
            close();
            return true;
        }
        if (!inRect(mx, my, vx, vy, vw, vh)) {
            setFocused(null);
            return true;
        }
        if (button == 0 && maxScroll > 0 && mx >= vx + vw - 8) {
            dragBar = true;
            dragTo(my);
            return true;
        }
        int sy = Math.round(scroll);
        for (Item it : items) {
            if (it.type == T_SECTION || it.type == T_HINT) continue;
            int iy = vy + it.y - sy;
            if (inRect(mx, my, vx, iy, cw, it.h)) {
                if (it.type == T_TOGGLE) {
                    SellConfig c = SellConfig.get();
                    c.enabled = !c.enabled;
                    SellConfig.save();
                    return true;
                }
                if (it.field != null) {
                    setFocused(it.field);
                    it.field.mouseClicked(mx, my, button);
                    return true;
                }
            }
        }
        setFocused(null);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragBar) {
            dragTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragBar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        targetScroll = (float) Math.max(0, Math.min(maxScroll, targetScroll - vertical * 30));
        return true;
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
