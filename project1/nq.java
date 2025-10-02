public class nq {
    public static void main(String args[]) {
        String mode = "";
        int N = 0;
        int seed = 1;
        int restarts = 0;
        int sideways = 0;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-mode":
                    if (i + 1 < args.length) {
                        mode = args[++i].toLowerCase();
                    } else {
                        System.err.println("Error: Missing value for -mode");
                        return;
                    }
                    break;
                case "-N":

                    try {
                        if (i + 1 < args.length) {
                            N = Integer.parseInt(args[++i]);
                        } else {
                            System.err.println("Error: Missing value for -N");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid value for -N: " + args[i]);
                        return;
                    }
                    break;
                case "-seed":
                    try {
                        if (i + 1 < args.length) {
                            seed = Integer.parseInt(args[++i]);
                        } else {
                            System.err.println("Error: Missing value for -seed");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid value for -seed: " + args[i]);
                        return;
                    }
                    break;
                case "-restarts":
                    try {
                        if (i + 1 < args.length) {
                            restarts = Integer.parseInt(args[++i]);
                        } else {
                            System.err.println("Error: Missing value for -restarts");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid value for -restarts: " + args[i]);
                        return;
                    }
                    break;
                case "-sideways":
                    try {
                        if (i + 1 < args.length) {
                            sideways = Integer.parseInt(args[++i]);
                        } else {
                            System.err.println("Error: Missing value for -sideways");
                            return;
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid value for sideways: " + args[i]);
                        return;
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    return;
            }
        }

        // Validate input
        if (N < 4) {
            System.err.println("Error: N must be >= 4");
            return;
        }
        if (!mode.equals("dfs") && !mode.equals("hc")) {
            System.err.println("Error: -mode must be either 'dfs' or 'hc'");
            return;
        }

        if (mode.equals("dfs")) {
            System.out.println("Running DFS with N = " + N);
            NQueensDfs queens = new NQueensDfs(N);
            queens.printQueenArray();
        }

        else if (mode.equals("hc")) {
            System.out.println("Running hill climbing wth N = " + N);
            NQueensHillClimbing queens = new NQueensHillClimbing(N, restarts, sideways, seed);
            System.out.println(queens.getStatus());
            System.out.println(queens.getQueenPosString());
        }
    }
}
