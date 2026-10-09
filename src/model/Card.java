package model;

public class Card {

    private final String nombre;
    private final int atk;
    private final int def;
    private final String imagenUrl;

    public Card(String nombre, int atk, int def, String imagenUrl) {
        this.nombre = nombre;
        this.atk = atk;
        this.def = def;
        this.imagenUrl = imagenUrl;
    }

    public String getNombre() {
        return nombre;
    }

    public int getAtk() {
        return atk;
    }

    public int getDef() {
        return def;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    // Útil para el log y para las pruebas en consola
    @Override
    public String toString() {
        return nombre + " (ATK " + atk + " / DEF " + def + ")";
    }
}