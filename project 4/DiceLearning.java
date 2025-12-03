import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class DiceLearning {

    // Configuration holder
    static class Config {
        int NS; // sides per die
        int ND; // max dice
        int H; // high winning score
        int L; // low winning score
        int G; // number of training games
        int M; // exploration vs exploitation parameter
        boolean verbose; //type -v for verbose
    }

    private static final Random RNG = new Random();

    /*
     * Wins[x][y][d], Losses[x][y][d]
     * // x: current player score (0..L-1)
     * // y: opponent score (0..L-1)
     * d: number of dice (1..ND); index 0 unused
     */
    private static int[][][] wins;
    private static int[][][] losses;

    public static void main(String[] args) {
        Config cfg = parseArgs(args);
        validateConfig(cfg);
        initTables(cfg);

        train(cfg);

        printPolicyTable(cfg);
    }

    /**
     * Parses and creates the args object
     * 
     * @param args Array of arg strings from the terminal input
     * @return Config object
     */
    private static Config parseArgs(String[] args) {
        Config cfg = new Config();
        // Use sentinel values to ensure required flags are present
        cfg.NS = -1;
        cfg.ND = -1;
        cfg.H = -1;
        cfg.L = -1;
        cfg.G = -1;
        cfg.M = -1;
        cfg.verbose = false;

        int i = 0;
        while (i < args.length) {
            String flag = args[i];
            if (flag.equals("-v")) {
                cfg.verbose = true;
                i++;
            } else if (flag.equals("-NS") || flag.equals("-ND") ||
                    flag.equals("-H") || flag.equals("-L") ||
                    flag.equals("-G") || flag.equals("-M")) {
                if (i + 1 >= args.length) {
                    die("Missing value for flag " + flag);
                }
                String val = args[i + 1];
                int ival;
                try {
                    ival = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    die("Invalid integer for " + flag + ": " + val);
                    return null;
                }
                switch (flag) {
                    case "-NS":
                        cfg.NS = ival;
                        break;
                    case "-ND":
                        cfg.ND = ival;
                        break;
                    case "-H":
                        cfg.H = ival;
                        break;
                    case "-L":
                        cfg.L = ival;
                        break;
                    case "-G":
                        cfg.G = ival;
                        break;
                    case "-M":
                        cfg.M = ival;
                        break;
                }
                i += 2;
            } else {
                die("Unknown flag: " + flag);
            }
        }

        // Check required flags were set
        if (cfg.NS == -1 || cfg.ND == -1 || cfg.H == -1 ||
                cfg.L == -1 || cfg.G == -1 || cfg.M == -1) {
            die("Missing one or more required flags: -NS -ND -H -L -G -M");
        }

        return cfg;
    }

    private static void validateConfig(Config cfg) {
        if (cfg.NS <= 0 || cfg.ND <= 0 || cfg.H <= 0 || cfg.L <= 0) {
            die("Error: NS, ND, H, and L must all be > 0.");
        }
        if (cfg.NS == 1 || cfg.ND == 1) {
            die("Error: NS and ND must both be > 1.");
        }
        if (cfg.H < cfg.L) {
            die("Error: H must be >= L.");
        }
        if (cfg.G < 1) {
            die("Error: G must be >= 1.");
        }
        if (cfg.M < 0) {
            die("Error: M must be >= 0.");
        }
    }

    // Init table based on config obeject given
    private static void initTables(Config cfg) {
        int L = cfg.L;
        int ND = cfg.ND;
        wins = new int[L][L][ND + 1];
        losses = new int[L][L][ND + 1];
    }

    private static void train(Config cfg) {
        for (int g = 0; g < cfg.G; g++) {
            playOneGame(cfg);
        }
    }

    private static int playOneGame(Config cfg) {
        int[] scores = new int[] { 0, 0 };
        int currentPlayer = 0;
        // Record of (player, x, y, d)
        int capacity = 64;
        int[] players = new int[capacity];
        int[] xs = new int[capacity];
        int[] ys = new int[capacity];
        int[] ds = new int[capacity];
        int steps = 0;

        if (cfg.verbose) {
            System.out.println("=== New Game ===");
        }

        int winner = -1;
        while (true) {
            int x = scores[currentPlayer];
            int y = scores[1 - currentPlayer];

            if (x >= cfg.L || y >= cfg.L) {
                // Shouldn't ever reach this case
                break;
            }

            int d = chooseNumDice(x, y, cfg);

            // Store current state
            if (steps == capacity) {
                capacity *= 2;
                players = grow(players, capacity);
                xs = grow(xs, capacity);
                ys = grow(ys, capacity);
                ds = grow(ds, capacity);
            }
            players[steps] = currentPlayer;
            xs[steps] = x;
            ys[steps] = y;
            ds[steps] = d;
            steps++;

            // Roll the dice
            int rollSum = 0;
            for (int i = 0; i < d; i++) {
                rollSum += 1 + RNG.nextInt(cfg.NS);
            }
            int newScore = x + rollSum;

            if (cfg.verbose) {
                String playerLabel = (currentPlayer == 0) ? "A" : "B";
                System.out.println(playerLabel + " at (" + x + "," + y + ") rolls " +
                        d + " dice, sum=" + rollSum + " -> new score=" + newScore);
            }

            // Check bust or win
            if (newScore > cfg.H) {
                // current player busts
                winner = 1 - currentPlayer;
                if (cfg.verbose) {
                    String loserLabel = (currentPlayer == 0) ? "A" : "B";
                    String winnerLabel = (winner == 0) ? "A" : "B";
                    System.out.println(loserLabel + " busts (> " + cfg.H + "). " +
                            winnerLabel + " wins.");
                    System.out.println("=== End Game ===\n");
                }
                break;
            } else if (newScore >= cfg.L && newScore <= cfg.H) {
                // current player wins
                winner = currentPlayer;
                if (cfg.verbose) {
                    String winnerLabel = (winner == 0) ? "A" : "B";
                    System.out.println(winnerLabel + " hits [" + cfg.L + "," + cfg.H + "]. " +
                            winnerLabel + " wins.");
                    System.out.println("=== End Game ===\n");
                }
                break;
                // switch turns
            } else {
                scores[currentPlayer] = newScore;
                currentPlayer = 1 - currentPlayer;
            }
        }

        // Update tables
        for (int i = 0; i < steps; i++) {
            int player = players[i];
            int x = xs[i];
            int y = ys[i];
            int d = ds[i];

            // Only update if indices are within table
            if (x >= 0 && x < cfg.L && y >= 0 && y < cfg.L && d >= 1 && d <= cfg.ND) {
                if (player == winner) {
                    wins[x][y][d]++;
                    if (cfg.verbose) {
                        System.out.println("Update: Wins[" + x + "," + y + "," + d + "] -> " +
                                wins[x][y][d]);
                    }
                } else {
                    losses[x][y][d]++;
                    if (cfg.verbose) {
                        System.out.println("Update: Losses[" + x + "," + y + "," + d + "] -> " +
                                losses[x][y][d]);
                    }
                }
            }
        }

        return winner;
    }

    // resize the array
    private static int[] grow(int[] arr, int newCap) {
        int[] out = new int[newCap];
        System.arraycopy(arr, 0, out, 0, arr.length);
        return out;
    }

    /**
     * Chooses the optimal number of dice using our equation
     * (Wins(A,B,d)/(Wins(A,B,d)+Losses(A,B,d)))
     * 
     * @param currentPlayerScore
     */

    private static int chooseNumDice(int currentPlayerScore, int otherPlayerScore, Config cfg) {
        int ND = cfg.ND;

        double[] Wd = new double[ND + 1];
        int T = 0;

        for (int numDice = 1; numDice <= ND; numDice++) {
            int totalWins = wins[currentPlayerScore][otherPlayerScore][numDice];
            int totalLosses = losses[currentPlayerScore][otherPlayerScore][numDice];
            int visits = totalWins + totalLosses;
            T += visits;
            if (visits > 0) {
                Wd[numDice] = ((double) totalWins) / visits;
            } else {
                Wd[numDice] = 0.0;
            }
        }

        // No visits: uniform random
        if (T == 0) {
            return 1 + RNG.nextInt(ND);
        }

        // Find best d (argmax Wd)
        double maxW = Double.NEGATIVE_INFINITY;
        int countBest = 0;
        for (int d = 1; d <= ND; d++) {
            if (Wd[d] > maxW) {
                maxW = Wd[d];
                countBest = 1;
            } else if (Math.abs(Wd[d] - maxW) < 1e-12) {
                countBest++;
            }
        }

        // settle ties randomly
        int chosenRank = (countBest == 0) ? 1 : (1 + RNG.nextInt(countBest));
        int best = -1;
        int seen = 0;
        for (int d = 1; d <= ND; d++) {
            if (Math.abs(Wd[d] - maxW) < 1e-12) {
                seen++;
                if (seen == chosenRank) {
                    best = d;
                    break;
                }
            }
        }

        if (cfg.M == 0) {
            // Pure exploitation if we have visited
            return best;
        }

        double Tdouble = (double) T;
        double Wb = Wd[best];
        double M = (double) cfg.M;

        // P_best = (T*Wb + M) / (T*Wb + M*ND)
        double Pbest = (Tdouble * Wb + M) / (Tdouble * Wb + M * ND);

        // Probabilities for each d
        double[] probs = new double[ND + 1];
        probs[best] = Pbest;

        // Others
        double s = 0.0;
        for (int d = 1; d <= ND; d++) {
            if (d == best)
                continue;
            s += Wd[d];
        }

        double denomOthers = s * Tdouble + (ND - 1) * M;
        if (denomOthers > 0.0) {
            for (int d = 1; d <= ND; d++) {
                if (d == best)
                    continue;
                probs[d] = (1.0 - Pbest) * (Tdouble * Wd[d] + M) / denomOthers;
            }
        } else {
            // Fallback: spread remaining probability evenly among non-best
            int othersCount = ND - 1;
            if (othersCount > 0) {
                double uniform = (1.0 - Pbest) / othersCount;
                for (int d = 1; d <= ND; d++) {
                    if (d == best)
                        continue;
                    probs[d] = uniform;
                }
            }
        }

        // Normalize (numeric safety)
        double sum = 0.0;
        for (int d = 1; d <= ND; d++) {
            sum += probs[d];
        }

        for (int d = 1; d <= ND; d++) {
            probs[d] /= sum;
        }

        // Sample according to probs
        double r = RNG.nextDouble();
        double cum = 0.0;
        for (int d = 1; d <= ND; d++) {
            cum += probs[d];
            if (r <= cum) {
                return d;
            }
        }
        // In case of rounding error
        return ND;
    }

    // Compute best dice and win prob for a given state based only on wins/losses
    private static BestResult computeBestForState(int currentPlayerScore, int otherPlayerScore, Config cfg) {
        int ND = cfg.ND;
        int L = cfg.L;
        if (currentPlayerScore < 0 || currentPlayerScore >= L || otherPlayerScore < 0 || otherPlayerScore >= L) {
            return new BestResult(false, -1, 0.0);
        }

        double[] Wd = new double[ND + 1];
        int totalVisits = 0;

        for (int d = 1; d <= ND; d++) {
            int w = wins[currentPlayerScore][otherPlayerScore][d];
            int l = losses[currentPlayerScore][otherPlayerScore][d];
            int visits = w + l;
            totalVisits += visits;
            if (visits > 0) {
                Wd[d] = ((double) w) / visits;
            } else {
                Wd[d] = 0.0;
            }
        }

        if (totalVisits == 0) {
            return new BestResult(false, -1, 0.0);
        }

        double maxW = Double.NEGATIVE_INFINITY;
        for (int d = 1; d <= ND; d++) {
            if (Wd[d] > maxW) {
                maxW = Wd[d];
            }
        }

        // Choose smallest d achieving maxW for deterministic printing
        int bestD = 1;
        for (int d = 1; d <= ND; d++) {
            if (Math.abs(Wd[d] - maxW) < 1e-12) {
                bestD = d;
                break;
            }
        }
        return new BestResult(true, bestD, Wd[bestD]);
    }

    // print policy table and write to output file
    private static void printPolicyTable(Config cfg) {
        int L = cfg.L;
        int ND = cfg.ND;

        System.out.println();
        System.out.println("Final policy table:");
        System.out.println("Each cell: best_dice:winning_probability (for the player to move)");
        System.out.println("Rows = current player's score X (0.." + (L - 1) +
                "), Columns = opponent's score Y (0.." + (L - 1) + ")\n");

        // Header
        StringBuilder sb = new StringBuilder();
        sb.append("X\\Y");
        for (int y = 0; y < L; y++) {
            sb.append("\t").append(y);
        }
        System.out.println(sb.toString());

        ArrayList<String> lineList = new ArrayList<>();
        // Rows
        for (int x = 0; x < L; x++) {
            sb.setLength(0);
            sb.append(x);
            for (int y = 0; y < L; y++) {
                BestResult br = computeBestForState(x, y, cfg);
                if (!br.exists) {
                    sb.append("\t").append("n/a");
                } else {
                    sb.append("\t")
                            .append(br.bestD)
                            .append(":")
                            .append(String.format("%.3f", br.bestProb));
                }
            }
            System.out.println(sb.toString());
            lineList.add(sb.toString() + "\n");

        }
        String filename = "policy_output.txt";

        try (FileWriter fw = new FileWriter(filename)) {
            for (String line : lineList) {
                System.out.println(line);
                fw.write(line);
            }
            System.out.println("\nOutput also written to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing to " + filename + ": " + e.getMessage());
        }

    }

    // Small helper structure for best policy lookup
    private static class BestResult {
        boolean exists;
        int bestD;
        double bestProb;

        BestResult(boolean exists, int bestD, double bestProb) {
            this.exists = exists;
            this.bestD = bestD;
            this.bestProb = bestProb;
        }
    }

    private static void die(String msg) {
        System.err.println(msg);
        System.exit(1);
    }
}
