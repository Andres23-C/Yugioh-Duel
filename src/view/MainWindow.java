package view;

import api.YgoApiClient;
import battle.BattleListener;
import battle.Duel;
import model.Card;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal del duelo.
 * Carga las cartas, deja elegir una por turno y muestra el log y el marcador.
 * Muestra las cartas del jugador (con botón) y las de la máquina (solo visibles).
 * Las reglas del duelo NO están aquí: viven en Duel, que avisa por eventos.
 */
public class MainWindow extends JFrame implements BattleListener {

    private static final int CARTAS_POR_JUGADOR = 3;

    private final YgoApiClient cliente = new YgoApiClient();

    private final JButton btnCargar = new JButton("Cargar cartas");
    private final JButton btnIniciar = new JButton("Iniciar duelo");
    private final JButton btnReiniciar = new JButton("Reiniciar duelo");
    private final JLabel lblEstado = new JLabel("Cargando cartas...");
    private final JLabel lblMarcador = new JLabel("Marcador: Jugador 0 - 0 Máquina");
    private final CardPanel[] panelesJugador = new CardPanel[CARTAS_POR_JUGADOR];
    private final CardPanel[] panelesIa = new CardPanel[CARTAS_POR_JUGADOR];
    private final JTextArea log = new JTextArea(10, 50);

    private List<Card> cartasJugador;
    private List<Card> cartasIa;
    private boolean duelEnCurso = false;
    private Duel duel;

    public MainWindow() {
        super("Yu-Gi-Oh! Duel Lite");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Arriba: botones y estado
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(btnCargar);
        norte.add(btnIniciar);
        norte.add(btnReiniciar);
        norte.add(lblEstado);
        btnIniciar.setEnabled(false);   // no se puede iniciar sin las 3 cartas
        btnReiniciar.setEnabled(false); // solo tiene sentido con cartas cargadas

        // Centro: a la izquierda las cartas de la máquina, a la derecha las tuyas
        JPanel zonaIa = new JPanel(new GridLayout(1, CARTAS_POR_JUGADOR, 10, 0));
        zonaIa.setBorder(BorderFactory.createTitledBorder("Cartas de la máquina"));
        JPanel zonaJugador = new JPanel(new GridLayout(1, CARTAS_POR_JUGADOR, 10, 0));
        zonaJugador.setBorder(BorderFactory.createTitledBorder("Tus cartas"));

        for (int i = 0; i < CARTAS_POR_JUGADOR; i++) {
            final int indice = i;

            panelesIa[i] = new CardPanel();
            panelesIa[i].ocultarBoton(); // la máquina elige sola: solo se ven sus cartas
            zonaIa.add(panelesIa[i]);

            panelesJugador[i] = new CardPanel();
            panelesJugador[i].setAlElegir(() -> elegirCarta(indice));
            zonaJugador.add(panelesJugador[i]);
        }
        JPanel centro = new JPanel(new GridLayout(1, 2, 15, 0));
        centro.add(zonaIa);
        centro.add(zonaJugador);

        // Abajo: marcador y log con scroll
        log.setEditable(false);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(lblMarcador, BorderLayout.NORTH);
        sur.add(new JScrollPane(log), BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        // ActionListener de cada botón
        btnCargar.addActionListener(e -> cargarCartas());
        btnIniciar.addActionListener(e -> iniciarDuelo());
        btnReiniciar.addActionListener(e -> iniciarDuelo()); // mismas cartas, duelo nuevo

        pack();
        setLocationRelativeTo(null);

        cargarCartas(); // "al iniciar" cada jugador recibe sus cartas
    }

    // Pide 6 cartas (3 y 3) en un hilo de fondo para no congelar la ventana
    private void cargarCartas() {
        btnCargar.setEnabled(false);
        btnIniciar.setEnabled(false);
        btnReiniciar.setEnabled(false);
        lblEstado.setText("Cargando cartas...");
        duelEnCurso = false; // cargar cartas nuevas cancela el duelo actual
        duel = null;
        log.setText("");
        lblMarcador.setText("Marcador: Jugador 0 - 0 Máquina");
        cartasJugador = null;
        cartasIa = null;
        for (CardPanel p : panelesJugador) {
            p.setElegible(false);
        }

        new SwingWorker<List<Card>, Void>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                return cliente.obtenerMazo(2 * CARTAS_POR_JUGADOR);
            }

            @Override
            protected void done() {
                try {
                    List<Card> todas = get();
                    cartasJugador = new ArrayList<>(todas.subList(0, CARTAS_POR_JUGADOR));
                    cartasIa = new ArrayList<>(todas.subList(CARTAS_POR_JUGADOR, 2 * CARTAS_POR_JUGADOR));
                    for (int i = 0; i < CARTAS_POR_JUGADOR; i++) {
                        panelesJugador[i].mostrarCarta(cartasJugador.get(i));
                        panelesIa[i].mostrarCarta(cartasIa.get(i));
                    }
                    lblEstado.setText("Cartas listas. Pulsa \"Iniciar duelo\".");
                    btnIniciar.setEnabled(true);
                } catch (ExecutionException e) {
                    String mensaje = mensajeDeError(e.getCause());
                    lblEstado.setText(mensaje);
                    escribir(mensaje);
                } catch (InterruptedException e) {
                    lblEstado.setText("Carga interrumpida");
                }
                btnCargar.setEnabled(true);
            }
        }.execute();
    }

    // Inicia un duelo nuevo con las cartas cargadas. También sirve para reiniciar:
    // vuelve a dejar todas las cartas como nuevas y empieza de cero.
    private void iniciarDuelo() {
        if (cartasJugador == null || cartasIa == null) {
            return; // validación: faltan cartas
        }

        Duel nuevo;
        try {
            nuevo = new Duel(cartasJugador, cartasIa, this);
        } catch (IllegalArgumentException ex) {
            escribir("Error al crear el duelo: " + ex.getMessage());
            return;
        }
        duel = nuevo;

        for (CardPanel p : panelesJugador) {
            p.reiniciar();
        }
        for (CardPanel p : panelesIa) {
            p.reiniciar();
        }

        duelEnCurso = true;
        btnIniciar.setEnabled(false);
        btnReiniciar.setEnabled(true);
        log.setText("");
        lblMarcador.setText("Marcador: Jugador 0 - 0 Máquina");
        lblEstado.setText("¡Elige una carta!");
        for (CardPanel p : panelesJugador) {
            p.setElegible(true);
        }
        escribir("Duelo iniciado.");
        escribir(duel.iniciaJugador() ? "Empieza el jugador." : "Empieza la máquina.");
        duel.iniciarRonda(); // si empieza la máquina, juega su carta ahora
    }

    private void elegirCarta(int indice) {
        if (!duelEnCurso || duel == null) {
            return;
        }
        panelesJugador[indice].marcarUsada();
        try {
            duel.jugarTurno(indice);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            escribir("Error: " + ex.getMessage());
        }
    }

    // ---- Eventos del duelo (los llama Duel a través de BattleListener) ----

    @Override
    public void onRoundDetail(String detail) {
        enUI(() -> escribir("  " + detail));
    }

    @Override
    public void onMachinePlays(int cardIndex, String card) {
        enUI(() -> {
            if (cardIndex >= 0 && cardIndex < CARTAS_POR_JUGADOR) {
                panelesIa[cardIndex].marcarUsada(); // se ve en gris la carta que jugó
            }
            escribir("La máquina juega primero: " + card);
            lblEstado.setText("La máquina ya jugó. ¡Elige tu carta!");
        });
    }

    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        // Se lee aquí: Duel ya guardó qué carta de la máquina se jugó
        final int indiceIa = (duel != null) ? duel.getUltimoIndiceIa() : -1;
        enUI(() -> {
            if (indiceIa >= 0 && indiceIa < CARTAS_POR_JUGADOR) {
                panelesIa[indiceIa].marcarUsada(); // se ve cuál carta ya gastó la máquina
            }
            escribir("Jugaste " + playerCard + " | Máquina jugó " + aiCard
                    + " -> Ganó: " + winner);
        });
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        enUI(() -> {
            lblMarcador.setText("Marcador: Jugador " + playerScore + " - " + aiScore + " Máquina");
            escribir("Puntaje: Jugador " + playerScore + " - " + aiScore + " Máquina");
        });
    }

    @Override
    public void onDuelEnded(String winner) {
        enUI(() -> {
            duelEnCurso = false;
            escribir("¡Duelo terminado! Ganador: " + winner);
            lblEstado.setText("Ganador: " + winner + ". Pulsa \"Reiniciar duelo\" para jugar de nuevo.");
            for (CardPanel p : panelesJugador) {
                p.setElegible(false);
            }
            btnCargar.setEnabled(true); // permite pedir cartas nuevas
        });
    }

    // ---- Utilidades ----

    private void escribir(String texto) {
        log.append(texto + "\n");
        log.setCaretPosition(log.getDocument().getLength()); // baja el scroll
    }

    // Garantiza que la pantalla se toque desde el hilo de Swing
    private void enUI(Runnable accion) {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.run();
        } else {
            SwingUtilities.invokeLater(accion);
        }
    }

    private String mensajeDeError(Throwable causa) {
        if (causa instanceof IOException) {
            return "Error de red: revisa tu conexión";
        }
        if (causa != null && causa.getMessage() != null) {
            return causa.getMessage();
        }
        return "No se pudo cargar la carta";
    }

    public static void main(String[] args) {
        // La interfaz siempre se crea en el hilo de Swing
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}