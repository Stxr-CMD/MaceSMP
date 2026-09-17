package com.macesmp.plugin.listeners;

import com.macesmp.plugin.MaceSMP;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Neutralizes the Slow Falling effect entirely - from splash/lingering potions,
 * tipped arrows, beacons, anything. Players can still craft the items, but the
 * effect itself is cancelled the moment it would apply, so it never does anything.
 *
 * This exists as a PvP-balance choice (so nobody can cheese fall-damage-based
 * combat, e.g. around the Mace) - toggle it off in config.yml under "pvp.disable-slow-falling"
 * if you ever want to allow it again.
 */
public class SlowFallingBlocker implements Listener {

    private final MaceSMP plugin;

    public SlowFallingBlocker(MaceSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!plugin.getConfig().getBoolean("pvp.disable-slow-falling", true)) {
            return;
        }
        if (event.getNewEffect() == null) {
            return; // effect being removed, not applied - nothing to block
        }
        if (event.getNewEffect().getType().equals(PotionEffectType.SLOW_FALLING)) {
            event.setCancelled(true);
            // Also strip it immediately in case it briefly landed from another plugin/order-of-events
            if (event.getEntity() instanceof LivingEntity entity && entity.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                entity.removePotionEffect(PotionEffectType.SLOW_FALLING);
            }
        }
    }
}
