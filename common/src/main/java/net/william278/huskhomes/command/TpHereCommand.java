package net.william278.huskhomes.command;

import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.player.OnlineUser;
import net.william278.huskhomes.teleport.Teleport;
import net.william278.huskhomes.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metadevs.redistab.redistab.RedisTabAPI;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TpHereCommand extends CommandBase implements TabCompletable {

    protected TpHereCommand(@NotNull HuskHomes implementor) {
        super("tphere", Permission.COMMAND_TPHERE, implementor, "tpohere");
    }

    @Override
    public void onExecute(@NotNull OnlineUser onlineUser, @NotNull String[] args) {
        CompletableFuture.runAsync(() -> {
            if (args.length != 1) {
                plugin.getLocales().getLocale("error_invalid_syntax", "/tphere <player>")
                        .ifPresent(onlineUser::sendMessage);
                return;
            }
            final String targetPlayerName = args[0];
            plugin.findPlayer(onlineUser, targetPlayerName).thenAccept(teleporterName -> {
                if (teleporterName.isEmpty()) {
                    plugin.getLocales().getLocale("error_player_not_found", targetPlayerName)
                            .ifPresent(onlineUser::sendMessage);
                    return;
                }

                Teleport.builder(plugin, onlineUser)
                        .setTeleporter(teleporterName.get())
                        .setTarget(onlineUser.getPosition())
                        .toTeleport()
                        .thenAccept(teleport -> teleport.execute().thenAccept(result -> {
                            if (result.successful()) {
                                result.getTeleporter()
                                        .flatMap(teleporter -> plugin.getLocales().getLocale("teleporting_other_complete",
                                                teleporter.username, onlineUser.username))
                                        .ifPresent(onlineUser::sendMessage);

                                plugin.getDatabase().setLastPosition(onlineUser, onlineUser.getPosition());
                            }
                        }));
            });

        });
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull String[] args, @Nullable OnlineUser user) {

        List<String> cache = RedisTabAPI.getInstance().getTotalPlayers();

        if(user== null || !user.hasPermission("redistab.vanish.see")) {
            cache.removeAll(RedisTabAPI.getInstance().getVanishedPlayers());
        }

        return args.length <= 1 ? cache.stream()
                .filter(s -> s.toLowerCase().startsWith(args.length == 1 ? args[0].toLowerCase() : ""))
                .sorted().collect(Collectors.toList()) : Collections.emptyList();
    }
}
