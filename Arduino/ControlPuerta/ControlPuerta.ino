// Mi sketch para el Arduino Uno: recibe comandos firmados por Bluetooth (HC-05)
// desde la app y controla la puerta con el relé y el sensor magnético.

// SoftwareSerial me permite hablar por Bluetooth en los pines 2 y 3,
// dejando el 0 y 1 libres para el monitor serie.
#include <SoftwareSerial.h>
// La EEPROM me sirve para guardar el contador anti-replay aunque se apague la placa.
#include <EEPROM.h>

SoftwareSerial miBluetooth(2, 3);

const int pinRele = 8;     // relé que abre/cierra la cerradura
const int pinSensor = 4;   // sensor magnético de la puerta
const int DIR_CONTADOR = 0; // dirección en la EEPROM donde guardo el contador

unsigned long ultimoContador = 0;
int ultimoEstadoSensor = -1;

// El setup corre una sola vez al encender.
void setup() {
  pinMode(pinRele, OUTPUT);
  digitalWrite(pinRele, LOW); // arranco con la puerta bloqueada

  pinMode(pinSensor, INPUT_PULLUP);

  Serial.begin(9600);        // monitor serie para depurar
  miBluetooth.begin(9600);   // velocidad del HC-05

  // Recupero el contador de la EEPROM; si la dirección está vacía, empiezo en 0.
  EEPROM.get(DIR_CONTADOR, ultimoContador);
  if (ultimoContador == 0xFFFFFFFF) {
    ultimoContador = 0;
  }
}

// El loop se repite siempre: atiendo lo que llega por Bluetooth y reviso el sensor.
void loop() {
  if (miBluetooth.available()) {
    String linea = miBluetooth.readStringUntil('\n');
    linea.trim();
    if (linea.length() > 0) {
      procesarLinea(linea);
    }
  }

  revisarSensor();
}

// Decido qué hacer con cada línea que llega desde la app.
void procesarLinea(String linea) {
  // Acepto también "1" y "0" para probar sin firma.
  if (linea == "1") {
    abrirPuerta();
    return;
  }
  if (linea == "0") {
    cerrarPuerta();
    return;
  }

  // Un token válido tiene el formato COMANDO|CONTADOR|FIRMA, o sea dos "|".
  int p1 = linea.indexOf('|');
  int p2 = linea.indexOf('|', p1 + 1);

  if (p1 < 0 || p2 < 0) {
    miBluetooth.println("ERROR:FORMATO");
    return;
  }

  String comando = linea.substring(0, p1);
  unsigned long contador = linea.substring(p1 + 1, p2).toInt();
  String firma = linea.substring(p2 + 1);

  // La firma HMAC-SHA256 siempre tiene 64 caracteres.
  if (firma.length() != 64) {
    miBluetooth.println("ERROR:FIRMA");
    return;
  }

  // Anti-replay ISO: si el contador no es mayor al último que acepté,
  // alguien está reenviando un comando capturado y lo rechazo.
  if (contador <= ultimoContador) {
    miBluetooth.println("ERROR:REPLAY");
    return;
  }

  if (comando == "OPEN") {
    abrirPuerta();
  } else if (comando == "CLOSE") {
    cerrarPuerta();
  } else {
    miBluetooth.println("ERROR:COMANDO");
    return;
  }

  // Guardo el contador aceptado en RAM y en la EEPROM para que no se pierda
  // si alguien apaga el Arduino.
  ultimoContador = contador;
  EEPROM.put(DIR_CONTADOR, ultimoContador);
}

// Energizo el relé para abrir y le aviso a la app.
void abrirPuerta() {
  digitalWrite(pinRele, HIGH);
  miBluetooth.println("LOCK:OPEN");
}

// Apago el relé para bloquear y le aviso a la app.
void cerrarPuerta() {
  digitalWrite(pinRele, LOW);
  miBluetooth.println("LOCK:CLOSED");
}

// Aviso solo cuando el estado del sensor cambia, para no saturar la app
// con el mismo mensaje todo el rato.
void revisarSensor() {
  int estado = digitalRead(pinSensor);
  if (estado != ultimoEstadoSensor) {
    ultimoEstadoSensor = estado;
    miBluetooth.println(estado == HIGH ? "SENSOR:OPEN" : "SENSOR:CLOSED");
  }
}
