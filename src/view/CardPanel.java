package view;

import model.Card;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URI;

/**
 * Muestra UNA carta: imagen, nombre, ATK y DEF, con su botón "Elegir carta".
 * No conoce el duelo: cuando se pulsa el botón solo ejecuta lo que le pasen.
 */
public class CardPanel extends JPanel {

    private final JLabel lblImagen = new JLabel("", SwingConstants.CENTER);
    private final JLabel lblNombre = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblStats = new JLabel("ATK - | DEF -", SwingConstants.CENTER);
    private final JButton btnElegir = new JButton("Elegir carta");

    private Card carta;
    private boolean usada = false;

    public CardPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEtchedBorder());

        // Tamaño fijo para que el panel no "salte" mientras carga la imagen
        lblImagen.setPreferredSize(new Dimension(130, 190));
        lblImagen.setMinimumSize(new Dimension(130, 190));

        lblImagen.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblNombre.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStats.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnElegir.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnElegir.setEnabled(false); // se habilita cuando empieza el duelo

        add(lblImagen);
        add(lblNombre);
        add(lblStats);
        add(btnElegir);
    }

    // Pinta los datos de la carta
    public void mostrarCarta(Card c) {
        this.carta = c;
        this.usada = false;
        // html + width: los nombres largos se parten en varias líneas
        lblNombre.setText("<html><body style='width:120px;text-align:center'>"
                + c.getNombre() + "</body></html>");
        lblStats.setText("ATK " + c.getAtk() + " | DEF " + c.getDef());
        btnElegir.setText("Elegir carta");
        cargarImagen(c.getImagenUrl());
    }

    // La ventana indica qué hacer cuando se pulsa "Elegir carta".
    // Llámalo UNA sola vez por panel.
    public void setAlElegir(Runnable accion) {
        btnElegir.addActionListener(e -> accion.run());
    }

    // Habilita o deshabilita el botón (las cartas usadas siguen deshabilitadas)
    public void setElegible(boolean elegible) {
        btnElegir.setEnabled(elegible && !usada && carta != null);
    }

    // Marca la carta como ya jugada
    public void marcarUsada() {
        usada = true;
        btnElegir.setEnabled(false);
        btnElegir.setText("Usada");
        lblImagen.setEnabled(false); // Swing pone la imagen en gris
    }

    public Card getCarta() {
        return carta;
    }

    // Descarga la imagen en un hilo de fondo para no congelar la ventana
    private void cargarImagen(String url) {
        lblImagen.setIcon(null);
        lblImagen.setEnabled(true);

        if (url == null || url.isEmpty()) {
            lblImagen.setText("Sin imagen");
            return;
        }
        lblImagen.setText("Cargando...");

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                // Hilo de fondo: la descarga puede tardar
                Image img = new ImageIcon(URI.create(url).toURL()).getImage();
                return new ImageIcon(img.getScaledInstance(130, 190, Image.SCALE_SMOOTH));
            }

            @Override
            protected void done() {
                // De vuelta en el hilo de la interfaz: aquí sí se toca la pantalla
                try {
                    lblImagen.setText("");
                    lblImagen.setIcon(get());
                } catch (Exception e) {
                    lblImagen.setIcon(null);
                    lblImagen.setText("Sin imagen");
                }
            }
        }.execute();
    }
}