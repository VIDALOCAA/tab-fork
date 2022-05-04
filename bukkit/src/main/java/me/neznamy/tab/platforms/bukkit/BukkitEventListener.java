package me.neznamy.tab.platforms.bukkit;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.protocol.PacketPlayOutPlayerInfo;
import me.neznamy.tab.shared.TAB;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The core for bukkit forwarding events into all enabled features
 */
public class BukkitEventListener implements Listener {

    private final BukkitPlatform platform;
    
    public BukkitEventListener(BukkitPlatform platform) {
        this.platform = platform;
    }
    
    /**
     * Listener to PlayerQuitEvent to remove player data and forward the event to features
     * @param e quit event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e){
        if (TAB.getInstance().isDisabled()) return;
        TAB.getInstance().getCPUManager().runTask(() -> TAB.getInstance().getFeatureManager().onQuit(TAB.getInstance().getPlayer(e.getPlayer().getUniqueId())));
    }
    
    /**
     * Listener to PlayerJoinEvent to create player data and forward the event to features
     * @param e join event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        if (TAB.getInstance().isDisabled()) return;
        TAB.getInstance().getCPUManager().runTask(() -> TAB.getInstance().getFeatureManager().onJoin(new BukkitTabPlayer(e.getPlayer(), platform.getProtocolVersion(e.getPlayer()))));

        hidePlayers(e.getPlayer());
    }

    /**
     * Listener to PlayerChangedWorldEvent to forward the event to features
     * @param e world changed event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent e){
        if (TAB.getInstance().isDisabled()) return;
        TAB.getInstance().getCPUManager().runTask(() -> TAB.getInstance().getFeatureManager().onWorldChange(e.getPlayer().getUniqueId(), e.getPlayer().getWorld().getName()));

        hidePlayers(e.getPlayer());
    }

    private void hidePlayers(Player player) {
        TAB.getInstance().getCPUManager().runTaskLater(1000, () -> {
            for (TabPlayer all : TAB.getInstance().getOnlinePlayers()) {
                all.sendCustomPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, new PacketPlayOutPlayerInfo.PlayerInfoData(player.getUniqueId())));
            }

            List<PacketPlayOutPlayerInfo.PlayerInfoData> playerInfoDataArrayList = Arrays.stream(TAB.getInstance().getOnlinePlayers()).map(tabPlayer -> new PacketPlayOutPlayerInfo.PlayerInfoData(tabPlayer.getUniqueId())).collect(Collectors.toList());
            new BukkitTabPlayer(player, platform.getProtocolVersion(player))
                    .sendCustomPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, playerInfoDataArrayList));
        });
    }

    /**
     * Listener to PlayerChangedWorldEvent to forward the event to features
     * @param e command preprocess event
     */
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (TAB.getInstance().isDisabled()) return;
        if (TAB.getInstance().getFeatureManager().onCommand(TAB.getInstance().getPlayer(e.getPlayer().getUniqueId()), e.getMessage())) e.setCancelled(true);
    }
}
