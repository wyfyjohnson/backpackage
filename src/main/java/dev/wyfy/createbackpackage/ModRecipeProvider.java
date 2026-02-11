package dev.wyfy.createbackpackage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(
        PackOutput output,
        CompletableFuture<HolderLookup.Provider> registries
    ) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        backpackRecipe(output);
        magnetUpgradeRecipe(output);
    }

    private void backpackRecipe(RecipeOutput output) {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(
            Map.of(
                'S',
                Ingredient.of(Items.STRING),
                'C',
                Ingredient.of(itemFromId("create", "clipboard")),
                'D',
                Ingredient.of(itemFromId("create", "cardboard")),
                'B',
                Ingredient.of(Items.BARREL),
                'G',
                Ingredient.of(itemFromId("create", "super_glue"))
            ),
            "SCS",
            "DBD",
            "SGS"
        );

        DamagingShapedRecipe recipe = new DamagingShapedRecipe(
            "",
            CraftingBookCategory.MISC,
            pattern,
            new ItemStack(CreateBackpackage.CARDBOARD_BACKPACK.get())
        );

        Advancement.Builder advancement = Advancement.Builder.advancement()
            .addCriterion(
                "has_cardboard",
                InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item()
                        .of(itemFromId("create", "cardboard"))
                        .build()
                )
            )
            .rewards(AdvancementRewards.EMPTY)
            .requirements(AdvancementRequirements.Strategy.OR);

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            CreateBackpackage.MODID,
            "cardboard_backpack"
        );

        output.accept(
            id,
            recipe,
            advancement.build(
                id.withPrefix(
                    "recipes/" + RecipeCategory.MISC.getFolderName() + "/"
                )
            )
        );
    }

    private void magnetUpgradeRecipe(RecipeOutput output) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(itemFromId("create", "copper_casing")));
        ingredients.add(Ingredient.of(Items.IRON_NUGGET));
        ingredients.add(Ingredient.of(Items.IRON_NUGGET));
        ingredients.add(Ingredient.of(Items.IRON_NUGGET));
        ingredients.add(Ingredient.of(Items.IRON_NUGGET));
        ingredients.add(Ingredient.of(itemFromId("create", "electron_tube")));
        ingredients.add(Ingredient.of(itemFromId("create", "super_glue")));

        DamagingShapelessRecipe recipe = new DamagingShapelessRecipe(
            "",
            CraftingBookCategory.MISC,
            new ItemStack(CreateBackpackage.MAGNET_UPGRADE.get()),
            ingredients
        );

        Advancement.Builder advancement = Advancement.Builder.advancement()
            .addCriterion(
                "has_copper_casing",
                InventoryChangeTrigger.TriggerInstance.hasItems(
                    ItemPredicate.Builder.item()
                        .of(itemFromId("create", "copper_casing"))
                        .build()
                )
            )
            .rewards(AdvancementRewards.EMPTY)
            .requirements(AdvancementRequirements.Strategy.OR);

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            CreateBackpackage.MODID,
            "magnet_upgrade"
        );

        output.accept(
            id,
            recipe,
            advancement.build(
                id.withPrefix(
                    "recipes/" + RecipeCategory.MISC.getFolderName() + "/"
                )
            )
        );
    }

    private static net.minecraft.world.level.ItemLike itemFromId(
        String namespace,
        String path
    ) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath(namespace, path)
        );
    }
}
