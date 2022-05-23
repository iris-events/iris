package id.global.iris.asyncapi.runtime.util;

public class VersionUtil {
    public static String bumpVersion(String version, int spillOver) {
        String[] split = version.split("\\.");

        for (int i = split.length - 1; i >= 0; i--) {
            int n = Integer.parseInt(split[i]);
            if (n < spillOver) {
                split[i] = (n + 1) + "";
                break;
            } else {
                split[i] = 0 + "";
            }
        }
        return String.join(".", split);
    }
}
