package sc.player2022.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sc.player.IGameHandler;
import sc.api.plugins.IGameState;
import sc.api.plugins.ITeam;
import sc.api.plugins.Team;
import sc.plugin2022.GameState;
import sc.plugin2022.Move;
import sc.plugin2022.Piece;
import sc.plugin2022.PieceType;
import sc.plugin2022.Vector;
import sc.shared.GameResult;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Logic implements IGameHandler {
  private static final Logger log = LoggerFactory.getLogger(Logic.class);
  // Suchtiefe
  /**
   * Die Suchtiefe gibt an wieviele Ebenen man in den MinMax-Baum hineingeht.
   * Ist die Suchtiefe zu hoch (dauert länger als zwei Sekunden), hat man verloren. 
   */   
   private static final int DEPTH = 3; //TODO: richtige Suchtiefe soll automatisch ermittelt werden

  //#region Werte für eine Bewertung einer bestimmten Spielsituation
  /* Erklärung:
   * Möchte man ändern, wie die evaluate Methode eine bestimmte Spielsituation bewertet, 
   * ändert man lediglich die folgenden Werte.   
   */
  // Werte  der Spielfiguren:
  private static final int ROBBE_VALUE = 1;
  private static final int SEESTERN_VALUE = 1;
  private static final int MOEWE_VALUE = 1;
  private static final int HERZMUSCHEL_VALUE = 1;
  /* Siegesmöglichkeiten:
   *  1. Zwei oder mehr Siegpunkte 
   *  2. Nach 31 Zügen mehr Siegpunkte als der Gegner
   */
  //Werte der Siegpunkte:
  /** Wert eines Bernsteins bis Zug 20 */
  private static final int AMBER_VALUE1 = 100;
  /** Wert eines Bernsteins ab Zug 21 */
  private static final int AMBER_VALUE2 = 100;
  //#endregion

  // Variablen zu den aktuellen Spieldaten
  /** gameState: Speichert alle Informationen zur momentanen Runde. */
  private GameState gameState;
  /** ambersMax, ambersMin: Speichert die aktuellen Bernsteine der beiden Spieler. */  
  private int ambersMax, ambersMin;
  
  // Variablen zu Team Daten
  /** teamMax, teamMin: Speichert, inwelchen Teams die KI und der Gegner sind.
   *  Die KI ist dabei immer im Team Max und der Gegner immer im Team Min. */
  private ITeam teamMax, teamMin;
  /** maxIsStarting: Speichert, ob die KI beginnt oder ob der Gegner beginnt.
   *  (Die KI ist immer Spieler Max) */
  private boolean maxIsStarting;

  public void onGameOver(GameResult data) {
    log.info("Das Spiel ist beendet, Ergebnis: {}", data);
  }

  private int onlyOnce;
  /**
   * Die calculateMove Methode wird ausgeführt wenn die KI am Zug ist.
   * @return einen Zug, den die KI im Spiel ausführen wird  
   */
  @Override
  public Move calculateMove() {
    Zahl = 0;
    long startTime = System.currentTimeMillis();
    log.info("Es wurde ein Zug von {} angefordert.", gameState.getCurrentTeam());

    if (onlyOnce == 0) {
      maxIsStarting = (gameState.getCurrentTeam() == gameState.getStartTeam() ? true : false);
      teamMax = (maxIsStarting ? Team.ONE : Team.TWO);
      teamMin = (maxIsStarting ? Team.TWO : Team.ONE); 
      ambersMax = gameState.getPointsForTeam(teamMax);
      ambersMin = gameState.getPointsForTeam(teamMin);
      onlyOnce = 1;
    }

    Move move = findBestMove(gameState); // die Variable move soll der best mögliche Zug sein
    log.info("Sende {} nach {}ms.", move, System.currentTimeMillis() - startTime);
    
    return move; // den besten Zug, den die KI zur Verfügung hat
  }

  @Override
  public void onUpdate(IGameState gameState) {
    this.gameState = (GameState) gameState;
    log.info("Zug: {} Dran: {}", gameState.getTurn(), gameState.getCurrentTeam());
  }

  @Override
  public void onError(String error) {
    log.warn("Fehler: {}", error);
  }

  /**
   * Die Methode findBestMove generiert alle möglichen Züge der KI (Erinnerung:
   * Die KI ist immer Spieler Max). Die Methode ruft dann für jeden möglichen 
   * Zug die evaluatePosition Methode auf, um den besten Zug herauszufinden.
   * @param gameState
   * @return den besten möglichen Zug
   */
  public Move findBestMove(GameState gameState) {
    Move bestMove; // speichert den besten Zug, den die KI zur Verfügung hat 
    int bestMoveScore; // speichert die Bewertung vom besten Zug

    ArrayList<GameState> possibleGameStates = new ArrayList<GameState>(); // speichert die möglichen GameStates 
    List<Move> moves = gameState.getPossibleMoves(); // speichert alle möglichen Züge
    
    for (Move move : moves) { // generiere alle möglichen Züge
      GameState clonedGameState = gameState.clone();
      clonedGameState.performMove(move);
      possibleGameStates.add(clonedGameState); 
    }

    bestMove = moves.get(0); // initialisiere bestMove zum ersten Zug in der moves-Liste
    bestMoveScore = 0;

    evaluatePosition(possibleGameStates.get(0), Integer.MIN_VALUE, Integer.MAX_VALUE, DEPTH, false);


    if (gameState.getTurn() > 1) { // Mache einen zufälligen Zug, wenn das Spiel noch in der ersten Runde ist
      for (int i = 0; i < possibleGameStates.size(); i++) {
        /*
         * rufe die evaluatePosition Methode für jeden möglichen GameState auf.
         * Wenn die Bewertung besser als die vorherige ist, ändere bestMove und
         * bestMoveScore zu den neuen Zug und zu der besseren Bewertung.
         */
        int j = evaluatePosition(possibleGameStates.get(i), Integer.MIN_VALUE, Integer.MAX_VALUE, DEPTH, false);
        if (j >= bestMoveScore) {
          bestMove = moves.get(i);
          bestMoveScore = j;
        }
      }
    } else {
      Random generator = new Random();
      int index = generator.nextInt(moves.size());
      bestMove = moves.get(index);
    }

    return bestMove;
  }

  /**
   * Die evaluatePosition Methode errechnet eine Zahl, welche beschreibt, wie vorteilhaft ein bestimmter
   * GameState für den Spieler Max ist. Die Methode ist Rekursiv und verringert die Suchtiefe jedesmal um 1,
   * wenn sie sich selbst aufruft. Wenn die Suchtiefe 0 erreicht ist, gibt die Methode das Ergebenis des
   * Durchlaufens der evaluate Methode in einem GameState zurück. Wenn die Suchtiefe nicht 0 ist, generiert
   * die Methode alle möglichen Züge aus der Position des spezifischen Spielers und führt dann die
   * evaluatePostion Methode für jeden vom GameState generierten möglichen Zug aus.
   * @param gameState
   * @param alpha
   * @param beta
   * @param depth
   * @param isPlayerMax
   * @return einen Integer, welcher beschreibt, wie gut ein spezifischer GameState ist -
   * ein höherer Wert entspricht dabei einen besseren GameState für den Spieler Max
   */
  public int evaluatePosition(GameState gameState, int alpha, int beta, int depth, boolean isPlayerMax) {
    /*
     * Wenn die Suchtiefe auf 0 reduziert wurde, gibt die Methode
     * evaluatePostion das Ergebnis der evaluate Methode zurück.
     */
    if (depth == 0) {
      int evaluation = evaluate(gameState);
      return evaluation;
    }
    if (isPlayerMax == false) { // Spieler Min
      List<Move> moves = gameState.getPossibleMoves(); // speichert alle möglichen Züge in einer gegebenen Position
      int newBeta = beta;
      /*
       * Diese for-Schleife geht alle möglichen Züge durch und ruft die evaluatePosition Methode
       * für jeden dieser Züge auf und wechselt den Spieler(Bool). Alpha-Beta Pruning wird benutzt
       * um offensichtlich schlechte Züge zu entfernen. Diese werden durch die Variablen Alpha
       * und Beta bestimmt. Alle Züge, inwelchen Beta, welches die Bewertung des minimierenden
       * Spielers ist, kleiner oder gleich zu Alpha ist, werden verworfen.
       */
      for (Move move : moves) {
        GameState successorGameState = gameState.clone();
        successorGameState.performMove(move);
        newBeta = Math.min(newBeta, evaluatePosition(successorGameState, alpha, beta, depth -1, !isPlayerMax));
        if (newBeta <= alpha) break;
      }
      return newBeta; // gibt die höchste Bewertung der möglichen Züge zurück
    } else { // Spieler Max
      List<Move> moves = gameState.getPossibleMoves();
      int newAlpha = alpha;
      /*
       * Diese for-Schleife geht alle möglichen Züge durch und errechnet 
       * einen neuen Wert für Alpha, wenn der successorGameState höher bewertet
       * wird, als der aktuelle höchste Wert, welcher in Alpha gespeichert ist.
       */
      for (Move move : moves) {
        GameState successorGameState =  gameState.clone();
        successorGameState.performMove(move);
        newAlpha = Math.max(newAlpha, evaluatePosition(successorGameState, alpha, beta, depth -1, !isPlayerMax));
        if (beta <= newAlpha) break;
      }
      return newAlpha; // gibt die höchste Bewertung der möglichen Züge zurück
    }
  }

  /**
   * Die Methode evaluate rechnet einen Wert aus, der beschreibt, wie vorteilhaft ein GameState
   * für den maximierenden Spieler ist (In diesem Zug). Die Methode durchläuft dabei einmal das
   * ganze Spielbrett. Jede Figur hat einen bestimmten Wert. Der summierte Wert aller Figuren des 
   * Min Spielers wird dabei vom summierten Wert des Max Spielers abgezogen. (scoreMax - scoreMin) 
   * @param gameState
   * @return einen Integer, der representiert, wie gut der Max Spieler auf dem Spielbrett steht,
   * aber nicht, ob der Spieler auch in der nächsten Runde vorteilhaft auf dem Spielbrett steht 
   */
  public int evaluate(GameState gameState) {
    
    int scoreMax = 0, scoreMin = 0;
    int ambersMax = gameState.getPointsForTeam(teamMax);
    int ambersMin = gameState.getPointsForTeam(teamMin);



    if (ambersMax > 1) return Integer.MAX_VALUE;
    if (ambersMin > 1) return Integer.MIN_VALUE;

    if (ambersMax > this.ambersMax) scoreMax += (gameState.getTurn() > 20 ? AMBER_VALUE1 : AMBER_VALUE2);
    if (ambersMin > this.ambersMin) scoreMin += (gameState.getTurn() > 20 ? AMBER_VALUE1 : AMBER_VALUE2);

    for (int x = 0; x < 8; x++) {
      for (int y = 0; y < 8; y++) {
        Piece piece = gameState.getBoard().get(x, y);
        if (piece != null) {
            int pieceValue;

            if (piece.getType() == PieceType.Robbe) pieceValue = ROBBE_VALUE;
            else if (piece.getType() == PieceType.Seestern) pieceValue = SEESTERN_VALUE;
            else if (piece.getType() == PieceType.Moewe) pieceValue = MOEWE_VALUE; 
            else pieceValue = HERZMUSCHEL_VALUE;
            
            if (piece.getTeam() == teamMax) scoreMax += pieceValue;
            else scoreMin += pieceValue;
        }
      }
    }
  
    return scoreMax - scoreMin; // Bewertung der Position des Max Spielers - Bewertung der Position des Min Spielers
  }
}
