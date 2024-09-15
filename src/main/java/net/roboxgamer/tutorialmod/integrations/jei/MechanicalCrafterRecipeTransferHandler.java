package net.roboxgamer.tutorialmod.integrations.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.roboxgamer.tutorialmod.menu.CraftingGhostSlotItemHandler;
import net.roboxgamer.tutorialmod.menu.MechanicalCrafterMenu;
import net.roboxgamer.tutorialmod.menu.ModMenuTypes;
import net.roboxgamer.tutorialmod.network.GhostSlotTransferPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MechanicalCrafterRecipeTransferHandler implements IRecipeTransferHandler<MechanicalCrafterMenu, RecipeHolder<CraftingRecipe>> {
  private final IRecipeTransferHandlerHelper handlerHelper;
  
  public MechanicalCrafterRecipeTransferHandler(IRecipeTransferHandlerHelper handlerHelper) {
    this.handlerHelper = handlerHelper;
  }
  
  @Override
  public @NotNull Class<? extends MechanicalCrafterMenu> getContainerClass() {
    return MechanicalCrafterMenu.class;
  }
  
  @Override
  public @NotNull Optional<MenuType<MechanicalCrafterMenu>> getMenuType() {
    return Optional.of(ModMenuTypes.MECHANICAL_CRAFTER_MENU.get());
  }
  
  @Override
  public @NotNull RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
    return RecipeTypes.CRAFTING;
  }
  
  @Override
  public @Nullable IRecipeTransferError transferRecipe(@NotNull MechanicalCrafterMenu container,
                                                       @NotNull RecipeHolder<CraftingRecipe> recipe,
                                                       @NotNull IRecipeSlotsView recipeSlots,
                                                       @NotNull Player player,
                                                       boolean maxTransfer,
                                                       boolean doTransfer) {
    CraftingRecipe craftingRecipe = recipe.value();
    
    NonNullList<Ingredient> ingredients = craftingRecipe.getIngredients();
    
    if (ingredients.size() > 9) {
      return handlerHelper.createUserErrorWithTooltip(
          Component.literal("The recipe is too large for the available crafting slots."));
    }
    
    if (doTransfer) {
      int[] slotMap;
      if (craftingRecipe instanceof ShapedRecipe shapedRecipe) {
        slotMap = getSlotMap((shapedRecipe.getWidth()),(shapedRecipe.getHeight()));
      }
      else {
        slotMap = getSlotMap();
      }
      clearGhostSlots(container);  // Clear existing ghost items before transfer
      
      for (int i = 0; i < ingredients.size(); i++) {
        Ingredient ingredient = ingredients.get(i);
        ItemStack[] ingredientItems = ingredient.getItems();
        if (ingredientItems.length > 0) {
          ItemStack matchingStack = ingredientItems[0].copy();
          matchingStack.setCount(1);  // Set count to 1 for ghost slots
          Slot ghostSlot = container.getSlot(slotMap[i]);
          if (ghostSlot instanceof CraftingGhostSlotItemHandler) {
            ghostSlot.set(matchingStack);  // Place the item in the ghost slot
            // Send packet to server to synchronize the ghost slot
            PacketDistributor.sendToServer(
                new GhostSlotTransferPayload(slotMap[i], matchingStack, container.getBlockEntity().getBlockPos())
            );
          }
        }
      }
    }
    
    return null;  // Success, no error
  }
  
  private static int @NotNull [] getSlotMap() {
    return getSlotMap(0,0); // Shapeless recipe
  }
  
  private static int @NotNull [] getSlotMap(int width,int height) {
    if (width == 0 && height == 0) {
      return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    }
    if (width == 2 && height == 2) {
      return new int[]{1, 2, 4, 5};
    } else if (width == 3 && height == 3) {
      return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    } else if (width == 1) {
      return new int[]{2,5,8};
    }
    else if (height == 1) {
      return new int[]{4,5,6};
    }
    return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};  // Default to 3x3 recipe
  }
  
  private void clearGhostSlots(MechanicalCrafterMenu container) {
    for (int i = 1; i <= 9; i++) {
      Slot ghostSlot = container.getSlot(i);
      if (ghostSlot instanceof CraftingGhostSlotItemHandler) {
        ghostSlot.set(ItemStack.EMPTY);
        PacketDistributor.sendToServer(
            new GhostSlotTransferPayload(i, ItemStack.EMPTY, container.getBlockEntity().getBlockPos())
        );
      }
    }
  }
}