Yugioh-Duel

Aplicación de escritorio en Java (Swing) que consume la API de YGOProDeck para recrear un duelo sencillo de Yu-Gi-Oh! entre el jugador y la máquina, como parte del Laboratorio #1 de Desarrollo de Software III (Universidad del Valle, Sede Tuluá).

Ejecución

Requisitos: Java 11 o superior y conexión a internet (las cartas y sus imágenes se descargan en vivo).

Dependencia: org.json, incluida en lib/json-20230227.jar. No se usan frameworks pesados.

En IntelliJ IDEA
Abre la carpeta del proyecto.
Clic derecho sobre lib/json-20230227.jar > Add as Library... > OK.
Marca src como Sources Root si no lo está (clic derecho > Mark Directory as).
Ejecuta la clase view.MainWindow.
Por terminal (PowerShell)
powershell
javac -encoding UTF-8 -cp lib/json-20230227.jar -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out;lib/json-20230227.jar" view.MainWindow
Cómo se juega
Al abrir la ventana se cargan solas 3 cartas Monster para el jugador y 3 para la máquina. Con Cargar cartas se piden cartas nuevas.
Se muestran las cartas de ambos lados con imagen, nombre, ATK y DEF. Las de la máquina solo se ven; las tuyas tienen el botón Elegir carta.
Iniciar duelo habilita las cartas. En cada ronda eliges una; la máquina juega una al azar entre las que le quedan. Las cartas usadas se ponen en gris.
El ganador de la ronda suma 1 punto. El primero en llegar a 2 rondas gana el duelo, y el log lo anuncia.
Reiniciar duelo empieza de nuevo con las mismas cartas, incluso a mitad de partida.
Diseño

El proyecto separa la interfaz, la lógica y el acceso a datos en paquetes con responsabilidades claras:

api: YgoApiClient consulta randomcard.php con java.net.http.HttpClient, interpreta el JSON con org.json y vuelve a pedir la carta si no es de tipo Monster. No conoce la interfaz: ante un fallo lanza una excepción y la ventana decide qué mostrar ("No se pudo cargar la carta", "Error de red").
model: Card (nombre, ATK, DEF y URL de la imagen).
battle: Duel contiene las reglas y BattleListener define los eventos onTurn, onScoreChanged y onDuelEnded (más onRoundDetail, opcional, con la explicación de cada ronda). Duel nunca toca la interfaz: solo avisa al listener.
view: MainWindow implementa BattleListener y actualiza marcador y log (JTextArea + JScrollPane); CardPanel dibuja una carta. Las peticiones de red y la descarga de imágenes corren en hilos de fondo con SwingWorker, así la interfaz nunca se bloquea.
Reglas del duelo (decisiones de diseño)

El enunciado compara ATK contra DEF pero no dice quién está en ataque o en defensa, así que se definió así:

Cada carta adopta su mejor posición: si ATK >= DEF está en ataque y combate con su ATK; si no, está en defensa y combate con su DEF.
Ambas en ataque: gana el mayor ATK. Una en ataque y otra en defensa: se compara el ATK del atacante contra el DEF del defensor. Ambas en defensa (caso no previsto en el enunciado): gana el mayor DEF.
El turno inicial se sortea al crear el duelo. Quien tiene la iniciativa gana los empates, y la iniciativa se alterna en cada ronda, por lo que nunca hay empate.
Cada carta se usa una sola vez. Con 3 rondas como máximo, siempre hay un ganador.
Estructura
Yugioh-Duel
├── lib/json-20230227.jar
└── src
    ├── api/    YgoApiClient
    ├── model/  Card
    ├── battle/ BattleListener, Duel, DuelDemo
    └── view/   MainWindow, CardPanel

DuelDemo es una prueba en consola de las reglas, sin ventana ni internet.

Capturas de pantalla
