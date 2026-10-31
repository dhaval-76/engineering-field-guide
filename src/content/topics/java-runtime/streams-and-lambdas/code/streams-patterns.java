// Top 3 customers by total spend (excluding cancelled orders)
List<String> top3 = orders.stream()
        .filter(o -> o.status() != Status.CANCELLED)
        .collect(Collectors.groupingBy(Order::customerId,
                Collectors.summingLong(Order::totalPaise)))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(3)
        .map(Map.Entry::getKey)
        // Java 16+: unmodifiable list
        .toList();

// Highest-paid employee per department
Map<String, Optional<Employee>> topPaid = employees.stream()
        .collect(Collectors.groupingBy(Employee::dept,
                Collectors.maxBy(Comparator.comparingLong(Employee::salary))));

// Flatten nested collections
Set<String> skills = employees.stream()
        .flatMap(e -> e.skills().stream())
        .collect(Collectors.toSet());

// Split into two groups
Map<Boolean, List<Employee>> seniorOrNot = employees.stream()
        .collect(Collectors.partitioningBy(e -> e.years() >= 5));

// toMap with a merge function — without it, duplicate keys throw IllegalStateException
Map<String, Long> wordCounts = words.stream().collect(Collectors.toMap(w -> w, w -> 1L, Long::sum));

// First match, with a default (short-circuits: stops at the first hit)
String firstLongA = strings.stream()
        .filter(s -> s.startsWith("A") && s.length() > 5)
        .findFirst()
        .map(String::toUpperCase)
        .orElse("NONE");

// Primitive streams avoid boxing
long total = orders.stream().mapToLong(Order::totalPaise).sum();
IntSummaryStatistics stats = scores.stream().mapToInt(Integer::intValue).summaryStatistics();
