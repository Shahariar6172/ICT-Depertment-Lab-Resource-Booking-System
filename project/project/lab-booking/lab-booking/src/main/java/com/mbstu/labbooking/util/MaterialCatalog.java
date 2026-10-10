package com.mbstu.labbooking.util;

import com.mbstu.labbooking.model.DeviceGroup;
import com.mbstu.labbooking.model.MaterialOption;
import java.util.*;
import java.util.regex.*;

/** Resolve labels without changing the inventory identity used to reserve physical units. */
public final class MaterialCatalog {
    private MaterialCatalog() { }
    private static String key(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    public static List<MaterialOption> build(List<DeviceGroup> stock, List<MaterialOption> definitions,
                                            Set<String> subjectMaterials) {
        List<MaterialOption> result = new ArrayList<>();
        for (DeviceGroup group : stock) {
            MaterialOption label = definitions.stream()
                    .filter(d -> key(d.getDeviceName()).equals(key(group.getName()))
                            && d.getCategory().equalsIgnoreCase(group.getCategory())).findFirst().orElse(null);
            if (label == null) {
                // Legacy admin categories may differ; an unambiguous name still has the same label.
                List<MaterialOption> matches = definitions.stream()
                        .filter(d -> key(d.getDeviceName()).equals(key(group.getName()))).toList();
                if (!matches.isEmpty() && matches.stream().allMatch(d ->
                        d.getMaterialName().equals(matches.get(0).getMaterialName()) &&
                        d.getVariantName().equals(matches.get(0).getVariantName()))) label = matches.get(0);
            }
            if (label == null) label = infer(group);
            result.add(new MaterialOption(label.getMaterialName(), label.getVariantName(),
                    group.getName(), group.getCategory(), group.getAvailable()));
        }
        // Catalog entries remain visible with zero availability when no physical stock is registered.
        for (MaterialOption definition : definitions) {
            boolean present = result.stream().anyMatch(r -> r.getMaterialName().equals(definition.getMaterialName())
                    && r.getVariantName().equals(definition.getVariantName()));
            if (!present) result.add(new MaterialOption(definition.getMaterialName(), definition.getVariantName(),
                    definition.getDeviceName(), definition.getCategory(), 0));
        }
        return result.stream().filter(o -> subjectMaterials == null || subjectMaterials.stream()
                        .anyMatch(m -> key(m).equals(key(o.getMaterialName())) || key(m).equals(key(o.getDeviceName()))))
                .sorted(Comparator.comparing(MaterialOption::getMaterialName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MaterialOption::getVariantName).thenComparing(MaterialOption::getCategory))
                .toList();
    }

    private static MaterialOption infer(DeviceGroup group) {
        String name = group.getName(), normalized = key(name), family = name, variant = "Standard";
        if (normalized.contains("breadboard")) {
            family = "Breadboard";
            Matcher parts = Pattern.compile("(?i)([123])\\s*[- ]?\\s*parts?").matcher(name);
            variant = parts.find() ? parts.group(1) + (parts.group(1).equals("1") ? " part" : " parts") : name;
        } else if (group.getCategory().equalsIgnoreCase("SENSOR") || normalized.contains("sensor")) {
            family = "Sensor"; variant = name;
        } else if (normalized.contains("arduino")) {
            family = "Arduino"; variant = name;
        } else if (normalized.contains("multimeter")) {
            family = "Digital Multimeter"; variant = name;
        } else if (normalized.contains("soldering")) {
            family = "Soldering Iron 60W"; variant = name;
        }
        return new MaterialOption(family, variant, name, group.getCategory(), group.getAvailable());
    }
}
