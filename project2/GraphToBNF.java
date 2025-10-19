import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

public class GraphToBNF {
    private int numColors;

    /**
     * Constructor: If supplied with .txt file, parse automatically
     * 
     * @param txtFile
     * @param numColors number of colors to color the map
     */
    public GraphToBNF(int numColors, String txtFile) {
        this.numColors = numColors;
        this.parseFile(txtFile);
    }

    public GraphToBNF(int numColors) {
        this.numColors = numColors;
        return;
    }

    /**
     * Parses .txt file containing our graph
     * Outputs .out file containing the parsed CNF
     * @output file with the name: ${graphFileName}.txt.bnf${numColors}.dp
     * @param txtFile
     */
    public void parseFile(String txtFile) {

        try {
            // check if any vertices aren't explicitly declared and declare them
            // make an arrayList with all expressions
            BufferedReader reader = new BufferedReader(new FileReader(txtFile));
            String currentLine = reader.readLine();
            HashSet<String> verticesSet = new HashSet<>();
            HashSet<String> declaredVerticesSet = new HashSet<>();
            ArrayList<String> expressions = new ArrayList<>();
            while (currentLine != null) {
                // ignore lines starting with # or empty lines
                if (currentLine.length() == 0) {
                    currentLine = reader.readLine();
                    continue;
                }
                if (currentLine.charAt(0) == '#') {
                    currentLine = reader.readLine();
                    continue;
                }

                // Create a set of all vertices and all explicitly declared vertices
                String[] currentLineSplit = currentLine.trim().split("\\s*:\\s*", 2);
                verticesSet.add(currentLineSplit[0]);
                declaredVerticesSet.add(currentLineSplit[0]);
                String neighbors = currentLineSplit[1];
                neighbors = neighbors.replaceAll("[\\[\\]]", "");
                String[] neighborsList = neighbors.split("\\s*,\\s*");
                for (String vertex : neighborsList) {
                    verticesSet.add(vertex);
                }
                expressions.add(currentLine);
                currentLine = reader.readLine();
            }

            // explicitly declare all inferred vertices
            verticesSet.removeAll(declaredVerticesSet);
            for (String vertex : verticesSet) {
                expressions.add(vertex + " : []\n");
            }

            String outputPath = txtFile + ".bnf" + this.numColors + ".dp";
            FileWriter writer = new FileWriter(outputPath);

            for (String current : expressions) {
                ArrayList<String> parsedExpressions = new ArrayList<>();
                // ignore lines starting with # or empty lines
                if (current.length() == 0) {
                    continue;
                }
                if (current.charAt(0) == '#') {
                    continue;
                }
                // Split the string so we get the vertex and its neighbors
                String[] currentLineSplit = current.trim().split("\\s*:\\s*", 2);
                String currentVertex = currentLineSplit[0];
                String neighbors = currentLineSplit[1];
                neighbors = neighbors.replaceAll("[\\[\\]]", "");
                String[] neighborsList = neighbors.split("\\s*,\\s*");

                // A vertex/state needs at least one color
                String currentVertexString = "";
                for (int i = 0; i < this.numColors; i++) {
                    String unitToAdd = currentVertex;
                    currentVertexString += unitToAdd + "_" + i;

                    if (this.numColors > 1 && i < this.numColors - 1) {
                        currentVertexString += "|";
                    }
                }
                parsedExpressions.add(currentVertexString + "\n");

                // No adjacent same colors for every edge, distinct clause for each color
                if (neighborsList[0] != "") {
                    for (int i = 0; i < this.numColors; i++) {
                        String adjacentString = currentVertex + "_" + i + "=>![";
                        for (int j = 0; j < neighborsList.length; j++) {
                            adjacentString += neighborsList[j] + "_" + i;
                            if (j < neighborsList.length - 1) {
                                adjacentString += "|";
                            }
                        }
                        parsedExpressions.add(adjacentString + "]\n");
                    }
                }
                // Color(WA,R) =>¬[Color(WA,G) v Color(WA,B)]
                // At most one color for each vertex
                for (int i = 0; i < this.numColors; i++) {
                    String newCurrentVertexString = currentVertex + "_" + i + "=>![";
                    for (int j = 0; j < numColors; j++) {
                        if (i != j) {
                            newCurrentVertexString += currentVertex + "_" + j;
                            if (j < numColors - 1 && (j + 1 != i || j + 1 < numColors - 1)) {
                                newCurrentVertexString += "|";
                            }

                        }
                    }
                    parsedExpressions.add(newCurrentVertexString + "]\n");
                }

                // write parsed CNF to output file
                for (String expression : parsedExpressions) {
                    writer.write(expression);
                }
            }
            writer.close();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GraphToBNF test = new GraphToBNF(2, "tiny.txt");

    }
}
