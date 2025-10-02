This was tested on

Extract all files to one directory
To compile all files cd into the directory and run the following command:
javac *.java

To run code, run the following command with the provided arguments:
java nq -mode <Required, dfs hc> -N <Required, int: for board size> -seed <int: seed for RNG, default = 1> -restarts <int: number of restarts permitted for hc, default = 1> -sideways <int: number of sideways movements permitted for each step in hc, default = 1> 

Output: For dfs just the array of queen positions
For hc the array of queen positions and whether or not it succeeded

Resources consulted:

NQueens using depth first search: https://www.geeksforgeeks.org/dsa/n-queen-problem-backtracking-3/
Hill climbing algorithm: https://www.geeksforgeeks.org/artificial-intelligence/introduction-hill-climbing-artificial-intelligence/

I used ChatGPT mostly in NQueensHillClimbing largely for debugging purposes