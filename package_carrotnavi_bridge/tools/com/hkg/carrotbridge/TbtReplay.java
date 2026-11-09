package com.hkg.carrotbridge;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

/**
 * Offline replay of a real-vehicle tbt_live_trace.log through the current parser/mapper.
 *
 * Usage: java -cp <classes> com.hkg.carrotbridge.TbtReplay tbt_live_trace.log.old tbt_live_trace.log
 *
 * For every TX7706 line recorded on the car it rebuilds the packet with the CURRENT code and prints
 * one compact row: relative time, recorded (old) TBT fields vs. replayed (new) TBT fields.
 */
public class TbtReplay {
    public static void main(String[] args) throws Exception {
        CarrotBridgeMain.NavState st = new CarrotBridgeMain.NavState();
        long t0 = -1;
        String lastRow = "";
        Map<Integer, Integer> turnHist = new TreeMap<Integer, Integer>();
        int tx = 0;
        int sdiMismatch = 0;
        String[] sdiKeys = {"nSdiType", "nSdiSpeedLimit", "nSdiPlusType", "nRoadLimitSpeed", "roadcate"};
        for (String path : args) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), Charset.forName("UTF-8")));
            String line;
            StringBuilder dump = null;
            while ((line = br.readLine()) != null) {
                int sp = line.indexOf(' ');
                boolean stamped = sp > 0 && line.substring(0, sp).matches("\\d{12,}");
                if (!stamped) {
                    if (dump != null) dump.append(line).append('\n');
                    continue;
                }
                if (dump != null) {
                    // snapshot belongs to the previous timestamp; parse it before advancing the clock
                    CarrotBridgeMain.parseDumpsysCluster(dump.toString(), st);
                    dump = null;
                }
                long ts = Long.parseLong(line.substring(0, sp));
                if (t0 < 0) t0 = ts;
                CarrotBridgeMain.clockOverrideMs = ts;
                String rest = line.substring(sp + 1);
                if (rest.startsWith("LOG: ")) {
                    CarrotBridgeMain.parseLogcatLine(rest.substring(5), st, null);
                } else if (rest.startsWith("DUMPSYS_SNAPSHOT:")) {
                    dump = new StringBuilder();
                } else if (rest.startsWith("TX7706: ")) {
                    String old = rest.substring(8);
                    CarrotBridgeMain.CarrotPacket p = CarrotBridgeMain.buildCarrotPacket(st);
                    tx++;
                    String newJson = p.toJson();
                    for (String k : sdiKeys) {
                        if (!jsonVal(old, k).equals(jsonVal(newJson, k))) {
                            sdiMismatch++;
                            System.out.println(String.format("%5d  SDI DIFF %s old=%s new=%s", (ts - t0) / 1000, k, jsonVal(old, k), jsonVal(newJson, k)));
                        }
                    }
                    Integer c = turnHist.get(p.nTBTTurnType);
                    turnHist.put(p.nTBTTurnType, c == null ? 1 : c + 1);
                    String row = String.format("new tbt=%d/%dm '%s' next=%d/%dm goal=%dm/%ds | old tbt=%s/%sm",
                            p.nTBTTurnType, p.nTBTDist, p.szTBTMainText, p.nTBTTurnTypeNext, p.nTBTDistNext,
                            p.nGoPosDist, p.nGoPosTime, jsonVal(old, "nTBTTurnType"), jsonVal(old, "nTBTDist"));
                    if (!row.equals(lastRow)) {
                        System.out.println(String.format("%5d  %s", (ts - t0) / 1000, row));
                        lastRow = row;
                    }
                }
            }
            if (dump != null) CarrotBridgeMain.parseDumpsysCluster(dump.toString(), st);
            br.close();
        }
        System.out.println("SDI/road fields differing from the recorded packets: " + sdiMismatch);
        System.out.println("TX packets replayed: " + tx + "  nTBTTurnType histogram: " + turnHist);
    }

    private static String jsonVal(String json, String key) {
        int i = json.indexOf("\"" + key + "\":");
        if (i < 0) return "?";
        int s = i + key.length() + 3;
        int e = s;
        while (e < json.length() && json.charAt(e) != ',' && json.charAt(e) != '}') e++;
        return json.substring(s, e);
    }
}
