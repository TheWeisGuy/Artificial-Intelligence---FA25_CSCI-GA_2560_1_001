This was tested on Ubuntu 20.04.6 LTS
JDK 11.0.27

I used chatgpt mainly for the BNFToCnf, Solver, and DPLL solver files

To compile, in the command line type: javac *.java
To run, type: java Solver [-v] <ncolors> <input-file>

Arguments: 

[-v] optional verbose flag which will show each step of running DPLL (defaults to false)

<ncolors> Integer for the number of colors to try coloring the map with (Max: 4)

<input-file> A text file containing the graph to use
Each vertex given in the input file should be in the below format:
<Vertex> : [neighbor1,neighbor2, ... , neighborN]
For example: AL : [MS,TN,GA,FL]
Empty lines and lines starting with # will be ignored


This will generate three files:

A file containing bnf expressions parsed from the graph file:
<input-file>.bnf.<ncolors>.dp

A file containing cnf expressions parsed from the given bnf:
<input-file>.<ncolors>.dp

A file containing the map color assignments after running DPLL on the above cnf:
<input-file>.<ncolors>.out

Note: If a map has no solutions, these above files will still be produced, but the color assignments file will indicate there was no solution