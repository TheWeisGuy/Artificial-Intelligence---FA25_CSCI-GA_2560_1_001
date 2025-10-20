import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DPLLSolver {

    private Map<String, Boolean> assignments = new TreeMap<>();
    private boolean verbose = true;

    public DPLLSolver(boolean verbose) {
        this.verbose = verbose;
    }

    // ========================= Core DPLL Algorithm ========================= //

    public boolean solve(List<List<String>> cnf) {
        return dpll(cnf);
    }

    private boolean dpll(List<List<String>> clauses) {
        clauses = simplify(clauses);

        // 1. All clauses satisfied
        if (clauses.isEmpty()) return true;

        // 2. Contradiction: empty clause
        for (List<String> c : clauses)
            if (c.isEmpty()) return false;

        // 3. Unit propagation
        for (List<String> clause : clauses) {
            if (clause.size() == 1) {
                String lit = clause.get(0);
                String var = lit.replace("!", "");
                boolean val = !lit.startsWith("!");
                if (verbose) System.out.println("easy case: Singleton " + var + "=" + val);
                assignments.put(var, val);
                return dpll(assign(clauses, var, val));
            }
        }

        // 4. Pure literal elimination
        Set<String> allVars = new HashSet<>();
        Set<String> pos = new HashSet<>();
        Set<String> neg = new HashSet<>();

        for (List<String> clause : clauses) {
            for (String lit : clause) {
                String v = lit.replace("!", "");
                allVars.add(v);
                if (lit.startsWith("!")) neg.add(v);
                else pos.add(v);
            }
        }

        for (String v : allVars) {
            if (!pos.contains(v)) {
                if (verbose) System.out.println("easy case: Pure literal " + v + "=false");
                assignments.put(v, false);
                return dpll(assign(clauses, v, false));
            }
            if (!neg.contains(v)) {
                if (verbose) System.out.println("easy case: Pure literal " + v + "=true");
                assignments.put(v, true);
                return dpll(assign(clauses, v, true));
            }
        }

        // 5. Pick smallest unassigned variable (lexicographically)
        String next = allVars.stream()
                .filter(v -> !assignments.containsKey(v))
                .sorted()
                .findFirst()
                .orElse(null);

        if (next == null) return true;

        // 6. Guess True
        if (verbose) System.out.println("hard case: guess " + next + "=true");
        assignments.put(next, true);
        if (dpll(assign(clauses, next, true))) return true;

        // 7. Backtrack -> guess False
        if (verbose) System.out.println("contradiction: backtrack guess " + next + "=false");
        assignments.put(next, false);
        return dpll(assign(clauses, next, false));
    }

    private List<List<String>> assign(List<List<String>> clauses, String var, boolean val) {
        List<List<String>> newClauses = new ArrayList<>();
        for (List<String> c : clauses) {
            if (c.contains(val ? var : "!" + var)) continue; // satisfied
            List<String> newC = c.stream()
                    .filter(l -> !l.equals(val ? "!" + var : var))
                    .collect(Collectors.toList());
            newClauses.add(newC);
        }
        return newClauses;
    }

    private List<List<String>> simplify(List<List<String>> clauses) {
        List<List<String>> clean = new ArrayList<>();
        for (List<String> c : clauses) {
            Set<String> s = new HashSet<>(c);
            boolean tautology = false;
            for (String l : s)
                if (s.contains(l.startsWith("!") ? l.substring(1) : "!" + l))
                    tautology = true;
            if (!tautology) clean.add(new ArrayList<>(s));
        }
        return clean;
    }

    public static List<List<String>> parseCNF(File file) throws IOException {
        List<List<String>> cnf = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                cnf.add(Arrays.asList(line.split("\\s+")));
            }
        }
        return cnf;
    }
    
    public Map<String, Boolean> getAssignments(){
        return this.assignments;
    }

    public static void main(String[] args) throws IOException {

        File inputFile = new File("oz.txt.test4.dp");
        boolean verbose = !(args.length > 1 && args[1].equalsIgnoreCase("--quiet"));

        List<List<String>> cnf = parseCNF(inputFile);

        DPLLSolver solver = new DPLLSolver(true);
        boolean result = solver.solve(cnf);

        if (result) {
            System.out.println("Satisfying assignment found:");
            solver.assignments.forEach((k,v) -> System.out.print(k + "=" + v + " "));
            System.out.println();
        } else {
            System.out.println("NO VALID ASSIGNMENT");
        }
    }
}
