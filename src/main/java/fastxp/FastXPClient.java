package fastxp;

import fastxp.config.FastXPConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public class FastXPClient implements ClientModInitializer {

    private double tickAccumulator = 0.0;

    @Override
    public void onInitializeClient() {
        registerCommands();
        registerTickEvent();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("xpbottle")
                .then(ClientCommandManager.literal("on").executes(context -> {
                    FastXPConfig.enabled = true;
                    context.getSource().sendFeedback(Text.literal("§a[FastXP] Enabled"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(context -> {
                    FastXPConfig.enabled = false;
                    context.getSource().sendFeedback(Text.literal("§c[FastXP] Disabled"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("maxcps")
                    .then(ClientCommandManager.argument("cps", IntegerArgumentType.integer(0, 50))
                        .executes(context -> {
                            int cps = IntegerArgumentType.getInteger(context, "cps");
                            FastXPConfig.maxCps = cps;
                            context.getSource().sendFeedback(Text.literal("§e[FastXP] Max CPS set to " + cps));
                            return 1;
                        })))
            );
        });
    }

    private void registerTickEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (!FastXPConfig.enabled || FastXPConfig.maxCps <= 0) return;

            boolean holdingMain = client.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE);
            boolean holdingOff = client.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE);

            if ((holdingMain || holdingOff) && client.options.useKey.isPressed()) {
                double usesPerTick = FastXPConfig.maxCps / 20.0;
                tickAccumulator += usesPerTick;

                while (tickAccumulator >= 1.0) {
                    Hand hand = holdingMain ? Hand.MAIN_HAND : Hand.OFF_HAND;
                    client.interactionManager.interactItem(client.player, hand);
                    tickAccumulator -= 1.0;
                }
            } else {
                tickAccumulator = 0.0;
            }
        });
    }
}