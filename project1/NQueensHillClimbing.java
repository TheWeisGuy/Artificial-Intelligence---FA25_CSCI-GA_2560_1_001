import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class NQueensHillClimbing {

    private int[] queenPos;
    private HashSet<String> visitedList;
    private int error;
    private int sidewaysMoveCap;
    private int restartCap;
    private String status;
    private int seed;
    private Random rand;

    /**
     * @param boardSize Size of the board
     * @param restartCap How many times we are allowed to restart
     * @param sidewaysMoveCap How many times we are allowed to move sideways per step
     * @param seed Seed for random number generation
     */
    public NQueensHillClimbing(int boardSize, int restartCap, int sidewaysMoveCap, int seed) {
        this.visitedList = new HashSet<>();
        this.restartCap = restartCap;
        this.sidewaysMoveCap = sidewaysMoveCap;
        this.status = "";
        this.seed = seed;
        this.rand = new Random(seed);
        ArrayList<Integer> initialState = new ArrayList<>();
        for (int i = 0; i < boardSize; i++) {
            initialState.add(i);
        }
        Collections.shuffle(initialState, new Random(seed));
        this.queenPos = initialState.stream().mapToInt(Integer::valueOf).toArray();
        this.visitedList.add(stateKey(this.queenPos));
        this.error = this.errorFunction(this.queenPos);

        this.solve();
    }

    /**
     * @param queens Current state of the board
     * @return Board state as a string
     * Java HashSets do not properly recognize identical arrays
     * Must be converted to strings first
     */
    private String stateKey(int[] queens) {
        StringBuilder sb = new StringBuilder();
        for (int v : queens)
            sb.append(v).append(",");
        return sb.toString();
    }

    /**
     * @param queens Board state to check
     * @return Number of queens attacking each other
     */
    private int errorFunction(int[] queens) {
        int totalAttacked = 0;
        for (int i = 0; i < queens.length; i++) {
            if (!isSafe(i, queens[i], queens)) {
                totalAttacked++;
            }
        }
        return totalAttacked;
    }

    /**
     * @param rank Rank of the square we want to check
     * @param file File of the square we want to check
     * @param queens Board state we use to check
     * @return Whether or not the square is under attack
     */
    private boolean isSafe(int rank, int file, int[] queens) {
        for (int i = 0; i < rank; i++) {

            // check diagonal only
            if (Math.abs(rank - i) == Math.abs(file - queens[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return ArrayList of all neighbors of the current state
     */
    private ArrayList<int[]> getNeighbors() {
        ArrayList<int[]> neighbors = new ArrayList<int[]>();
        int n = queenPos.length;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // copy state
                int[] neighbor = queenPos.clone();
                // swap queens at i and j
                int temp = neighbor[i];
                neighbor[i] = neighbor[j];
                neighbor[j] = temp;
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Solves the board
     */
    private void solve() {
        boolean move = true;
        int sidewaysMoves = 0;
        int timesRestarted = 0;
        while (move && error > 0 && timesRestarted < this.restartCap) {
            move = false;

            ArrayList<int[]> neighbors = this.getNeighbors();
            int bestError = this.error;
            int[] bestState = this.queenPos;
            ArrayList<int[]> bestNeighbors = new ArrayList<>();

            // find the best state and error value
            for (int[] neighbor : neighbors) {
                int neighborError = this.errorFunction(neighbor);

                if (neighborError < bestError) {
                    bestError = neighborError;
                    bestState = neighbor;
                }
            }
            // collect potential sideways moves
            for (int[] neighbor : neighbors) {
                int neighborError = this.errorFunction(neighbor);
                if (neighborError == bestError) {
                    bestNeighbors.add(neighbor);
                }
            }
            // move to better state
            if (bestError < this.error) {
                this.queenPos = bestState;
                this.error = bestError;

                this.visitedList.add(stateKey(queenPos));
                move = true;
                sidewaysMoves = 0;
            }

            // Sideways move
            else if (bestError == error &&
                    sidewaysMoves < this.sidewaysMoveCap
                    && bestNeighbors.size() > 0) {

                this.queenPos = bestNeighbors.get(0).clone();
                this.error = bestError;
                sidewaysMoves++;
                move = true;
            }

            // if we need to restart
            else if (!move) {
                ArrayList<Integer> initialState = new ArrayList<>();
                for (int i = 0; i < queenPos.length; i++) {
                    initialState.add(i);
                }
                this.seed = rand.nextInt();
                Collections.shuffle(initialState, new Random(this.seed));
                this.queenPos = initialState.stream().mapToInt(Integer::valueOf).toArray();
                timesRestarted++;
                sidewaysMoves = 0;
                this.visitedList.clear();
                this.error = this.errorFunction(this.queenPos);
                move = true;
            }
        }

        if (this.error == 0) {
            this.status = "Found valid solution";
        } else {
            this.status = ("Stuck at error = " + this.error);
        }
    }

    /**
     * @return String of the current board state
     */
    public String getQueenPosString() {
        return Arrays.toString(this.queenPos);
    }

    /**
     * @return String of the status given by solve
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Prints the board as a grid
     */
    public void printBoard() {
        int[][] board = new int[this.queenPos.length][this.queenPos.length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                if (this.queenPos[i] == j) {
                    board[i][j] = 1;
                } else {
                    board[i][j] = 0;
                }
            }
        }
        for (int i = 0; i < board.length; i++) {
            System.out.println(Arrays.toString(board[i]));
        }
    }

    /**
     * Should not need to use this
     * Use nq.java instead
     * @param args[0] Int for the board size
     * @param args[1] Int for the number of restarts
     * @param args[2] Int for the number of permitted sideways moves per step
     */
    public static void main(String args[]) {
        Instant startTimeObject = Instant.now();
        Long startTime = startTimeObject.toEpochMilli();

        int boardSize = Integer.parseInt(args[0]);
        int restartCap = 5;
        int sidewaysMoveCap = 5;
        if (args.length >= 2) {
            restartCap = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            sidewaysMoveCap = Integer.parseInt(args[2]);
        }

        NQueensHillClimbing queens = new NQueensHillClimbing(boardSize,
                restartCap,
                sidewaysMoveCap, 10);
        queens.printBoard();

        Instant endTimeObject = Instant.now();
        Long endTime = (endTimeObject.toEpochMilli() - startTime);

        System.out.println("Time taken: " + endTime.toString() + " ms");
        System.out.println(queens.getStatus());
    }
}
