# 🔄 Villager Reroll Mod (Fabric 1.21.9)

**Villager Reroll** es un mod de cliente para Minecraft (Fabric) diseñado para automatizar el tedioso proceso de conseguir libros encantados específicos (como Mending/Reparación) con aldeanos libreros.

El mod rompe el atril, espera a que el aldeano pierda la profesión, coloca el atril de nuevo y verifica el comercio automáticamente hasta encontrar el encantamiento deseado.

---

## 📋 Requisitos

Para usar este mod necesitas:
1.  **Minecraft Java Edition** (Versión 1.21.9 o la que soporte el mod).
2.  **Fabric Loader** instalado.
3.  **Fabric API** (colocado en la carpeta `mods`).

---

## ⚙️ Instalación

### Opción A: Descargar el .jar (Si ya lo compilaste)
1.  Copia el archivo `villagerreroll-1.0.0.jar` generado.
2.  Pégalo en tu carpeta de mods:
    *   **Linux:** `~/.minecraft/mods`
    *   **Windows:** `%appdata%/.minecraft/mods`
    *   **Mac:** `~/Library/Application Support/minecraft/mods`
3.  Asegúrate de tener también `fabric-api.jar` en esa carpeta.

### Opción B: Compilar desde el código fuente
Si tienes el código fuente y quieres generar el archivo tú mismo:
1.  Abre una terminal en la carpeta del proyecto.
2.  Ejecuta el comando de compilación:
    ```bash
    ./gradlew build
    ```
3.  El archivo resultante estará en `build/libs/villagerreroll-1.0.0.jar`.

---

## 🎮 Cómo usar (Paso a Paso)

Para que el mod funcione correctamente en Survival, debes seguir esta configuración de inventario. El mod aprovecha la mecánica de usar las dos manos para ser más rápido.

### 1. Preparación del Inventario
*   **Mano Principal (Derecha):** Un **Hacha**.
    *   *Recomendación:* Hacha de Diamante/Netherite con **Eficiencia V** (hará el proceso instantáneo).
*   **Mano Secundaria (Izquierda):** El **Atril** (Lectern).
    *   *Nota:* Debes tener el atril en la mano izquierda (`F` por defecto para cambiar de mano).

### 2. Configurar qué buscas
Por defecto, el mod busca **Irrompibilidad III** (Unbreaking 3). Para cambiarlo, usa los comandos en el chat:

*   **Comando:** `/vr set <encantamiento> <nivel>`
*   **Autocompletado:** Presiona `TAB` para ver la lista de encantamientos disponibles.

**Ejemplos:**
```mc
/vr set minecraft:mending 1           (Busca Reparación)
/vr set minecraft:efficiency 5        (Busca Eficiencia V)
/vr set minecraft:fortune 3           (Busca Fortuna III)
/vr set minecraft:protection 4        (Busca Protección IV)
```
Para ver qué estás buscando actualmente:

```Mc
/vr status
```

### 3. Activar el Reroll

Encierra al aldeano y asegúrate de que tiene espacio para el atril.

Apunta con la mira al aldeano (o al atril si ya está puesto).

Presiona la tecla R (puedes cambiarla en Controles).

¡Listo! El mod hará el ciclo automáticamente.

Cuando encuentre el libro, te avisará en el chat, dejará la ventana de comercio abierta y se desactivará solo.

## ⚠️ Advertencias Importantes

Hambre (Hunger): Romper bloques consume saturación. Si dejas el mod funcionando mucho tiempo, tu personaje podría morir de hambre. Lleva comida.

Durabilidad: El hacha se desgastará. Usa una con Unbreaking III o ten cuidado de que no se rompa.

Servidores (Multiplayer):

Este mod envía paquetes de interacción automáticamente (es una macro).

Úsalo bajo tu propio riesgo. En muchos servidores públicos esto puede ser motivo de ban.

El mod incluye pequeños retrasos (delays) para lidiar con el lag del servidor, pero si el server va muy mal, podría fallar al poner el bloque (ghost blocks).

## 🛠️ Solución de Problemas

El mod golpea el bloque pero no lo rompe:

Asegúrate de estar en modo Survival.

Asegúrate de que el mod no se está reiniciando constantemente (no presiones R varias veces seguidas).

El código actual está optimizado para mantener el "click" presionado virtualmente.

Me dice "Necesitas el Atril en la MANO SECUNDARIA":

Pon el atril en tu mano izquierda (Off-hand). El mod usa la mano derecha para romper (Hacha) y la izquierda para poner (Atril).

El comando da error en rojo:

Usa el autocompletado con TAB. El formato interno debe ser exacto (ej. minecraft:mending).
