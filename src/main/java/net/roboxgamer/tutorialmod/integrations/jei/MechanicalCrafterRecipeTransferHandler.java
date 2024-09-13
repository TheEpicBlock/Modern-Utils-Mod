package net.roboxgamer.tutorialmod.integrations.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.roboxgamer.tutorialmod.menu.CraftingGhostSlotItemHandler;
import net.roboxgamer.tutorialmod.menu.MechanicalCrafterMenu;
import net.roboxgamer.tutorialmod.menu.ModMenuTypes;
import net.roboxgamer.tutorialmod.network.GhostSlotTransferPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    List<Ingredient> ingredients = recipe.value().getIngredients();
    
    if (ingredients.size() > 9) {
      return handlerHelper.createUserErrorWithTooltip(Component.literal("The recipe is too large for the available crafting slots."));
    }
    
    if (!doTransfer) {
      return null;  // No validation needed when not transferring
    } else {
      return transferRecipeToGhostSlots(container, ingredients);
    }
  }
  
  private @Nullable IRecipeTransferError transferRecipeToGhostSlots(@NotNull MechanicalCrafterMenu container,
                                                                    @NotNull List<Ingredient> ingredients) {
    int[] slotMap = getSlotMap(ingredients);
    clearGhostSlots(container);
    
    for (int i = 0; i < ingredients.size(); i++) {
      Ingredient ingredient = ingredients.get(i);
      ItemStack ghostItem = new ItemStack(ingredient.getItems()[0].getItem());  // Create a ghost item from the first item in the ingredient
      
      Slot ghostSlot = container.getSlot(slotMap[i]);
      if (ghostSlot instanceof CraftingGhostSlotItemHandler) {
        ghostItem.setCount(1);
        ghostSlot.set(ghostItem);
        
        PacketDistributor.sendToServer(
            new GhostSlotTransferPayload(slotMap[i], ghostItem, container.getBlockEntity().getBlockPos())
        );
      }
    }
    
    return null;  // No error, transfer successful
  }
  
  private static int @NotNull [] getSlotMap(List<Ingredient> ingredients) {
    if (ingredients.size() == 4) {
      return new int[] {1, 2, 4, 5};  // 2x2 recipe
    }
    return new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9};  // 3x3 recipe
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