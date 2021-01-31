package me.geek.tom.slimeforfabric;

import com.mojang.brigadier.Command;
import me.geek.tom.slimeforfabric.deser.SlimeDeserialiser;
import me.geek.tom.slimeforfabric.io.OkioSinkOutput;
import me.geek.tom.slimeforfabric.io.OkioSourceInput;
import me.geek.tom.slimeforfabric.ser.SlimeSerialiser;
import me.geek.tom.slimeforfabric.util.ChunkArea;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.argument.NbtCompoundTagArgumentType.getCompoundTag;
import static net.minecraft.command.argument.NbtCompoundTagArgumentType.nbtCompound;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SlimeForFabric implements ModInitializer {
    @SuppressWarnings("CodeBlock2Expr")
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("slime")
                    .requires(s -> s.hasPermissionLevel(4))
                    .then(literal("write")
                            .then(argument("output", string())
                                    .then(argument("x", integer()).then(argument("z", integer()).then(argument("width", integer()).then(argument("depth", integer()).then(argument("custom", nbtCompound())
                                            .executes(ctx -> {
                                                ServerWorld world = ctx.getSource().getWorld();
                                                ChunkArea area = new ChunkArea(
                                                        getInteger(ctx, "x"), getInteger(ctx, "z"),
                                                        getInteger(ctx, "width"), getInteger(ctx, "depth")
                                                );
                                                String output = getString(ctx, "output");
                                                Path worldsPath = FabricLoader.getInstance().getGameDir().resolve("slime_worlds");
                                                if (!Files.exists(worldsPath)) {
                                                    try {
                                                        Files.createDirectories(worldsPath);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                        return 0;
                                                    }
                                                }

                                                Path outputPath = worldsPath.resolve(output.trim() + ".slimem");
                                                UtilsKt.write(outputPath, buffer -> {
                                                    try {
                                                        SlimeSerialiser.INSTANCE.serialiseWorld(new OkioSinkOutput(buffer), world, area, getCompoundTag(ctx, "custom"));
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        throw e;
                                                    }
                                                });
                                                ctx.getSource().sendFeedback(new LiteralText("Wrote to: " + outputPath.getFileName()), false);

                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )))))
                            )
                    ).then(literal("load")
                            .then(argument("name", string()).executes(ctx -> {
                                ServerWorld world = ctx.getSource().getWorld();
                                String name = getString(ctx, "name");
                                Path worldsPath = FabricLoader.getInstance().getGameDir().resolve("slime_worlds");
                                if (!Files.exists(worldsPath)) {
                                    try {
                                        Files.createDirectories(worldsPath);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return 0;
                                    }
                                }
                                Path inputPath = worldsPath.resolve(name + ".slimem");
                                if (!Files.exists(inputPath)) {
                                    ctx.getSource().sendError(new LiteralText("404: Not found: " + inputPath));
                                    return 0;
                                }

                                UtilsKt.read(inputPath, buffer -> {
                                    try {
                                        SlimeDeserialiser.INSTANCE.deserialise(new OkioSourceInput(buffer), world);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                                ctx.getSource().sendFeedback(new LiteralText("Loaded (most) data from: " + inputPath.getFileName()), false);

                                return Command.SINGLE_SUCCESS;
                            }))
                    )
            );
        });
    }
}
