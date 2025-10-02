import java.util.Arrays;
import java.time.Instant;

public class NQueensDfs {

    private int[][] board;

    /**
     * constructor to initialize the board object and solve it
     * 
     * @param boardSize Length and width of the board
     */
    public NQueensDfs(int boardSize) {
        this.board = new int[boardSize][boardSize];
        this.placeQueenDfs(0);
    }

    /**
     * @param rank int: The rank number of the square we're checking
     * @param file int: The file number of the square we're checking
     * @return True if the square is safe, else false
     **/
    private boolean isSafe(int rank, int file) {
        // check current file
        for (int i = 0; i < rank; i++) {
            if (this.board[i][file] == 1) {
                return false;
            }
        }

        // Check upper diagonal on left side
        for (int i = rank - 1, j = file - 1; i >= 0 && j >= 0; i--, j--) {
            if (this.board[i][j] == 1) {
                return false;
            }
        }

        // Check upper diagonal on right side
        for (int i = rank - 1, j = file + 1; j < this.board.length && i >= 0; i--, j++) {
            if (this.board[i][j] == 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Solves the board
     * 
     * @param rank Current rank we are attempting to place a queen on
     * @return True if a queen was able to be placed, else false
     */
    private boolean placeQueenDfs(int rank) {
        if (rank == this.board.length) {
            return true;
        }

        for (int i = 0; i < this.board.length; i++) {
            if (isSafe(rank, i)) {
                this.board[rank][i] = 1;

                // recursively call placeQueen and see if there is a valid place for the next
                // queen
                if (placeQueenDfs(rank + 1)) {
                    return true;
                }
                this.board[rank][i] = 0;
            }
        }
        return false;
    }

    /*
     * Print the board neatly as a grid
     */
    public void printBoard() {
        for (int i = 0; i < this.board.length; i++) {
            System.out.println(Arrays.toString(this.board[i]));
        }
    }

    /**
     * Prints the rank of each queen
     */
    public void printQueenArray() {
        int[] queensPos = new int[board.length];
        for (int i = 0; i < this.board.length; i++) {
            for (int j = 0; j < this.board.length; j++) {
                if (this.board[i][j] == 1) {
                    queensPos[i] = j;
                }
            }
        }
        System.out.println(Arrays.toString(queensPos));
    }

    /**
     * Should not need to use this
     * Use nq.java instead
     * Create and solve a board
     * Prints the solved board and how long it takes in seconds
     * @param args[0] Int for the dimensions of the board
     */
    public static void main(String args[]) {
        Instant startTimeObject = Instant.now();
        Long startTime = startTimeObject.toEpochMilli();

        NQueensDfs queens = new NQueensDfs(Integer.parseInt(args[0]));
        queens.printBoard();

        Instant endTimeObject = Instant.now();
        Long endTime = (endTimeObject.toEpochMilli() - startTime) / 1000;

        System.out.println("Time taken: " + endTime.toString() + " seconds");
    }
}
