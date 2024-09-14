package net.roboxgamer.tutorialmod.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.TippedArrowRecipe;
import org.jetbrains.annotations.NotNull;

public class CustomTippedArrowRecipe extends TippedArrowRecipe {
  private NonNullList<Ingredient> ingredients;
  
  public CustomTippedArrowRecipe(CraftingBookCategory category) {
    super(category);
  }
  
  public CustomTippedArrowRecipe(TippedArrowRecipe recipe) {
    this(recipe.category());
  }
  
  public void setIngredients(NonNullList<Ingredient> ingredients) {
    this.ingredients = ingredients;
  }
  
  @Override
  public @NotNull NonNullList<Ingredient> getIngredients() {
    return this.ingredients;
  }
}