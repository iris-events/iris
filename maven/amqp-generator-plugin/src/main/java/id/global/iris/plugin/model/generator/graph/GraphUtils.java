package id.global.iris.plugin.model.generator.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphUtils {

    private static String[] splitMe(String s) {
        return s.split(",");
    }

    private static String getFirst(String[] s) {
        return s[0];
    }

    private static String getLast(String[] s) {
        return s[s.length - 1];
    }

    private static String[] removeFirst(String res) {
        String[] tmp = splitMe(res);
        return Arrays.stream(tmp)
                .skip(1).toArray(String[]::new);
    }

    private static boolean areMatching(String element, String[] splitted) {
        return getFirst(splitMe(element)).equalsIgnoreCase(getLast(splitted));
    }

    private static List<String> calculateForRecursive(List<String> list) {
        ArrayList<String> returnList = new ArrayList<>();
        list.forEach(value -> {
            final StringBuilder sb = new StringBuilder(value);
            final String finalString = value;
            final String[] splitted = splitMe(finalString);

            List<String> as = list.stream()
                    .filter(element -> areMatching(element, splitted))
                    .map(res -> String.join(",", removeFirst(res)))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

            if (!as.isEmpty()) {
                as.stream()
                        .filter(s -> !s.isBlank())
                        .forEach(app -> {
                            sb.append(",").append(app);
                            returnList.add(sb.toString());
                            sb.setLength(0);
                            sb.append(finalString);
                        });
            } else {
                returnList.add(sb.toString());
                sb.setLength(0);
            }
        });
        return returnList;
    }

    private static List<String> recursive(List<String> list1) {
        List<String> ars = calculateForRecursive(list1);
        if (ars.size() == list1.size())
            return ars;
        return recursive(ars);

    }

    private static List<String> prepareListFromMap(Map<String, List<String>> map) {
        List<String> list = new ArrayList<>();
        map.forEach((k, as) -> {
            final StringBuilder sb = new StringBuilder();
            sb.append(k);
            if (as.isEmpty()) {
                list.add(sb.toString());
                sb.setLength(0);
                sb.append(k);
            } else {
                as.forEach(val -> {
                    sb.append(",").append(val);
                    list.add(sb.toString());
                    sb.setLength(0);
                    sb.append(k);
                });
            }
        });
        return list;
    }

    public static List<String> getClassChains(Map<String, List<String>> map) {
        List<String> list = prepareListFromMap(map);

        List<String> distinctNodes = list.stream()
                .flatMap(str -> Arrays.stream(str.split(",")))
                .distinct()
                .collect(Collectors.toList());

        Graph graph = new Graph(distinctNodes.size());

        list.forEach(l -> {
            String[] edges = l.split(",");
            graph.addEdge(distinctNodes.indexOf(edges[0]), distinctNodes.indexOf(edges[1]));
        });

        List<String> res = new ArrayList<>();

        if (!graph.isCyclic()) {
            res = recursive(list);
        }

        return res.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
    }
}
