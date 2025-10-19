interface Expr {}

class Var implements Expr {
    String name;
    Var(String n) { name = n; }
    public String toString() { return name; }
}

class Not implements Expr {
    Expr e;
    Not(Expr e) { this.e = e; }
    public String toString() { return "!" + e; }
}

class And implements Expr {
    Expr left, right;
    And(Expr l, Expr r) { left = l; right = r; }
    public String toString() { return "(" + left + " ^ " + right + ")"; }
}

class Or implements Expr {
    Expr left, right;
    Or(Expr l, Expr r) { left = l; right = r; }
    public String toString() { return "(" + left + " v " + right + ")"; }
}

class Impl implements Expr {
    Expr left, right;
    Impl(Expr l, Expr r) { left = l; right = r; }
    public String toString() { return "(" + left + " => " + right + ")"; }
}

class Iff implements Expr {
    Expr left, right;
    Iff(Expr l, Expr r) { left = l; right = r; }
    public String toString() { return "(" + left + " <=> " + right + ")"; }
}
