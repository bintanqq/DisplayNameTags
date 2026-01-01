package com.mattmx.nametags;

import com.mattmx.nametags.entity.NameTagEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class NameTagsCommand implements CommandExecutor, TabCompleter {
    private final @NotNull NameTags plugin;

    public NameTagsCommand(@NotNull NameTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("nametags.command.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                reload();
                sender.sendMessage(Component.text("Reloaded!").color(NamedTextColor.GREEN));
            }
            case "debug" -> {
                if (!sender.hasPermission("nametags.command.debug")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                sendDebugInfo(sender);
            }
            case "hide" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                if (!sender.hasPermission("nametags.command.hide")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                hideNameTag(player);
            }
            case "show" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                if (!sender.hasPermission("nametags.command.show")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!")
                        .color(NamedTextColor.RED));
                    return true;
                }
                showNameTag(player);
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(
            Component.text("=== NameTags Commands ===").color(NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("/nametags reload").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Reload the configuration").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/nametags debug").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Show debug information").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/nametags hide").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Hide your nametag").color(NamedTextColor.GRAY))
                .appendNewline()
                .append(Component.text("/nametags show").color(NamedTextColor.YELLOW))
                .append(Component.text(" - Show your nametag").color(NamedTextColor.GRAY))
        );
    }

    private void hideNameTag(@NotNull Player player) {
        if (plugin.getVisibilityManager().isHidden(player)) {
            player.sendMessage(Component.text("Your nametag is already hidden!")
                .color(NamedTextColor.YELLOW));
            return;
        }

        plugin.getVisibilityManager().setHidden(player, true);

        NameTagEntity entity = plugin.getEntityManager().getNameTagEntity(player);
        if (entity != null) {
            // Remove all viewers
            for (UUID viewerId : entity.getPassenger().getViewers().toArray(new UUID[0])) {
                entity.getPassenger().removeViewer(viewerId);
            }
        }

        player.sendMessage(Component.text("Your nametag has been hidden!")
            .color(NamedTextColor.GREEN));
    }

    private void showNameTag(@NotNull Player player) {
        if (!plugin.getVisibilityManager().isHidden(player)) {
            player.sendMessage(Component.text("Your nametag is already visible!")
                .color(NamedTextColor.YELLOW));
            return;
        }

        plugin.getVisibilityManager().setHidden(player, false);

        // Run async to avoid blocking the main thread
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            NameTagEntity entity = plugin.getEntityManager().getNameTagEntity(player);

            if (entity == null) {
                // Create the entity if it doesn't exist
                entity = plugin.getEntityManager().getOrCreateNameTagEntity(player);
            }

            // Add all online players as viewers
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player) || plugin.getConfig().getBoolean("show-self", false)) {
                    entity.getPassenger().addViewer(onlinePlayer.getUniqueId());
                    entity.sendPassengerPacket(onlinePlayer);
                }
            }

            // Update visibility to handle any edge cases
            entity.updateVisibility();

            // Send success message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(Component.text("Your nametag is now visible!")
                    .color(NamedTextColor.GREEN));
            });
        });
    }

    private void sendDebugInfo(@NotNull CommandSender sender) {
        sender.sendMessage(
            Component.text("NameTags debug")
                .appendNewline()
                .append(
                    Component.text("Total NameTags: " + plugin.getEntityManager().getCacheSize())
                        .hoverEvent(HoverEvent.showText(
                            Component.text("By Entity UUID: " + plugin.getEntityManager().getCacheSize())
                                .appendNewline()
                                .append(Component.text("By Entity ID: " + plugin.getEntityManager().getEntityIdMapSize()))
                                .appendNewline()
                                .append(Component.text("By Passenger ID: " + plugin.getEntityManager().getPassengerIdMapSize()))
                        ))
                        .color(NamedTextColor.WHITE)
                )
                .appendNewline()
                .append(
                    Component.text("Cached last sent passengers: " + plugin.getEntityManager().getLastSentPassengersSize())
                        .color(NamedTextColor.WHITE)
                )
                .appendNewline()
                .append(
                    Component.text("Viewers:")
                        .appendNewline()
                        .append(
                            Component.text(
                                String.join("\n",
                                    plugin.getEntityManager()
                                        .getAllEntities()
                                        .stream()
                                        .map((nameTag) -> " - " + nameTag.getBukkitEntity().getUniqueId() + ": " + nameTag.getPassenger().getViewers())
                                        .toList()
                                )
                            )
                        )
                )
                .color(NamedTextColor.GOLD)
        );
    }

    private void reload() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final NameTagEntity tag = plugin.getEntityManager().getNameTagEntity(player);

            if (tag != null) {
                tag.getTraits().destroy();
            }
        }

        this.plugin.reloadConfig();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            final NameTagEntity tag = plugin.getEntityManager().removeEntity(player);

            if (tag != null) {
                tag.destroy();
            }

            // Skip creating nametag if player has it hidden
            if (plugin.getVisibilityManager().isHidden(player)) {
                continue;
            }

            final NameTagEntity newTag = plugin.getEntityManager().getOrCreateNameTagEntity(player);

            // Add existing viewers
            if (tag != null) {
                for (final UUID viewer : tag.getPassenger().getViewers()) {
                    newTag.getPassenger().addViewer(viewer);

                    // Send passenger packet
                    Player playerViewer = Bukkit.getPlayer(viewer);
                    if (playerViewer != null) {
                        newTag.sendPassengerPacket(playerViewer);
                    }
                }
            }

            newTag.updateVisibility();
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String lastArg = args.length >= 1 ? args[0].toLowerCase() : "";
        return Stream.of("reload", "debug", "hide", "show")
            .filter((arg) -> sender.hasPermission("nametags.command." + arg))
            .filter((arg) -> arg.toLowerCase().startsWith(lastArg))
            .toList();
    }
}