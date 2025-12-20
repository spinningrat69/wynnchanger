package io.wynnchanger.client.model;

import io.wynnchanger.client.SkinModelMapping;
import io.wynnchanger.client.SkinPropertyType;
import io.wynnchanger.client.SkinPropertyValue;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class SkinMappingResolver {
    private SkinMappingResolver() {
    }

    public static Optional<SkinType> resolveType(ItemStack stack) {
        return resolveMappingForStack(stack).map(SkinModelMapping::type);
    }

    public static Optional<SkinModelMapping> resolveMappingForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        Identifier baseItemId = Registries.ITEM.getId(stack.getItem());
        if (baseItemId == null) {
            return Optional.empty();
        }
        return resolveMappingForStack(baseItemId, stack);
    }

    public static Optional<SkinModelMapping> resolveMappingForStack(Identifier baseItemId, ItemStack stack) {
        SkinPropertyValue customData = readCustomModelData(stack);
        if (customData != null) {
            Optional<SkinModelMapping> mapping = WynnchangerClient.getSkinRegistry().findMapping(baseItemId, customData);
            if (mapping.isPresent()) {
                return mapping;
            }
        }

        SkinPropertyValue damageValue = readDamageValue(stack);
        if (damageValue != null) {
            Optional<SkinModelMapping> mapping = WynnchangerClient.getSkinRegistry().findMapping(baseItemId, damageValue);
            if (mapping.isPresent()) {
                return mapping;
            }
        }

        return Optional.empty();
    }

    public static Optional<SkinModelMapping> resolveMappingForSelection(ItemStack stack, Identifier modelId) {
        if (stack == null || stack.isEmpty() || modelId == null) {
            return Optional.empty();
        }
        Identifier baseItemId = Registries.ITEM.getId(stack.getItem());
        if (baseItemId == null) {
            return Optional.empty();
        }
        return resolveMappingForSelection(baseItemId, modelId);
    }

    public static Optional<SkinModelMapping> resolveMappingForSelection(Identifier baseItemId, Identifier modelId) {
        if (baseItemId == null || modelId == null) {
            return Optional.empty();
        }
        return WynnchangerClient.getSkinRegistry().findMapping(baseItemId, modelId);
    }

    private static SkinPropertyValue readCustomModelData(ItemStack stack) {
        CustomModelDataComponent customModelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (customModelData == null) {
            return null;
        }
        Float value = customModelData.getFloat(0);
        if (value == null) {
            return null;
        }
        return new SkinPropertyValue(SkinPropertyType.CUSTOM_MODEL_DATA, value, 0.001f);
    }

    private static SkinPropertyValue readDamageValue(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return null;
        }
        float ratio = (float) stack.getDamage() / (float) maxDamage;
        return new SkinPropertyValue(SkinPropertyType.DAMAGE, ratio, 0.0005f);
    }
}
