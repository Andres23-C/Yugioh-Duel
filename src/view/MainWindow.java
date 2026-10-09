package view;

import api.YgoApiClient;
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
 * Las reglas del duelo NO están aquí: viven en Duel, que avisa por eventos.
 */
public class MainWindow extends JFrame {

    private static final int CARTAS_POR_JUGADOR = 3;

    private final YgoApiClient cliente = new YgoApiClient();

    private final JButton btnCargar = new JButton("Cargar cartas");
    private final JButton btnIniciar = new JButton("Iniciar duelo");
    private final JLabel lblEstado = new JLabel("Cargando cartas...");
    private final JLabel lblMarcador = new JLabel("Marcador: Jugador 0 - 0 Máquina");
    private final CardPanel[] panelesJugador = new CardPanel[CARTAS_POR_JUGADOR];
    private final JTextArea log = new JTextArea(10, 50);

    private List<Card> cartasJugador;
    private List<Card> cartasIa;
    private boolean duelEnCurso = false;

    public MainWindow() {
        super("Yu-Gi-Oh! Duel Lite");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Arriba: botones y estado
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(btnCargar);
        norte.add(btnIniciar);
        norte.add(lblEstado);
        btnIniciar.setEnabled(false); // no se puede iniciar sin las 3 cartas

        // Centro: las 3 cartas del jugador
        JPanel centro = new JPanel(new GridLayout(1, CARTAS_POR_JUGADOR, 10, 0));
        centro.setBorder(BorderFactory.createTitledBorder("Tus cartas"));
        for (int i = 0; i < CARTAS_POR_JUGADOR; i++) {
            final int indice = i;
            panelesJugador[i] = new CardPanel();
            panelesJugador[i].setAlElegir(() -> elegirCarta(indice));
            centro.add(panelesJugador[i]);
        }

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

        pack();
        setLocationRelativeTo(null);

        cargarCartas(); // "al iniciar" cada jugador recibe sus cartas
    }

    // Pide 6 cartas (3 y 3) en un hilo de fondo para no congelar la ventana
    private void cargarCartas() {
        btnCargar.setEnabled(false);
        btnIniciar.setEnabled(false);
        lblEstado.setText("Cargando cartas...");
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

    private void iniciarDuelo() {
        if (cartasJugador == null || cartasIa == null) {
            return; // validación: faltan cartas
        }
        duelEnCurso = true;
        btnIniciar.setEnabled(false);
        btnCargar.setEnabled(false);
        log.setText("");
        lblMarcador.setText("Marcador: Jugador 0 - 0 Máquina");
        lblEstado.setText("¡Elige una carta!");
        for (CardPanel p : panelesJugador) {
            p.setElegible(true);
        }
        escribir("Duelo iniciado.");

        // TODO (cuando exista Duel): duel = new Duel(cartasJugador, cartasIa, this);
    }

    private void elegirCarta(int indice) {
        if (!duelEnCurso) {
            return;
        }
        panelesJugador[indice].marcarUsada();

        // TODO (cuando exista Duel): duel.jugarTurno(indice);
        // Duel avisará por onTurn, onScoreChanged y onDuelEnded.
        escribir("Elegiste: " + cartasJugador.get(indice).getNombre()); // temporal: borrar al conectar
    }

    // ---- Eventos del duelo (los llamará Duel a través de BattleListener) ----
    // Cuando exista la interfaz, se agrega "implements BattleListener" a la clase.

    public void onTurn(String playerCard, String aiCard, String winner) {
        enUI(() -> escribir("Jugaste " + playerCard + " | Máquina jugó " + aiCard
                + " -> Ganó: " + winner));
    }

    public void onScoreChanged(int playerScore, int aiScore) {
        enUI(() -> {
            lblMarcador.setText("Marcador: Jugador " + playerScore + " - " + aiScore + " Máquina");
            escribir("Puntaje: Jugador " + playerScore + " - " + aiScore + " Máquina");
        });
    }

    public void onDuelEnded(String winner) {
        enUI(() -> {
            duelEnCurso = false;
            escribir("¡Duelo terminado! Ganador: " + winner);
            lblEstado.setText("Ganador: " + winner);
            for (CardPanel p : panelesJugador) {
                p.setElegible(false);
            }
            btnCargar.setEnabled(true); // permite empezar otro duelo
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
