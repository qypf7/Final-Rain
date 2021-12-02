package sc.player2022.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.api.plugins.IGameState;
import sc.api.plugins.ITeam;
import sc.player.IGameHandler;
import sc.plugin2022.Coordinates;
import sc.plugin2022.GameState;
import sc.plugin2022.Move;
import sc.plugin2022.Piece;
import sc.shared.GameResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Das Herz des Clients: Eine sehr simple Logik, die ihre Zuege zufaellig
 * waehlt, aber gueltige Zuege macht.
 * <p>
 * Ausserdem werden zum Spielverlauf Konsolenausgaben gemacht.
 */
public class Logic implements IGameHandler {
  private static final Logger log = LoggerFactory.getLogger(Logic.class);

  /** Aktueller Spielstatus. */
  private GameState gameState;

  public void onGameOver(GameResult data) {
    log.info("Das Spiel ist beendet, Ergebnis: {}", data);
  }

  @Override
  public Move calculateMove() {
    long startTime = System.currentTimeMillis();
    log.info("Es wurde ein Zug von {} angefordert.", gameState.getCurrentTeam());

    List<Move> possibleMoves = gameState.getPossibleMoves();
    System.out.println("Anzahl der m�glichen Z�ge: " + possibleMoves.size());

    for (Move m : possibleMoves) {
      System.out.println(m.toString());
      System.out.println(gameState.getBoard().get(m.getFrom().getX(), m.getFrom().getY()));

    }

    // CurrentPieces
    Map<Coordinates, Piece> p = new HashMap<>();
    p = gameState.getCurrentPieces();
    System.out.println("Count Pieces:" + p.size());
    for (Map.Entry<Coordinates, Piece> singleP : p.entrySet()) {
      System.out.println(singleP.getKey() + "/" + singleP.getValue());
    }


    Move move = calcE1(possibleMoves);
    //Move move = possibleMoves.get((int) (Math.random() * possibleMoves.size()));
    
    log.info("Sende {} nach {}ms.", move, System.currentTimeMillis() - startTime);
    return move;
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
  
  //eigene Methoden
  public Move calcE1(List<Move> lm) {
	  
	  Move best = null;
	  best = lm.get(0);
	  
    int result = Integer.MIN_VALUE;
    for (Move move : lm) {
      int erg = findTheBest(move, gameState);
      if (erg > result) { //besseren Zug gefunden
        result = erg;
        best = move;
      }
    }

	  return best;
  }

  public int findTheBest(Move fm, GameState fg) {
    int score = 0;
    GameState cfg = new GameState();
    cfg = fg.clone();
    ITeam myTeam = cfg.getCurrentTeam();
    cfg.performMove(fm);
    score = cfg.getPointsForTeam(myTeam);
    
  
    //Berechnung
    return score;
  }
  
}
