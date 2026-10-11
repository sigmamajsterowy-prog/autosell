package pl.autosell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AutoSeller {
    private AutoSeller() {}

    private static int cooldown = 40;
    private static int delay = -1;
    private static int slot;
    private static SellConfig.Rule rule;

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) { delay = -1; return; }

        // czekamy chwilę po zmianie slotu / przeniesieniu przedmiotu
        if (delay >= 0) {
            if (delay == 0) { fire(mc); delay = -1; } else delay--;
            return;
        }

        SellConfig c = SellConfig.get();
        if (!c.enabled || mc.currentScreen != null) return;
        if (p.currentScreenHandler != p.playerScreenHandler) return;
        if (--cooldown > 0) return;

        // szukamy w calym ekwipunku: najpierw hotbar (0-8), potem plecak (9-35)
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getStack(i);
            for (SellConfig.Rule r : c.rules) {
                if (!r.matches(s)) continue;

                rule = r;
                if (i < 9) {
                    slot = i;
                    p.getInventory().selectedSlot = i;
                } else {
                    // przerzuc przedmiot z plecaka na aktualnie wybrany slot hotbara
                    slot = p.getInventory().selectedSlot;
                    mc.interactionManager.clickSlot(
                            p.playerScreenHandler.syncId, i, slot, SlotActionType.SWAP, p);
                }
                delay = 4;
                cooldown = Math.max(5, c.intervalSeconds) * 20;
                return;
            }
        }
        cooldown = 20; // nic do wystawienia, sprawdz za sekunde
    }

    /** Klawisz J: wystawia to, co trzymasz w rece (jesli pasuje do reguly). */
    public static void sellHeld(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        ItemStack s = p.getMainHandStack();
        for (SellConfig.Rule r : SellConfig.get().rules) {
            if (r.matches(s)) {
                slot = p.getInventory().selectedSlot;
                rule = r;
                fire(mc);
                return;
            }
        }
        p.sendMessage(Text.literal("[AutoSell] Brak reguły dla tego przedmiotu.").formatted(Formatting.RED), false);
    }

    private static void fire(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || rule == null) return;
        ItemStack s = p.getInventory().getStack(slot);
        if (!rule.matches(s)) return;

        String desc = ItemInfo.describe(s);
        String cmd = SellConfig.get().command.replace("{price}", String.valueOf(rule.price));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        p.networkHandler.sendChatCommand(cmd);

        p.sendMessage(Text.literal("[AutoSell] ").formatted(Formatting.DARK_PURPLE)
                .append(Text.literal("Wystawiono: " + desc + " za " + rule.price).formatted(Formatting.WHITE)), false);
    }
}
