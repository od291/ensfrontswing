/**
 * 
 */
/**
 * 
 */
module HelloWord {
    requires java.base;  // Module de base de Java
    requires java.net.http;  // Si tu utilises java.net.http
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.jfree.jfreechart;
    requires com.fasterxml.jackson.annotation;

    requires java.desktop;  // Nécessaire pour les bibliothèques Swing
  // Exporter ton package
}
