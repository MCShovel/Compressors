package com.norcode.bukkit.compressors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Compressor {
    private Comparator<ItemStack> compressorComparator = new Comparator<ItemStack>() {
        public int compare(ItemStack o1, ItemStack o2) {
            int v1 = 0;
            int v2 = 0;
            if (o1 != null) {
                if (o1.isSimilar(recipe.getResult())) {
                    v1 = -1;
                } else if (isSourceMaterial(o1)) {
                    v1 = 1;
                }
            }
            if (o2 != null) {
                if (o2.isSimilar(recipe.getResult())) {
                    v2 = -1;
                } else if (isSourceMaterial(o2)) {
                    v2 = 1;
                }
            }
            return v1 - v2;
        }
    };
    
    CompressorRecipe recipe;
    Compressors plugin;
    Hopper hopper;
    public CompressorRecipe getRecipe() {
        return recipe;
    }
    public Compressor(Compressors plugin, CompressorRecipe recipe, Hopper hopper) {
        this.recipe = recipe;
        this.plugin = plugin;
        this.hopper = hopper;
    }
    
    public boolean isSourceMaterial(ItemStack stack) {
        return this.recipe.isSourceMaterial(stack);
    }

    public void compress() {
        Inventory inv = this.hopper.getInventory();
        for (int i=0;i<recipe.getSteps().size();i++) {
            ItemStack src = recipe.getSteps().get(i).clone();
            int qty = src.getAmount();
            HashMap<Integer, ItemStack> didntHave = inv.removeItem(src);
            if (didntHave.isEmpty()) {
                if (i == recipe.getSteps().size()-1) {
                    inv.addItem(recipe.getResult().clone());
                } else {
                    ItemStack s = recipe.getSteps().get(i+1).clone();
                    s.setAmount(1);
                    inv.addItem(s);
                }
            } else {
                if (didntHave.get(0).getAmount() < qty) {
                    didntHave.get(0).setAmount(qty-didntHave.get(0).getAmount());
                    inv.addItem(didntHave.get(0));
                }
            }
        }
        this.sort();
    }

    public void sort() {
        Inventory inv = this.hopper.getInventory();
        ItemStack s = null;
        ItemStack[] newContents = new ItemStack[inv.getSize()];
        for (int i=0; i<inv.getSize(); i++) {
            newContents[i] = inv.getItem(i);
        }
        Arrays.sort(newContents, compressorComparator);
        inv.setContents(newContents);
    }
}
