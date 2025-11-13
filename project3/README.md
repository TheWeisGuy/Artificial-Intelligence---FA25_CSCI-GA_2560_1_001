I tested this on ubuntu linux using jdk 11.0.27

I used chat gpt for much of the main logic in the markov process, as well as for debugging

To compile, run javac *.java

To run, run java Markov and input the required args

Order does not matter for these:

-df : a float discount factor [0, 1] to use on future rewards, defaults to 1.0 if not set
-max : maximize values as rewards, defaults to false which minimizes values as costs
-tol : a float tolerance for exiting value iteration, defaults to 0.001 (matches test outputs)
-iter : an integer that indicates a cutoff for value iteration, defaults to 150

inputFile.txt - this must come last

It will out put the optimal policy and then the values for each node into the terminal