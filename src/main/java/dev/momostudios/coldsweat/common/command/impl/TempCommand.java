package dev.momostudios.coldsweat.common.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.command.BaseCommand;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class TempCommand extends BaseCommand
{
    public TempCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return builder
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-150, 150))
                                        .executes(source -> executeSetPlayerTemp(
                                        source.getSource(), EntityArgument.getPlayers(source, "players"), IntegerArgumentType.getInteger(source, "amount")))
                                )
                        )
                )
                .then(Commands.literal("get")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(source -> executeGetPlayerTemp(
                                source.getSource(), EntityArgument.getPlayers(source, "players")))
                        )
                );
    }

    private int executeSetPlayerTemp(CommandSource source, Collection<ServerPlayerEntity> players, int amount)
    {
        // Set the temperature for all affected targets
        for (ServerPlayerEntity player : players)
        {
            player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
            {
                cap.set(Temperature.Types.CORE, amount);
                TempHelper.updateTemperature(player, cap, true);
            });
        }

        //Compose & send message
        if (players.size() == 1)
        {
            PlayerEntity target = players.iterator().next();

            source.sendFeedback(new TranslationTextComponent("commands.cold_sweat.temperature.set.single.result", target.getName().getString(), amount), true);
        }
        else
        {
            source.sendFeedback(new TranslationTextComponent("commands.cold_sweat.temperature.set.result", players.size(), amount), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeGetPlayerTemp(CommandSource source, Collection<ServerPlayerEntity> players)
    {
        for (ServerPlayerEntity target : players.stream().sorted(Comparator.comparing(player -> player.getName().getString())).collect(Collectors.toList()))
        {
            //Compose & send message
            source.sendFeedback(new TranslationTextComponent("commands.cold_sweat.temperature.get.result", target.getName().getString(),
                    (int) TempHelper.getTemperature(target, Temperature.Types.BODY).get()), false);
        }
        return Command.SINGLE_SUCCESS;
    }
}
