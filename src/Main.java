import benchmark.Benchmark;
import demo.CityDemo;
import testing.GraphTests;

/** Main entry point: java -cp out Main [all|test|benchmark|demo] */
public final class Main {
    public static void main(String[] args) throws Exception {
        String action = args.length == 0 ? "all" : args[0].toLowerCase();
        switch (action) {
            case "all" -> { GraphTests.main(new String[0]); Benchmark.main(new String[0]); }
            case "test" -> GraphTests.main(new String[0]);
            case "demo" -> CityDemo.main(new String[0]);
            case "benchmark" -> Benchmark.main(new String[0]);
            default -> {
                System.err.println("Usage: java -cp out Main [all|test|benchmark|demo]");
                System.exit(2);
            }
        }
    }
}
