import java.util.HashSet;
import java.util.Set;

public class Spiele {
    // Felder 0 bis 8, zeilenweise von links oben nach rechts unten.
    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    static class Result {
        int value;
        int move;
        long nodes;
        long cutoffs;

        Result(int value, int move) {
            this.value = value;
            this.move = move;
        }

        @Override
        public String toString() {
            return "Wert=" + value + ", Feld=" + move
                + ", Knoten=" + nodes + ", Abbrueche=" + cutoffs;
        }
    }

    // null: Spiel laeuft noch. Bewertung immer aus Sicht von X.
    static Integer utility(char[] board) {
        for (int[] line : LINES) {
            char c = board[line[0]];
            if (c != '.' && c == board[line[1]] && c == board[line[2]]) {
                return c == 'X' ? 1 : -1;
            }
        }
        for (char c : board) {
            if (c == '.') return null;
        }
        return 0;
    }

    static class Search {
        final boolean pruning;
        long nodes;
        long cutoffs;

        Search(boolean pruning) {
            this.pruning = pruning;
        }

        Result visit(char[] board, char player, int alpha, int beta) {
            nodes++;
            Integer terminalValue = utility(board);
            if (terminalValue != null) return new Result(terminalValue, -1);

            int value = player == 'X' ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            int move = -1;
            char nextPlayer = player == 'X' ? 'O' : 'X';

            for (int i = 0; i < 9; i++) {
                if (board[i] != '.') continue;
                char[] child = board.clone();
                child[i] = player;
                int childValue = visit(child, nextPlayer, alpha, beta).value;

                // Bei Gleichstand bleibt der erste Zug gewaehlt.
                boolean better = player == 'X' ? childValue > value : childValue < value;
                if (move == -1 || better) {
                    value = childValue;
                    move = i;
                }

                if (pruning) {
                    if (player == 'X') alpha = Math.max(alpha, value);
                    else beta = Math.min(beta, value);
                    if (alpha >= beta) {
                        cutoffs++;
                        break;
                    }
                }
            }
            return new Result(value, move);
        }
    }

    // Erwartet eine legale Stellung und den richtigen Spieler am Zug.
    static Result search(char[] board, char player, boolean pruning) {
        Search search = new Search(pruning);
        Result result = search.visit(board, player, Integer.MIN_VALUE, Integer.MAX_VALUE);
        result.nodes = search.nodes;
        result.cutoffs = search.cutoffs;
        return result;
    }

    // Rueckgabe: X1, X2, O1, O2, Eval (Games.04).
    static int[] counts(char[] board) {
        int x1 = 0, x2 = 0, o1 = 0, o2 = 0;
        for (int[] line : LINES) {
            int x = 0, o = 0;
            for (int i : line) {
                if (board[i] == 'X') x++;
                if (board[i] == 'O') o++;
            }
            if (o == 0) {
                if (x == 1) x1++;
                if (x == 2) x2++;
            }
            if (x == 0) {
                if (o == 1) o1++;
                if (o == 2) o2++;
            }
        }
        return new int[] {x1, x2, o1, o2, 3 * x2 + x1 - 3 * o2 - o1};
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void walk(char[] board, char player, Set<String> seen) {
        String key = new String(board);
        if (!seen.add(key)) return;
        Result minimax = search(board, player, false);
        Result alphaBeta = search(board, player, true);
        check(minimax.value == alphaBeta.value, "Unterschiedlicher Wert: " + key);
        check(minimax.move == alphaBeta.move, "Unterschiedlicher Zug: " + key);
        check(alphaBeta.nodes <= minimax.nodes, "Mehr Knoten mit Alpha-Beta: " + key);
        if (utility(board) != null) return;

        for (int i = 0; i < 9; i++) {
            if (board[i] == '.') {
                char[] child = board.clone();
                child[i] = player;
                walk(child, player == 'X' ? 'O' : 'X', seen);
            }
        }
    }

    static void verify() {
        Set<String> seen = new HashSet<>();
        walk(".........".toCharArray(), 'X', seen);
        check(seen.size() == 5478, "Falsche Anzahl an Stellungen");
        Result empty = search(".........".toCharArray(), 'X', false);
        check(empty.value == 0 && empty.nodes == 549946, "Leeres Brett: Minimax");
        check(search(".........".toCharArray(), 'X', true).nodes == 18297, "Leeres Brett: Alpha-Beta");
        check(search("X...O...X".toCharArray(), 'O', false).nodes == 1053, "Zwischenstand: Minimax");
        check(search("X...O...X".toCharArray(), 'O', true).nodes == 318, "Zwischenstand: Alpha-Beta");
        check(search("XX.OO....".toCharArray(), 'X', false).move == 2, "Gewinnzug fehlt");
        check(search("XX..O....".toCharArray(), 'O', false).move == 2, "Blockierzug fehlt");
        System.out.println("Pruefung bestanden: " + seen.size()
            + " erreichbare Zustaende, identische Werte und Zuege.");
    }

    public static void main(String[] args) {
        String[] names = {"Leeres Brett", "Zwischenstand"};
        String[] boards = {".........", "X...O...X"};
        char[] players = {'X', 'O'};
        for (int i = 0; i < boards.length; i++) {
            System.out.println(names[i]);
            System.out.println("Minimax: " + search(boards[i].toCharArray(), players[i], false));
            System.out.println("Alpha-Beta: " + search(boards[i].toCharArray(), players[i], true));
        }

        System.out.println("\nBewertungsfunktion (Games.04):");
        String[] examples = {"XXXOO....", "XX.OOOX..", "XOXXOOOXX", "X........", "X...O....", "XX..O...."};
        for (String board : examples) {
            int[] c = counts(board.toCharArray());
            System.out.println(board + " X1=" + c[0] + " X2=" + c[1]
                + " O1=" + c[2] + " O2=" + c[3] + " Eval=" + c[4]
                + " Nutzen=" + utility(board.toCharArray()));
        }
        for (String arg : args) {
            if (arg.equals("--test")) {
                verify();
                break;
            }
        }
    }
}
