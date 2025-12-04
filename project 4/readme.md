This was tested on Windows using JDK 17
Tested also on ubuntu using JDK 11

To compile: javac *.java
To run: java DiceLearning
Flags:

    -ND num dice 
    -NS num sides on the dice
    -H high winning score
    -L low winning score
    -M : similar to alpha, but any non-negative integer (exploitation vs. exploration parameter)
    -G : number of games to train against
    -v [optional]: a verbose mode flag that outputs each episode and the table updates (good for debugging purposes) Leave blank for false

Outputs a txt file called policy_output.txt with the generated policy table

Used chatgpt for debugging and assistance with the assignment
