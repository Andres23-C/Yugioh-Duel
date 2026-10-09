package battle;

/**
 * Eventos de un duelo. Desacopla la lógica (Duel) de la interfaz gráfica.
 * Los métodos se invocan en el mismo hilo que llama a Duel.jugarTurno().
 */
public interface BattleListener {

    /** Se notifica al resolverse cada ronda. winner: "Jugador" o "Máquina". */
    void onTurn(String playerCard, String aiCard, String winner);

    /** Se notifica cada vez que cambia el marcador. */
    void onScoreChanged(int playerScore, int aiScore);

    /** Se notifica una sola vez, cuando alguien llega a 2 rondas ganadas. */
    void onDuelEnded(String winner);

    /**
     * Opcional: explicación de la ronda (posiciones y valores comparados),
     * útil para el log. Se llama justo antes de onTurn. Se puede ignorar.
     */
    default void onRoundDetail(String detail) {
    }

    /**
     * Opcional: la máquina tiene la iniciativa y juega su carta ANTES de que el
     * jugador elija. cardIndex es la posición (0..2) de su carta; card, su descripción.
     */
    default void onMachinePlays(int cardIndex, String card) {
    }
}