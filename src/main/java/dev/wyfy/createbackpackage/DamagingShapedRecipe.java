package dev.wyfy.createbackpackage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class DamagingShapedRecipe extends ShapedRecipe {

    public DamagingShapedRecipe(
        String group,
        CraftingBookCategory category,
        ShapedRecipePattern pattern,
        ItemStack result,
        boolean showNotification
    ) {
        super(group, category, pattern, result, showNotification);
    }

    public DamagingShapedRecipe(
        String group,
        CraftingBookCategory category,
        ShapedRecipePattern pattern,
        ItemStack result
    ) {
        super(group, category, pattern, result);
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
        return CreateBackpackage.DAMAGING_SHAPED_SERIALIZER.get();
    }

    public static class Serializer
        implements RecipeSerializer<DamagingShapedRecipe>
    {

        private static final MapCodec<DamagingShapedRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                instance
                    .group(
                        com.mojang.serialization.Codec.STRING
                            .optionalFieldOf("group", "")
                            .forGetter(ShapedRecipe::getGroup),
                        CraftingBookCategory.CODEC.fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(ShapedRecipe::category),
                        ShapedRecipePattern.MAP_CODEC.forGetter(
                            r -> r.pattern
                        ),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r ->
                            r.getResultItem(null)
                        ),
                        com.mojang.serialization.Codec.BOOL
                            .optionalFieldOf("show_notification", true)
                            .forGetter(ShapedRecipe::showNotification)
                    )
                    .apply(instance, DamagingShapedRecipe::new)
            );

        private static final StreamCodec<
            RegistryFriendlyByteBuf,
            DamagingShapedRecipe
        > STREAM_CODEC = StreamCodec.of(
            Serializer::toNetwork,
            Serializer::fromNetwork
        );

        private static DamagingShapedRecipe fromNetwork(
            RegistryFriendlyByteBuf buf
        ) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(
                CraftingBookCategory.class
            );
            ShapedRecipePattern pattern =
                ShapedRecipePattern.STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            boolean showNotification = buf.readBoolean();
            return new DamagingShapedRecipe(
                group,
                category,
                pattern,
                result,
                showNotification
            );
        }

        private static void toNetwork(
            RegistryFriendlyByteBuf buf,
            DamagingShapedRecipe recipe
        ) {
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buf, recipe.getResultItem(null));
            buf.writeBoolean(recipe.showNotification());
        }

        @Override
        public MapCodec<DamagingShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<
            RegistryFriendlyByteBuf,
            DamagingShapedRecipe
        > streamCodec() {
            return STREAM_CODEC;
        }
    }
}
