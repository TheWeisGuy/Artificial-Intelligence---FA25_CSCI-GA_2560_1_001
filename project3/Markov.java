import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * Usage examples:
 * java Markov -max -df .9 -tol 0.0001 some-input.txt
 * java Markov -df 1.0 -iter 200 map.txt
 * 
 * CLI Args, java Markov <-max> <-df <float>> <-tol <float>> <txtFilePath>
 * 
 * Output:
 * - Optimal policy (only for true decision nodes) and values (formatted to 3
 * decimals).
 */
public class Markov {

    // config given by command line
    static class Config {
        double df = 1.0;
        boolean maximize = false;
        double tol = 1e-3;
        int iter = 150;
        String inputPath;
    }

    // fields for each node
    static class NodeSpec {

        String name;
        Double reward;
        List<String> edgeNames = null;
        List<Double> probs = null;

        public NodeSpec(String name) {
            this.name = name;
        }
    }

    // Resolved graph
    static class Graph {
        int n;
        String[] names;
        double[] reward;
        int[][] edges;
        double[][] probsRaw;

        boolean[] isTerminal;
        boolean[] isChance;
        boolean[] isDecision;
        boolean[] forcedForSingleEdge;
    }

    // Policy class
    // generates a policy given an array
    static class Policy {
        int[] actionIndex;

        Policy(int n) {
            actionIndex = new int[n];
            Arrays.fill(actionIndex, -1);
        }

        Policy copy() {
            Policy p = new Policy(actionIndex.length);
            System.arraycopy(actionIndex, 0, p.actionIndex, 0, actionIndex.length);
            return p;
        }

        boolean equalsTo(Policy other) {
            return Arrays.equals(this.actionIndex, other.actionIndex);
        }
    }

    // main method, parses a graph and generates the policy and runs markov for it
    public static void main(String[] args) {
        try {
            Config cfg = parseArgs(args);
            Map<String, NodeSpec> specs = parseInputFile(cfg.inputPath);
            Graph g = buildGraph(specs);

            // generate empty policy
            Policy pi = initialPolicy(g);

            // Policy iteration
            Policy optimalPolicy;
            double[] optimalValues;
            int safetyCap = 1000;
            while (true) {
                double[] V = valueIteration(g, pi, cfg);
                Policy piPrime = greedyPolicy(g, V, cfg);

                if (pi.equalsTo(piPrime) || safetyCap-- <= 0) {
                    optimalPolicy = piPrime;
                    optimalValues = V;
                    break;
                }
                pi = piPrime;
            }

            printOutput(g, optimalPolicy, optimalValues);
        } catch (UserError e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }

    // helper function parse flags given
    // returns a config object containing the parsed flags
    static Config parseArgs(String[] args) throws UserError {
        if (args.length == 0)
            throw new UserError("No arguments. Provide flags and an input file.");
        Config cfg = new Config();
        int i = 0;
        while (i < args.length) {
            String a = args[i];
            if (a.equals("-max")) {
                cfg.maximize = true;
                i++;
            } else if (a.equals("-df")) {
                if (i + 1 >= args.length)
                    throw new UserError("Missing argument for -df");
                cfg.df = parseDoubleStrict(args[++i], "-df");
                if (cfg.df < 0 || cfg.df > 1)
                    throw new UserError("Discount factor must be in [0,1]");
                i++;
            } else if (a.equals("-tol")) {
                if (i + 1 >= args.length)
                    throw new UserError("Missing argument for -tol");
                cfg.tol = parseDoubleStrict(args[++i], "-tol");
                if (cfg.tol <= 0)
                    throw new UserError("Tolerance must be > 0");
                i++;
            } else if (a.equals("-iter")) {
                if (i + 1 >= args.length)
                    throw new UserError("Missing argument for -iter");
                cfg.iter = parseIntStrict(args[++i], "-iter");
                if (cfg.iter <= 0)
                    throw new UserError("iter must be > 0");
                i++;
            } else if (a.startsWith("-")) {
                throw new UserError("Unknown flag: " + a);
            } else {
                // input file (assume last non-flag token)
                if (cfg.inputPath != null)
                    throw new UserError("Multiple input files provided. Only one is allowed.");
                cfg.inputPath = a;
                i++;
            }
        }
        if (cfg.inputPath == null)
            throw new UserError("Missing input file path.");
        return cfg;
    }

    static double parseDoubleStrict(String s, String flag) throws UserError {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new UserError("Invalid float for " + flag + ": " + s);
        }
    }

    static int parseIntStrict(String s, String flag) throws UserError {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new UserError("Invalid integer for " + flag + ": " + s);
        }
    }

    // parses text file so it can be processed
    static Map<String, NodeSpec> parseInputFile(String path) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(path));

        // Patterns (lenient about spaces)
        Pattern rewardPat = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");
        Pattern edgesPat = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*:\\s*\\[\\s*([A-Za-z0-9_\\s,]+?)\\s*]\\s*$");
        // Allow "F%.8" or "F % .8 .2"
        Pattern probsPat = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*%\\s*([0-9Ee+\\-\\.\\s]+)\\s*$");

        Map<String, NodeSpec> nodes = new LinkedHashMap<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;

            Matcher m;

            m = rewardPat.matcher(line);
            if (m.matches()) {
                String name = m.group(1);
                double val = Double.parseDouble(m.group(2));
                NodeSpec ns = nodes.computeIfAbsent(name, NodeSpec::new);
                ns.reward = val;
                continue;
            }

            m = edgesPat.matcher(line);
            if (m.matches()) {
                String name = m.group(1);
                String inner = m.group(2);
                NodeSpec ns = nodes.computeIfAbsent(name, NodeSpec::new);
                List<String> outs = new ArrayList<>();
                for (String tok : inner.split(",")) {
                    String t = tok.trim();
                    if (!t.isEmpty())
                        outs.add(t);
                }
                ns.edgeNames = outs;
                continue;
            }

            m = probsPat.matcher(line);
            if (m.matches()) {
                String name = m.group(1);
                String inner = m.group(2);
                NodeSpec ns = nodes.computeIfAbsent(name, NodeSpec::new);
                List<Double> ps = new ArrayList<>();
                for (String tok : inner.trim().split("\\s+")) {
                    if (tok.isEmpty())
                        continue;
                    ps.add(Double.parseDouble(tok));
                }
                if (ps.isEmpty())
                    throw new UserError("Empty probability list for " + name);
                ns.probs = ps;
                continue;
            }

            throw new UserError("Unrecognized line: " + line);
        }

        if (nodes.isEmpty())
            throw new UserError("No nodes parsed. Check input.");

        return nodes;
    }

    /**
     * Builds the graph
     * 
     * @param specs parsed input file
     * @return
     * @throws UserError
     */
    static Graph buildGraph(Map<String, NodeSpec> specs) throws UserError {
        for (NodeSpec ns : specs.values()) {
            if (ns.edgeNames != null) {
                for (String ref : ns.edgeNames) {
                    if (!specs.containsKey(ref)) {
                        throw new UserError("Edge target '" + ref + "' referenced by " + ns.name
                                + " must be separately defined (reward, edges, or probs).");
                    }
                }
            }
        }

        // Indexing
        int n = specs.size();
        String[] names = specs.keySet().toArray(new String[0]);
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++)
            idx.put(names[i], i);

        Graph g = new Graph();
        g.n = n;
        g.names = names;
        g.reward = new double[n];
        g.edges = new int[n][];
        g.probsRaw = new double[n][];
        g.isTerminal = new boolean[n];
        g.isChance = new boolean[n];
        g.isDecision = new boolean[n];
        g.forcedForSingleEdge = new boolean[n];

        for (int i = 0; i < n; i++) {
            NodeSpec ns = specs.get(names[i]);

            if (ns.reward == null) {
                throw new UserError("Missing reward definition for node '" + ns.name + "'");
            }
            g.reward[i] = ns.reward;

            if (ns.edgeNames == null || ns.edgeNames.isEmpty()) {
                // terminal
                g.edges[i] = new int[0];
                g.isTerminal[i] = true;

                if (ns.probs != null) {
                    throw new UserError("Terminal node '" + ns.name + "' cannot have a probability line.");
                }
                continue;
            }

            // has edges
            int deg = ns.edgeNames.size();
            int[] outs = new int[deg];
            for (int k = 0; k < deg; k++)
                outs[k] = idx.get(ns.edgeNames.get(k));
            g.edges[i] = outs;

            if (deg == 1) {
                // Forced single-edge transition
                g.isDecision[i] = true;
                g.forcedForSingleEdge[i] = true;
                if (ns.probs != null && ns.probs.size() == 1 && Math.abs(ns.probs.get(0) - 1.0) > 1e-12) {
                    /*
                     * treat single edge as forced 1.0
                     */
                } else if (ns.probs != null && ns.probs.size() != 1) {
                    throw new UserError("Node '" + ns.name
                            + "' has one edge but multiple probs given. Remove the % line or give a single prob 1.0.");
                }
                g.probsRaw[i] = null;
                continue;
            }

            // multiple edges
            if (ns.probs == null) {
                // default: decision with p=1
                g.isDecision[i] = true;
                g.probsRaw[i] = null; // p=1 implied
            } else if (ns.probs.size() == 1) {
                // decision with success p
                g.isDecision[i] = true;
                g.probsRaw[i] = new double[] { ns.probs.get(0) };
                if (g.probsRaw[i][0] < 0 || g.probsRaw[i][0] > 1) {
                    throw new UserError("Node '" + ns.name + "' has invalid success probability " + g.probsRaw[i][0]);
                }
            } else if (ns.probs.size() == deg) {
                // chance node
                g.isChance[i] = true;
                g.probsRaw[i] = new double[deg];
                double sum = 0;
                for (int k = 0; k < deg; k++) {
                    g.probsRaw[i][k] = ns.probs.get(k);
                    sum += g.probsRaw[i][k];
                }
                if (Math.abs(sum - 1.0) > 1e-9) {
                    throw new UserError("Chance node '" + ns.name + "' probabilities must sum to 1 (got " + sum + ").");
                }
                for (double p : g.probsRaw[i]) {
                    if (p < -1e-12 || p > 1 + 1e-12)
                        throw new UserError("Chance node '" + ns.name + "' has invalid probability " + p);
                }
            } else {
                throw new UserError("Node '" + ns.name
                        + "': probability list must have either 1 item (decision) or |edges| items (chance).");
            }
            System.err.printf("%s: reward=%.1f edges=%s probs=%s%n",
                    names[i], g.reward[i],
                    Arrays.toString(g.edges[i]),
                    Arrays.toString(g.probsRaw[i]));
        }

        return g;
    }

    // create initial policy
    static Policy initialPolicy(Graph g) {
        Policy pi = new Policy(g.n);
        for (int s = 0; s < g.n; s++) {
            if (g.isDecision[s] && !g.forcedForSingleEdge[s]) {
                // choose first action arbitrarily
                pi.actionIndex[s] = 0;
            } else {
                pi.actionIndex[s] = -1;
            }
        }
        return pi;
    }

    /**
     * Perform value iterations on the graph
     * 
     * @param g
     * @param pi
     * @param cfg
     * @return
     */
    static double[] valueIteration(Graph g, Policy pi, Config cfg) {
        int n = g.n;
        double[][] P = buildTransitionUnderPolicy(g, pi);

        // Build linear system: (I - df * P) * V = reward
        double[][] A = new double[n][n];
        double[] b = new double[n];

        for (int i = 0; i < n; i++) {
            b[i] = g.reward[i];
            for (int j = 0; j < n; j++) {
                double pij = P[i][j];
                A[i][j] = (i == j ? 1.0 : 0.0) - cfg.df * pij;
            }
        }

        System.err.println("df = " + cfg.df);
        int idxA = -1, idxQ = -1, idxB = -1;
        for (int i = 0; i < n; i++) {
            if (g.names[i].equals("A"))
                idxA = i;
            if (g.names[i].equals("Q"))
                idxQ = i;
            if (g.names[i].equals("B"))
                idxB = i;
        }
        System.err.println("Row A of (I - df P): " + Arrays.toString(A[idxA]));
        System.err.println("b[A] (reward A): " + b[idxA]);
        System.err.println("Row B of (I - df P): " + Arrays.toString(A[idxB]));
        System.err.println("b[B] (reward B): " + b[idxB]);
        // Solve A * x = b via Gaussian elimination with partial pivoting
        double[] x = gaussianSolve(A, b);
        System.err.println("Residuals (A x - b):");
        for (int i = 0; i < n; i++) {
            double lhs = 0.0;
            for (int j = 0; j < n; j++) {
                lhs += A[i][j] * x[j];
            }
            double r_i = lhs - b[i];
            System.err.printf("  %s : %.6f%n", g.names[i], r_i);
        }
        return x;
    }

    // Solve A x = b for a dense n x n matrix A using Gaussian elimination
    static double[] gaussianSolve(double[][] A, double[] b) {
        int n = A.length;
        double[][] a = new double[n][n];
        double[] x = new double[n];
        double[] rhs = new double[n];

        // Copy inputs to avoid mutating original arrays
        for (int i = 0; i < n; i++) {
            rhs[i] = b[i];
            System.arraycopy(A[i], 0, a[i], 0, n);
        }

        // Forward elimination with partial pivoting
        for (int k = 0; k < n; k++) {
            // Find pivot row
            int pivot = k;
            double maxAbs = Math.abs(a[k][k]);
            for (int i = k + 1; i < n; i++) {
                double val = Math.abs(a[i][k]);
                if (val > maxAbs) {
                    maxAbs = val;
                    pivot = i;
                }
            }

            // Swap rows if needed
            if (pivot != k) {
                double[] tmpRow = a[k];
                a[k] = a[pivot];
                a[pivot] = tmpRow;

                double tmpVal = rhs[k];
                rhs[k] = rhs[pivot];
                rhs[pivot] = tmpVal;
            }

            double pivotVal = a[k][k];
            if (Math.abs(pivotVal) < 1e-12) {
                throw new RuntimeException("Singular matrix in gaussianSolve");
            }

            // Normalize pivot row
            for (int j = k; j < n; j++) {
                a[k][j] /= pivotVal;
            }
            rhs[k] /= pivotVal;

            // Eliminate below
            for (int i = k + 1; i < n; i++) {
                double factor = a[i][k];
                if (factor == 0.0)
                    continue;
                for (int j = k; j < n; j++) {
                    a[i][j] -= factor * a[k][j];
                }
                rhs[i] -= factor * rhs[k];
            }
        }

        // Back substitution
        for (int i = n - 1; i >= 0; i--) {
            double sum = rhs[i];
            for (int j = i + 1; j < n; j++) {
                sum -= a[i][j] * x[j];
            }
            x[i] = sum;
        }

        return x;
    }

    static double[][] buildTransitionUnderPolicy(Graph g, Policy pi) {
        int n = g.n;
        double[][] P = new double[n][n];

        for (int s = 0; s < n; s++) {
            if (g.isTerminal[s]) {
                // Terminal, no future transitions so pass
                continue;
            }

            int[] outs = g.edges[s];
            if (outs == null || outs.length == 0)
                continue;

            if (g.forcedForSingleEdge[s]) {
                int t = outs[0];
                P[s][t] = 1.0;
                continue;
            }

            if (g.isChance[s]) {
                double[] probs = g.probsRaw[s];
                for (int k = 0; k < outs.length; k++) {
                    P[s][outs[k]] += probs[k];
                }
            } else if (g.isDecision[s]) {
                int a = pi.actionIndex[s];
                if (a < 0 || a >= outs.length) {
                    // If policy not set then skip
                    a = 0;
                }
                if (g.probsRaw[s] == null) {
                    // Deterministic to chosen action (p=1)
                    P[s][outs[a]] = 1.0;
                } else {
                    // Success prob p; failures split among others
                    double p = g.probsRaw[s][0];
                    int deg = outs.length;
                    double fail = (deg > 1) ? (1.0 - p) / (deg - 1) : 0.0;
                    for (int k = 0; k < deg; k++) {
                        P[s][outs[k]] = (k == a) ? p : fail;
                    }
                }
            } else {
                // if reached invalid node
                throw new RuntimeException("Invalid node classification at " + g.names[s]);
            }
        }
        return P;
    }

    static Policy greedyPolicy(Graph g, double[] V, Config cfg) {
        Policy pi = new Policy(g.n);

        for (int s = 0; s < g.n; s++) {
            if (g.isTerminal[s]) {
                pi.actionIndex[s] = -1;
                continue;
            }
            if (g.forcedForSingleEdge[s]) {
                pi.actionIndex[s] = -1;
                continue;
            }
            if (g.isChance[s]) {
                pi.actionIndex[s] = -1;
                continue;
            }

            if (g.isDecision[s]) {
                int[] outs = g.edges[s];
                if (outs.length == 0) {
                    pi.actionIndex[s] = -1;
                    continue;
                }

                // Evaluate each action's value v(s)
                int bestA = 0;
                double bestVal = cfg.maximize ? -Double.MAX_VALUE : Double.MAX_VALUE;

                for (int a = 0; a < outs.length; a++) {
                    double cont = 0.0;
                    if (g.probsRaw[s] == null) {
                        cont = V[outs[a]];
                    } else {
                        double p = g.probsRaw[s][0];
                        int deg = outs.length;
                        double fail = (deg > 1) ? (1.0 - p) / (deg - 1) : 0.0;
                        for (int k = 0; k < deg; k++) {
                            double pk = (k == a) ? p : fail;
                            cont += pk * V[outs[k]];
                        }
                    }
                    double total = g.reward[s] + (cfg.df * cont);
                    if (cfg.maximize) {
                        if (total > bestVal) {
                            bestVal = total;
                            bestA = a;
                        }
                    } else {
                        if (total < bestVal) {
                            bestVal = total;
                            bestA = a;
                        }
                    }
                }
                pi.actionIndex[s] = bestA;
            } else {
                pi.actionIndex[s] = -1;
            }
        }

        return pi;
    }

    /**
     * Prints output
     * 
     * @param g  graph created by buildGraph
     * @param pi Computed policy
     * @param V  values generated
     */
    static void printOutput(Graph g, Policy pi, double[] V) {
        // Detect if there is any policy-relevant decision node
        boolean hasPolicy = false;
        for (int s = 0; s < g.n; s++) {
            if (g.isDecision[s] && !g.forcedForSingleEdge[s]) {
                hasPolicy = true;
                break;
            }
        }

        if (hasPolicy) {
            System.out.println("Optimal Policy:");
            for (int s = 0; s < g.n; s++) {
                if (g.isDecision[s] && !g.forcedForSingleEdge[s]) {
                    int a = pi.actionIndex[s];
                    int[] outs = g.edges[s];
                    String to = (a >= 0 && a < outs.length) ? g.names[outs[a]] : "(invalid)";
                    System.out.printf("  %s -> %s%n", g.names[s], to);
                }
            }
        } else {
            System.out.println("No policy (all nodes are chance/terminal/forced-single-edge).");
        }

        System.out.println("Values:");
        for (int s = 0; s < g.n; s++) {
            System.out.printf("  %s : %.3f%n", g.names[s], V[s]);
        }
    }

    // User error class for bad flag usage
    static class UserError extends Exception {
        public UserError(String msg) {
            super(msg);
        }
    }
}
