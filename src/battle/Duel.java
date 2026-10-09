package battle;

import model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Reglas del duelo Yu-Gi-Oh! Lite: 3 cartas por lado, gana el primero en
 * llegar a 2 rondas. No toca la interfaz: todo se comunica por BattleListener.
 *
 * ---------------------------------------------------------------------
 * DECISIÓN DE DISEÑO: ¿quién está en ataque y quién en defensa?
 * El enunciado compara "ATK vs DEF" pero no dice cómo se asigna la posición.
 * Criterio adoptado:
 *   1. Cada carta adopta automáticamente su MEJOR posición:
 *        ATK >= DEF -> posición de ATAQUE (valor de combate = ATK)
 *        ATK <  DEF -> posición de DEFENSA (valor de combate = DEF)
 *   2. Con eso se cubren todos los casos del enunciado:
 *        - ambas en ataque         -> gana el mayor ATK
 *        - una ataque, una defensa -> ATK del atacante vs DEF del defensor
 *        - ambas en defensa (no lo cubre el enunciado) -> gana el mayor DEF
 *   3. Iniciativa: si la tiene la máquina, juega su carta PRIMERO (se anuncia con
 *      onMachinePlays) y luego el jugador elige. Si la tiene el jugador, elige
 *      primero y la máquina responde al azar. Además, en un empate gana quien tiene la iniciativa en esa ronda. El turno inicial
 *      se sortea al crear el duelo y la iniciativa se alterna en cada ronda.
 *      Así nunca hay empate, y el "turno inicial aleatorio" tiene efecto real.
 * ---------------------------------------------------------------------
 */
public class Duel {

    public static final String GANADOR_JUGADOR = "Jugador";
    public static final String GANADOR_IA = "Máquina";
    public static final int CARTAS_POR_JUGADOR = 3;
    public static final int PUNTOS_PARA_GANAR = 2;

    private final List<Card> cartasJugador;
    private final List<Card> cartasIa;
    private final boolean[] usadasJugador = new boolean[CARTAS_POR_JUGADOR];
    private final boolean[] usadasIa = new boolean[CARTAS_POR_JUGADOR];
    private final BattleListener listener;
    private final Random random;

    private int puntajeJugador = 0;
    private int puntajeIa = 0;
    private boolean iniciaJugador;   // quién tiene la iniciativa en la ronda actual
    private boolean terminado = false;
    private int cartaIaPendiente = -1;  // carta que la máquina ya jugó por tener la iniciativa
    private int ultimoIndiceIa = -1;  // carta que la máquina jugó en la última ronda

    public Duel(List<Card> cartasJugador, List<Card> cartasIa, BattleListener listener) {
        this(cartasJugador, cartasIa, listener, new Random());
    }

    /** Constructor con Random inyectable (útil para pruebas reproducibles). */
    public Duel(List<Card> cartasJugador, List<Card> cartasIa, BattleListener listener, Random random) {
        Objects.requireNonNull(cartasJugador, "Las cartas del jugador no pueden ser null");
        Objects.requireNonNull(cartasIa, "Las cartas de la máquina no pueden ser null");
        this.listener = Objects.requireNonNull(listener, "El listener no puede ser null");
        this.random = Objects.requireNonNull(random, "Random no puede ser null");

        if (cartasJugador.size() != CARTAS_POR_JUGADOR || cartasIa.size() != CARTAS_POR_JUGADOR) {
            throw new IllegalArgumentException(
                    "Cada jugador debe tener exactamente " + CARTAS_POR_JUGADOR + " cartas");
        }
        if (tieneNulos(cartasJugador) || tieneNulos(cartasIa)) {
            throw new IllegalArgumentException("Las listas no pueden contener cartas null");
        }

        // Copia defensiva: la lista original puede cambiar sin afectar el duelo.
        this.cartasJugador = new ArrayList<>(cartasJugador);
        this.cartasIa = new ArrayList<>(cartasIa);
        this.iniciaJugador = random.nextBoolean();   // turno inicial aleatorio
    }

    /**
     * Juega una ronda: el jugador usa la carta indicada y la máquina elige
     * una al azar entre las que le quedan.
     *
     * @param indiceCartaJugador posición (0..2) de la carta del jugador
     * @throws IllegalStateException    si el duelo ya terminó
     * @throws IllegalArgumentException si el índice es inválido o la carta ya se usó
     */
    public void jugarTurno(int indiceCartaJugador) {
        if (terminado) {
            throw new IllegalStateException("El duelo ya terminó");
        }
        if (indiceCartaJugador < 0 || indiceCartaJugador >= CARTAS_POR_JUGADOR) {
            throw new IllegalArgumentException("Índice de carta inválido: " + indiceCartaJugador);
        }
        if (usadasJugador[indiceCartaJugador]) {
            throw new IllegalArgumentException("Esa carta ya fue usada en una ronda anterior");
        }

        int indiceIa = (cartaIaPendiente >= 0) ? cartaIaPendiente : elegirCartaIa();
        cartaIaPendiente = -1;
        ultimoIndiceIa = indiceIa;   // se guarda ANTES de notificar al listener
        usadasJugador[indiceCartaJugador] = true;
        usadasIa[indiceIa] = true;

        Card cartaJ = cartasJugador.get(indiceCartaJugador);
        Card cartaI = cartasIa.get(indiceIa);

        // Se calcula y actualiza TODO el estado antes de notificar:
        // si un listener lanza una excepción, el duelo no queda inconsistente.
        boolean jAtaque = cartaJ.getAtk() >= cartaJ.getDef();
        boolean iAtaque = cartaI.getAtk() >= cartaI.getDef();
        int valorJ = jAtaque ? cartaJ.getAtk() : cartaJ.getDef();
        int valorI = iAtaque ? cartaI.getAtk() : cartaI.getDef();

        boolean ganaJugador;
        String motivoEmpate = "";
        if (valorJ != valorI) {
            ganaJugador = valorJ > valorI;
        } else {
            ganaJugador = iniciaJugador;
            motivoEmpate = " (empate: gana quien tenía la iniciativa)";
        }

        if (ganaJugador) {
            puntajeJugador++;
        } else {
            puntajeIa++;
        }
        iniciaJugador = !iniciaJugador;   // la iniciativa se alterna cada ronda
        terminado = puntajeJugador >= PUNTOS_PARA_GANAR || puntajeIa >= PUNTOS_PARA_GANAR;

        String ganador = ganaJugador ? GANADOR_JUGADOR : GANADOR_IA;
        String detalle = String.format("%s en %s (%d) vs %s en %s (%d)%s",
                cartaJ.getNombre(), jAtaque ? "ATAQUE" : "DEFENSA", valorJ,
                cartaI.getNombre(), iAtaque ? "ATAQUE" : "DEFENSA", valorI,
                motivoEmpate);

        // Notificaciones en orden: detalle -> turno -> marcador -> fin (si aplica)
        listener.onRoundDetail(detalle);
        listener.onTurn(cartaJ.getNombre(), cartaI.getNombre(), ganador);
        listener.onScoreChanged(puntajeJugador, puntajeIa);
        if (terminado) {
            listener.onDuelEnded(puntajeJugador >= PUNTOS_PARA_GANAR ? GANADOR_JUGADOR : GANADOR_IA);
        } else {
            iniciarRonda(); // si la máquina tiene la iniciativa, juega su carta de la ronda siguiente
        }
    }

    /**
     * Abre una ronda. Si la máquina tiene la iniciativa, juega su carta ahora y lo avisa
     * al listener. Si la tiene el jugador, no hace nada. Hay que llamarlo una vez justo
     * después de crear el duelo; después Duel lo llama solo al terminar cada ronda.
     */
    public void iniciarRonda() {
        if (terminado || iniciaJugador || cartaIaPendiente >= 0) {
            return;
        }
        int indice = elegirCartaIa();
        cartaIaPendiente = indice;
        listener.onMachinePlays(indice, cartasIa.get(indice).toString());
    }

    /** Recorre la lista a mano: List.of(...).contains(null) lanza NullPointerException. */
    private static boolean tieneNulos(List<Card> cartas) {
        for (Card c : cartas) {
            if (c == null) {
                return true;
            }
        }
        return false;
    }

    /** Elige al azar una carta de la máquina que aún no se haya usado. */
    private int elegirCartaIa() {
        List<Integer> disponibles = new ArrayList<>();
        for (int i = 0; i < CARTAS_POR_JUGADOR; i++) {
            if (!usadasIa[i]) {
                disponibles.add(i);
            }
        }
        if (disponibles.isEmpty()) {
            throw new IllegalStateException("La máquina no tiene cartas disponibles");
        }
        return disponibles.get(random.nextInt(disponibles.size()));
    }

    /** Posición (0..2) de la carta que la máquina jugó en la última ronda; -1 si aún no hay. */
    public int getUltimoIndiceIa() {
        return ultimoIndiceIa;
    }

    public boolean haTerminado() {
        return terminado;
    }

    public int getPuntajeJugador() {
        return puntajeJugador;
    }

    public int getPuntajeIa() {
        return puntajeIa;
    }

    /** Para que la ventana deshabilite las cartas ya jugadas. */
    public boolean cartaUsada(int indiceCartaJugador) {
        if (indiceCartaJugador < 0 || indiceCartaJugador >= CARTAS_POR_JUGADOR) {
            throw new IllegalArgumentException("Índice de carta inválido: " + indiceCartaJugador);
        }
        return usadasJugador[indiceCartaJugador];
    }

    /** True si el jugador tiene la iniciativa en la ronda que viene. */
    public boolean iniciaJugador() {
        return iniciaJugador;
    }
}