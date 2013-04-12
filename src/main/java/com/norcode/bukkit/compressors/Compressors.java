package com.norcode.bukkit.compressors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public class Compressors extends JavaPlugin implements Listener {

    private HashMap<String, CompressorRecipe> recipeMap = new HashMap<String, CompressorRecipe>();
    private boolean worldWhitelist = true; // blacklist if false
    private HashSet<String> worldList = new HashSet<String>();
    private Permission wildcardPermission;
    @Override
    public void onEnable() {
        wildcardPermission = new Permission("compressors.create.*", PermissionDefault.OP);
        getServer().getPluginManager().addPermission(wildcardPermission);
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getServer().getPluginManager().removePermission(wildcardPermission);
    }

    public ItemStack parseItemStack(String s) {
        String[] parts = s.split(":");
        Material mat = Material.valueOf(parts[0].toUpperCase());
        short data = 0;
        int qty = 1;
        if (parts.length == 3) {
            data = Short.parseShort(parts[1]);
            qty = Integer.parseInt(parts[2]);
        } else {
            qty = Integer.parseInt(parts[1]);
        }
        return new ItemStack(mat, qty, data);
    }

    public boolean enabledInWorld(World w) {
        boolean enabled = ((worldWhitelist && worldList.contains(w.getName().toLowerCase())) || 
                (!worldWhitelist && !worldList.contains(w.getName().toLowerCase())));
        return enabled;
    }

    public void loadConfig() {
        String listtype = getConfig().getString("world-selection", "whitelist").toLowerCase();
        wildcardPermission.getChildren().clear();
        wildcardPermission.recalculatePermissibles();
        if (listtype.equals("blacklist")) {
            this.worldWhitelist = false;
        } else {
            this.worldWhitelist = true;
        }
        this.worldList.clear();
        for (String wn: getConfig().getStringList("world-list")) {
            this.worldList.add(wn.toLowerCase());
        }
        ConfigurationSection cfg = getConfig().getConfigurationSection("recipes");
        recipeMap.clear();
        Permission perm;
        for (String recipeName: cfg.getKeys(false)) {
            perm = new Permission("compressors.create." + recipeName.toLowerCase(), PermissionDefault.TRUE);
            perm.addParent(wildcardPermission, true);
            List<ItemStack> steps = new ArrayList<ItemStack>();
            for (String s: cfg.getStringList(recipeName)) {
                steps.add(parseItemStack(s));
            }
            ItemStack result = steps.remove(0);
            Collections.reverse(steps);
            recipeMap.put(recipeName.toLowerCase(), new CompressorRecipe(result, steps.toArray(new ItemStack[0])));
        }
        wildcardPermission.recalculatePermissibles();
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
            if (enabledInWorld(hopper.getWorld())) {
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
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Hopper) {
            Hopper hopper = (Hopper) event.getSource().getHolder();
            if (enabledInWorld(hopper.getWorld())) {
                final Compressor comp = getCompressor(hopper);
                if (comp != null) {
                    if (!event.getItem().isSimilar(comp.getRecipe().getResult())) {
                        if (comp.isSourceMaterial(event.getItem())) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

        if (event.getDestination().getHolder() instanceof Hopper && !event.isCancelled()) {
            Hopper hopper = (Hopper) event.getDestination().getHolder();
            if (enabledInWorld(hopper.getWorld())) {
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
    }

    @EventHandler
    public void onSignCreated(SignChangeEvent event) {
        if (event.getLine(1) != null && event.getLine(2) != null) {
            String type = event.getLine(1).toLowerCase(); 
            if (event.getLine(0).toLowerCase().equals("[compressor]")) {
                if (getRecipe(type) != null && event.getPlayer().hasPermission("compressors.create." + type)) {
                    event.setLine(0, ChatColor.DARK_BLUE + "[COMPRESSOR]");
                }
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
                if (sign.getLine(0).toLowerCase().equals(ChatColor.DARK_BLUE + "[compressor]")) {
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
