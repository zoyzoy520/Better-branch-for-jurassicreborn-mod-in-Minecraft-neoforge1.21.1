package net.example.jrtameall;

import java.util.HashMap;
import java.util.Map;

/**
 * Hand-tuned per-species seat height offsets, in blocks, added on top of the
 * base formula max(bbHeight * 0.6, 0.45) + size-class raise. Adjusted manually
 * per user test reports.
 */
public final class SeatOffsets {

    private static final Map<String, Double> OFFSETS = new HashMap<>();

    static {
        // 南方巨兽龙:大档(+3.0)基础上再 +2
        OFFSETS.put("giganotosaurus", 2.0D);
        // 三角龙:大档(+3.0)基础上累计 -5.5(先 -1 再 -1.5 再 -1 再 -2)
        OFFSETS.put("triceratops", -5.5D);
        // 三角龙类(其他大型角龙):当前基础上 -1
        OFFSETS.put("sinoceratops", -1.0D);
        OFFSETS.put("chasmosaurus", -1.0D);
        OFFSETS.put("styracosaurus", -1.0D);
        // 中型恐龙:小档(+1.5)基础上累计 -3(先 -1 再 -2)
        OFFSETS.put("parasaurolophus", -3.0D);
        // 重爪龙/迅棘龙:中型档(+1.5)基础上 -2
        OFFSETS.put("baryonyx", -2.0D);
        OFFSETS.put("spinoraptor", -2.0D);
        // 镰刀龙:当前基础上累计 -2.5(先 -1 再 -1.5)
        OFFSETS.put("therizinosaurus", -2.5D);
        // 小型恐龙:小档(+1.5)基础上累计 -2.5(先 -1.5 再 -1)
        OFFSETS.put("gallimimus", -2.5D);
        OFFSETS.put("velociraptor", -2.5D);
        OFFSETS.put("velociraptorblue", -2.5D);
        OFFSETS.put("velociraptorcharlie", -2.5D);
        OFFSETS.put("velociraptordelta", -2.5D);
        OFFSETS.put("velociraptorecho", -2.5D);
        // 剑齿虎:小档(+1.5)基础上累计 -3.5(先 -2.5 再 -1)
        OFFSETS.put("smilodon", -3.5D);
        // 恐鳄:当前基础上累计 +2(先 +1 再 +1)
        OFFSETS.put("deinosuchus", 1.0D);
        // 风神翼龙:当前基础上 -1
        OFFSETS.put("quetzalcoatlus", -1.0D);
    }

    private SeatOffsets() {
    }

    public static boolean contains(String species) {
        return OFFSETS.containsKey(species);
    }

    public static void set(String species, double offset) {
        OFFSETS.put(species, offset);
    }

    public static double get(String species) {
        return OFFSETS.getOrDefault(species, 0.0D);
    }
}
