package io.wynnchanger.client.model;

import io.wynnchanger.client.SkinModelMapping;
import io.wynnchanger.client.SkinPropertyType;
import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.entity.EquipmentSlot;

import java.util.List;
import java.util.Optional;

public final class SkinModelOverride {
    private static final ThreadLocal<Boolean> SUPPRESS_OVERRIDE = ThreadLocal.withInitial(() -> false);

    private SkinModelOverride() {
    }

    public static void withOverridesSuppressed(Runnable action) {
        boolean previous = SUPPRESS_OVERRIDE.get();
        SUPPRESS_OVERRIDE.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_OVERRIDE.set(previous);
        }
    }

    public static ItemStack overrideStack(ItemStack original, LivingEntity entity, ModelTransformationMode mode) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        if (!shouldApply(original, entity, mode)) {
            return original;
        }
        SkinType type = WynnItemClassifier.classify(original);
        if (type == SkinType.UNKNOWN) {
            return original;
        }

        SkinSwapState state = WynnchangerClient.getSwapState();
        Optional<Identifier> selection = state.getSelection(type);
        if (selection.isEmpty()) {
            return original;
        }
        Optional<SkinModelMapping> mapping = SkinMappingResolver.resolveMappingForSelection(original, selection.get());
        if (mapping.isEmpty()) {
            return original;
        }

        ItemStack copy = original.copy();
        applyMappingToStack(copy, mapping.get());
        return copy;
    }

    public static Optional<ItemStack> buildStackForModel(Identifier modelId) {
        Optional<Identifier> baseItemId = WynnchangerClient.getSkinRegistry().findBaseItemForModel(modelId);
        if (baseItemId.isEmpty()) {
            return Optional.empty();
        }
        Optional<SkinModelMapping> mapping = SkinMappingResolver.resolveMappingForSelection(baseItemId.get(), modelId);
        if (mapping.isEmpty()) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(Registries.ITEM.get(baseItemId.get()));
        applyMappingToStack(stack, mapping.get());
        return Optional.of(stack);
    }

    private static boolean shouldApply(ItemStack original, LivingEntity entity, ModelTransformationMode mode) {
        if (Boolean.TRUE.equals(SUPPRESS_OVERRIDE.get())) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        if (entity == null) {
            return mode == ModelTransformationMode.GUI && isRelevantStack(original, client.player);
        }
        if (client.player.getId() != entity.getId()) {
            return false;
        }
        if (isHandMode(mode)) {
            return true;
        }
        return isRelevantStack(original, client.player);
    }

    private static boolean isRelevantStack(ItemStack original, LivingEntity entity) {
        if (original == null || original.isEmpty()) {
            return false;
        }
        if (ItemStack.areEqual(original, entity.getMainHandStack())
                || ItemStack.areEqual(original, entity.getOffHandStack())) {
            return true;
        }
        return ItemStack.areEqual(original, entity.getEquippedStack(EquipmentSlot.HEAD))
                || ItemStack.areEqual(original, entity.getEquippedStack(EquipmentSlot.CHEST))
                || ItemStack.areEqual(original, entity.getEquippedStack(EquipmentSlot.LEGS))
                || ItemStack.areEqual(original, entity.getEquippedStack(EquipmentSlot.FEET));
    }

    private static boolean isHandMode(ModelTransformationMode mode) {
        return switch (mode) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
                    THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> true;
            default -> false;
        };
    }


    static void applyMappingToStack(ItemStack stack, SkinModelMapping mapping) {
        if (mapping.propertyType() == SkinPropertyType.CUSTOM_MODEL_DATA) {
            applyCustomModelData(stack, mapping.value());
            return;
        }
        if (mapping.propertyType() == SkinPropertyType.DAMAGE) {
            applyDamageMapping(stack, mapping.value());
        }
    }

    private static void applyCustomModelData(ItemStack stack, float value) {
        CustomModelDataComponent component = new CustomModelDataComponent(
                List.of(value),
                List.of(),
                List.of(),
                List.of()
        );
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, component);
    }

    private static void applyDamageMapping(ItemStack stack, float value) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        int targetDamage = Math.round(value * (float) maxDamage);
        if (targetDamage < 0) {
            targetDamage = 0;
        } else if (targetDamage > maxDamage) {
            targetDamage = maxDamage;
        }
        stack.setDamage(targetDamage);
    }
}
