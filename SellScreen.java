package pl.autosell;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class SellScreen extends Screen {
    private static final int ACCENT = 0xFF8B5CF6;
    private static final int PER_PAGE = 4;
    private static final int W = 340, H = 256;

    private int page = 0;
    private String priceText = "400";
    private int left, top;

    public SellScreen() {
        super(Text.literal("AutoSell"));
    }

    @Override
    protected void init() {
        SellConfig c = SellConfig.get();
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

        TextFieldWidget price = new TextFieldWidget(textRenderer, x + 96, top + 102, 80, 18, Text.empty());
        price.setMaxLength(12);
        price.setTextPredicate(s -> s.matches("\\d*"));
        price.setText(priceText);
        price.setChangedListener(s -> priceText = s);
        addDrawableChild(price);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Dodaj trzymany"), b -> addHeld())
                .dimensions(x + 186, top + 101, fw - 186, 20).build());

        // lista reguł
        List<SellConfig.Rule> rules = c.rules;
        int pages = Math.max(1, (rules.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= rules.size()) break;
            final int fi = idx;
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                        rules.remove(fi); SellConfig.save(); clearAndInit();
                    }).dimensions(left + W - 14 - 20, top + 142 + i * 22, 20, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { if (page > 0) { page--; clearAndInit(); } })
                .dimensions(x, top + H - 26, 20, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    if ((page + 1) * PER_PAGE < rules.size()) { page++; clearAndInit(); }
                }).dimensions(x + 24, top + H - 26, 20, 18).build());
    }

    private void addHeld() {
        if (client == null || client.player == null) return;
        ItemStack s = client.player.getMainHandStack();
        if (s.isEmpty()) {
            client.player.sendMessage(Text.literal("[AutoSell] Trzymaj przedmiot w ręce.").formatted(Formatting.RED), false);
            return;
        }
        int price;
        try { price = Integer.parseInt(priceText); } catch (Exception e) { return; }
        if (price <= 0) return;

        String id = ItemInfo.itemId(s);
        var ench = ItemInfo.enchants(s);
        SellConfig c = SellConfig.get();
        c.rules.removeIf(r -> r.item.equals(id) && r.enchants.equals(ench));

        SellConfig.Rule r = new SellConfig.Rule();
        r.item = id;
        r.enchants = ench;
        r.price = price;
        r.label = ItemInfo.describe(s);
        c.rules.add(r);
        SellConfig.save();
        clearAndInit();
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
        ctx.drawTextWithShadow(textRenderer, "CENA", x + 96, top + 91, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "REGUŁY (trzymaj przedmiot i kliknij Dodaj)", x, top + 128, ACCENT);

        List<SellConfig.Rule> rules = SellConfig.get().rules;
        if (rules.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Brak reguł.", x, top + 146, 0xAAAAAA);
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= rules.size()) break;
            SellConfig.Rule r = rules.get(idx);
            String line = r.label + " → " + r.price;
            line = textRenderer.trimToWidth(line, W - 28 - 26);
            ctx.drawTextWithShadow(textRenderer, line, x, top + 146 + i * 22, 0xFFFFFF);
        }

        int pages = Math.max(1, (rules.size() + PER_PAGE - 1) / PER_PAGE);
        ctx.drawTextWithShadow(textRenderer, (page + 1) + "/" + pages, x + 52, top + H - 22, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "K = menu, J = wystaw trzymany", left + W - 14 - textRenderer.getWidth("K = menu, J = wystaw trzymany"), top + H - 22, 0x888888);
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
