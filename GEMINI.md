# Reglas de Interfaz y Diseño UI — BTM Contabilidad Android

## Regla — Cero distorsión en diseño de interfaces (UI / Mobile / Web)
Al diseñar, crear o modificar pantallas (Jetpack Compose, Flutter, HTML/Web, etc.):
- **Prohibido distorsionar cifras y textos críticos:** Montos monetarios, balances, monedas, identificadores y etiquetas numéricas NUNCA deben romperse, truncarse ni dividirse entre líneas de forma descuidada (por ejemplo, separar el signo/moneda `- US$` del valor numérico `347.00`).
- **Anchos y distribución segura:** En tarjetas, modales o diálogos donde el espacio horizontal sea limitado, los valores numéricos destacados deben tener su propia fila o línea con `maxLines = 1` y `softWrap = false`, evitando `Row(SpaceBetween)` donde el texto izquierdo estrangule al monto derecho.
- **Scroll y accesibilidad de acciones:** Todo diálogo, modal o formulario con varios campos debe implementar scroll vertical (`verticalScroll()`) y padding para teclado (`imePadding()`), con botones de acción a ancho completo o proporciones equilibradas para garantizar que ningún botón quede fuera de vista o inalcanzable.
- **Verificación visual obligatoria:** Antes de dar por finalizada una tarea con interfaz de usuario, se debe verificar visualmente en emulador o captura de pantalla que ningún texto se encime, recorte o deforme.
