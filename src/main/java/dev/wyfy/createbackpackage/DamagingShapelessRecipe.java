package dev.wyfy.createbackpackage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class DamagingShapelessRecipe extends ShapelessRecipe {

    public DamagingShapelessRecipe(
        String group,
        CraftingBookCategory category,
        ItemStack result,
        NonNullList<Ingredient> ingredients
    ) {
        super(group, category, result, ingredients);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(
            input.size(),
            ItemStack.EMPTY
        );

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.isDamageableItem()) {
                ItemStack damaged = stack.copy();
                damaged.setDamageValue(damaged.getDamageValue() + 1);

                if (damaged.getDamageValue() < damaged.getMaxDamage()) {
                    remaining.set(i, damaged);
                }
                // else: item broke, leave EMPTY
            } else if (stack.getItem().hasCraftingRemainingItem(stack)) {
                remaining.set(
                    i,
                    stack.getItem().getCraftingRemainingItem(stack)
                );
            }
        }

        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CreateBackpackage.DAMAGING_SHAPELESS_SERIALIZER.get();
    }

    public static class Serializer
        implements RecipeSerializer<DamagingShapelessRecipe>
    {

        private static final MapCodec<DamagingShapelessRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                instance
                    .group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(
                            ShapelessRecipe::getGroup
                        ),
                        CraftingBookCategory.CODEC.fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(ShapelessRecipe::category),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r ->
                            r.getResultItem(null)
                        ),
                        Ingredient.CODEC_NONEMPTY.listOf()
                            .fieldOf("ingredients")
                            .flatXmap(
                                list -> {
                                    NonNullList<Ingredient> ingredients =
                                        NonNullList.create();
                                    ingredients.addAll(list);
                                    if (ingredients.isEmpty()) {
                                        return com.mojang.serialization.DataResult.error(
                                            () ->
                                                "No ingredients for shapeless recipe"
                                        );
                                    }
                                    if (ingredients.size() > 9) {
                                        return com.mojang.serialization.DataResult.error(
                                            () ->
                                                "Too many ingredients for shapeless recipe"
                                        );
                                    }
                                    return com.mojang.serialization.DataResult.success(
                                        ingredients
                                    );
                                },
                                com.mojang.serialization.DataResult::success
                            )
                            .forGetter(ShapelessRecipe::getIngredients)
                    )
                    .apply(instance, DamagingShapelessRecipe::new)
            );

        private static final StreamCodec<
            RegistryFriendlyByteBuf,
            DamagingShapelessRecipe
        > STREAM_CODEC = StreamCodec.of(
            Serializer::toNetwork,
            Serializer::fromNetwork
        );

        private static DamagingShapelessRecipe fromNetwork(
            RegistryFriendlyByteBuf buf
        ) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(
                CraftingBookCategory.class
            );
            int ingredientCount = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(
                ingredientCount,
                Ingredient.EMPTY
            );
            ingredients.replaceAll(ignored ->
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
            );
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            return new DamagingShapelessRecipe(
                group,
                category,
                result,
                ingredients
            );
        }

        private static void toNetwork(
            RegistryFriendlyByteBuf buf,
            DamagingShapelessRecipe recipe
        ) {
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            buf.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buf, recipe.getResultItem(null));
        }

        @Override
        public MapCodec<DamagingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
            RegistryFriendlyByteBuf,
            DamagingShapelessRecipe
        > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
