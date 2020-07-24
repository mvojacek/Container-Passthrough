package com.darkender.plugins.containerpassthrough;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import java.util.HashSet;
import java.util.Set;

public class ContainerPassthrough extends JavaPlugin implements Listener
{
    private Set<EntityType> passthroughEntities;
    private Set<Material> passthroughBlocks;
    private boolean ignoreInteractEvents = false;
    
    @Override
    public void onEnable()
    {
        passthroughEntities = new HashSet<>();
        passthroughEntities.add(EntityType.PAINTING);
        passthroughEntities.add(EntityType.ITEM_FRAME);
        
        passthroughBlocks = new HashSet<>();
        for(Material material : Material.values())
        {
            if(material.name().contains("SIGN"))
            {
                passthroughBlocks.add(material);
            }
        }
        
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    private boolean canOpenContainer(Player player, Block block, BlockFace face)
    {
        // Send out an event to ensure container locking plugins aren't bypassed
        ignoreInteractEvents = true;
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK,
                player.getInventory().getItemInMainHand(), block, face, EquipmentSlot.HAND);
        getServer().getPluginManager().callEvent(interactEvent);
        ignoreInteractEvents = false;
        return !(interactEvent.useInteractedBlock() == Event.Result.DENY || interactEvent.isCancelled());
    }
    
    private boolean tryOpeningContainer(Player player)
    {
        RayTraceResult result = player.rayTraceBlocks(5.0, FluidCollisionMode.NEVER);
        if(result == null || result.getHitBlock() == null || !(result.getHitBlock().getState() instanceof Container))
        {
            return false;
        }
        Container container = (Container) result.getHitBlock().getState();
        
        if(!canOpenContainer(player, result.getHitBlock(), result.getHitBlockFace()))
        {
            return false;
        }
        
        if(!player.getOpenInventory().getTopInventory().equals(container.getInventory()))
        {
            player.openInventory(container.getInventory());
        }
        return true;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if(event.getPlayer().isSneaking() || !passthroughEntities.contains(event.getRightClicked().getType()))
        {
            return;
        }
        
        if(tryOpeningContainer(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEvent event)
    {
        if(ignoreInteractEvents || event.getPlayer().isSneaking() ||
                event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getClickedBlock() == null ||
                !passthroughBlocks.contains(event.getClickedBlock().getType()))
        {
            return;
        }
    
        Block blockBehind = event.getClickedBlock().getRelative(event.getBlockFace().getOppositeFace());
        if(!(blockBehind.getState() instanceof Container) || !canOpenContainer(event.getPlayer(), blockBehind, event.getBlockFace()))
        {
            return;
        }
        Container container = (Container) blockBehind.getState();
        if(!event.getPlayer().getOpenInventory().getTopInventory().equals(container.getInventory()))
        {
            event.getPlayer().openInventory(container.getInventory());
        }
        event.setCancelled(true);
    }
}
