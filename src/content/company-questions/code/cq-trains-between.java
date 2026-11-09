record Stop(String train, String station, int arrival, int departure) {}

int minutes(int hhmm) {
    return hhmm / 100 * 60 + hhmm % 100;
}

List<String> trainsBetween(List<Stop> stops, String from, String to) {
    Map<String, Integer> leaves = new HashMap<>(), arrives = new HashMap<>();
    for (Stop s : stops) {
        if (s.station().equals(from)) leaves.put(s.train(), minutes(s.departure()));
        if (s.station().equals(to)) arrives.put(s.train(), minutes(s.arrival()));
    }
    Map<String, Integer> travel = new HashMap<>();
    for (String train : leaves.keySet()) {
        Integer end = arrives.get(train);
        if (end != null && end > leaves.get(train)) travel.put(train, end - leaves.get(train));
    }
    List<String> out = new ArrayList<>(travel.keySet());
    out.sort(Comparator.comparing(travel::get));
    return out;
}
