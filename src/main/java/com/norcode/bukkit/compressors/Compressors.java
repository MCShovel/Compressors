package com.norcode.bukkit.compressors;

import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Compressors extends JavaPlugin implements Listener {

    private HashMap<String, CompressorRecipe> recipeMap = new HashMap<String, CompressorRecipe>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        recipeMap.put("gold_block", new CompressorRecipe(new ItemStack(Material.GOLD_BLOCK, 1), new ItemStack(Material.GOLD_NUGGET, 9), new ItemStack(Material.GOLD_INGOT, 9)));
        recipeMap.put("gold_ingot", new CompressorRecipe(new ItemStack(Material.GOLD_INGOT, 1), new ItemStack(Material.GOLD_NUGGET, 9)));
        recipeMap.put("iron_block", new CompressorRecipe(new ItemStack(Material.IRON_BLOCK, 1), new ItemStack(Material.IRON_INGOT, 9)));
        recipeMap.put("diamond_block", new CompressorRecipe(new ItemStack(Material.DIAMOND_BLOCK, 1), new ItemStack(Material.DIAMOND, 9)));
        recipeMap.put("emerald_block", new CompressorRecipe(new ItemStack(Material.EMERALD_BLOCK, 1), new ItemStack(Material.EMERALD, 9)));
        recipeMap.put("lapis_block", new CompressorRecipe(new ItemStack(Material.LAPIS_BLOCK, 1), new ItemStack(Material.SNOW_BALL, 4)));
        recipeMap.put("redstone_block", new CompressorRecipe(new ItemStack(Material.REDSTONE_BLOCK, 1), new ItemStack(Material.REDSTONE, 9)));
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getInventory().getHolder();
            final Compressor comp = getCompressor(hopper);
            if (comp != null) {
                getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                    public void run() {
                        comp.compress();
                    }
                }, 0);
            }
        }
    }
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getInventory().getHolder();
            final Compressor comp = getCompressor(hopper);
            if (comp != null) {
                getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                    public void run() {
                        comp.compress();
                    }
                }, 0);
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getSource().getHolder();
            final Compressor comp = getCompressor(hopper);
            if (comp != null) {
                if (!event.getItem().isSimilar(comp.getRecipe().getResult())) {
                    if (comp.isSourceMaterial(event.getItem())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
        if (event.getDestination().getHolder() instanceof Hopper && !event.isCancelled()) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();
            final Compressor comp = getCompressor(hopper);
            if (comp != null) {
                getServer().getScheduler().runTaskLater(this, new Runnable() {
                    public void run() {
                        comp.compress();
                    }
                }, 0);
            }
        }
    }
    private static BlockFace[] sides = new BlockFace[] {
        BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH
    };

    public Compressor getCompressor(Hopper hopper) {
        for (BlockFace side: sides) {
            if (hopper.getBlock().getRelative(side).getType().equals(Material.WALL_SIGN)) {
                Sign sign = (Sign) hopper.getBlock().getRelative(side).getState();
                if (sign.getLine(0).toLowerCase().equals("[compressor]")) {
                    String recipe = sign.getLine(1);
                    if (recipe != null) {
                        return new Compressor(this, getRecipe(recipe), hopper);
                    }
                }
            }
        }
        return null;
    }

    private CompressorRecipe getRecipe(String recipe) {
        return recipeMap.get(recipe.toLowerCase());
    }
}
