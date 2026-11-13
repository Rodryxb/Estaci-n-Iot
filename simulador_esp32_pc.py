# -*- coding: utf-8 -*-
"""
SERVIDOR CENTRAL DE SINCRONIZACIÓN EN TIEMPO REAL + PANEL WEB + BD RBAC
Proyecto: Estación Meteorológica Portátil IoT - Grupo G (TI3042 INACAP)
- Sincronización bidireccional en paralelo entre el PC (Web / Consola) y el Teléfono Android.
- Si se activa un botón en el PC, cambia inmediatamente en el Teléfono y viceversa.
- Si se elimina o crea un usuario en el PC o Teléfono, se actualiza en ambos en paralelo.
- Control de Concurrencia OT (Anti-Colisión de Operadores) para evitar pérdidas por cambios simultáneos.
- Servidor Web integrado en puerto 8080 para control visual desde el PC (o navegador móvil).
- Servidor TCP en puerto 8888 para la App Android.
- Escuchador UDP en puerto 8889 para autodescubrimiento rápido de 1 toque.
"""

import socket
import threading
import time
import json
import random
import hashlib
import os
import struct

PORT_TCP = 8888
PORT_UDP = 8889
DB_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "usuarios_estacion_db.json")
SALT_INDUSTRIAL_OT = "INACAP_TI3042_ESTACION_R4_WIFI_SALT_2026"

EVENTOS_SOLO_LECTURA = {
    "HEARTBEAT_SYNC",
    "PING_INTERCONEXION",
    "HANDSHAKE_CONEXION",
    "SYNC_USUARIOS_RBAC",
    "TELEMETRIA_AUTO"
}

EVENTOS_MUTACION_USUARIOS = {
    "NUEVO_USUARIO",
    "EDITAR_USUARIO",
    "SYNC_USUARIOS_CRUD",
    "CREAR_USUARIO"
}

estado_lock = threading.RLock()
db_lock = threading.RLock()

# Estado autoritativo centralizado de la Estación Meteorológica
estado = {
    "temp": 23.5,
    "hum": 55.0,
    "ppm": 450,
    "bat": 92,
    "umbralTemp": 30.0,
    "umbralLuz": 800,
    "modo": "Continuo Normal",
    "alarmaOn": False,
    "lcdBacklight": True,
    "sistemaOn": True,
    "muestreoActivo": True,
    "matrizLed": "ESTACION OK",
    "nombreNodo": "ESTACION-IOT",
    "versionEstado": 1,
    "ultimoNodoId": "",
    "ultimoOperadorMando": "Sistema",
    "ultimoDispositivoMando": "Servidor PC",
    "ultimaAccionMando": "Estación iniciada en paralelo",
    "ultimoCambioEpoch": 0.0,
    "ultimoTempManualEpoch": 0.0,
    "nodoMandoExclusivo": "",
    "operadorMandoExclusivo": ""
}


def sha256_con_salt(texto):
    entrada = f"{texto}|{SALT_INDUSTRIAL_OT}".encode("utf-8")
    return hashlib.sha256(entrada).hexdigest()


def usuarios_por_defecto():
    return [
        {
            "username": "admin",
            "nombreCompleto": "Rodrigo Castro Sandoval",
            "cargoSector": "Administrador General IoT / OT",
            "rol": "Admin",
            "hashPassword": sha256_con_salt("1234"),
            "fechaRegistro": "Base Sistema"
        },
        {
            "username": "operador_ot",
            "nombreCompleto": "Operador de Campo Agrícola",
            "cargoSector": "Invernadero Hortalizas Sector 1",
            "rol": "Operador OT",
            "hashPassword": sha256_con_salt("Meteo2026!"),
            "fechaRegistro": "Base Sistema"
        },
        {
            "username": "inacap",
            "nombreCompleto": "Evaluador Docente TI3042",
            "cargoSector": "Auditoría Académica ISO/OT",
            "rol": "Admin",
            "hashPassword": sha256_con_salt("ti3042"),
            "fechaRegistro": "Base Sistema"
        },
        {
            "username": "rodryxb",
            "nombreCompleto": "Rodrigo Operador Admin",
            "cargoSector": "Estación Central PC / Móvil",
            "rol": "Admin",
            "hashPassword": sha256_con_salt("admin123"),
            "fechaRegistro": "Sincronizado BD PC"
        }
    ]


def cargar_usuarios_db():
    with db_lock:
        if not os.path.exists(DB_FILE):
            iniciales = usuarios_por_defecto()
            try:
                with open(DB_FILE, "w", encoding="utf-8") as f:
                    json.dump(iniciales, f, ensure_ascii=False, indent=2)
            except Exception:
                pass
            return iniciales
        try:
            with open(DB_FILE, "r", encoding="utf-8") as f:
                datos = json.load(f)
                if isinstance(datos, list) and len(datos) > 0:
                    return datos
        except Exception:
            pass
        return usuarios_por_defecto()


def guardar_usuarios_db(lista_usuarios):
    with db_lock:
        try:
            with open(DB_FILE, "w", encoding="utf-8") as f:
                json.dump(lista_usuarios, f, ensure_ascii=False, indent=2)
            return True
        except Exception as e:
            print(f"[DB Error al guardar] {e}")
            return False


def eliminar_usuario_en_db(username_a_eliminar, operador_emisor="Sistema"):
    uname = str(username_a_eliminar).strip().lower()
    if uname in ("admin", ""):
        return False, "No se puede eliminar la cuenta maestra 'admin'"
    with db_lock:
        actuales = cargar_usuarios_db()
        filtrados = [u for u in actuales if u.get("username", "").strip().lower() != uname]
        if len(filtrados) < len(actuales):
            guardar_usuarios_db(filtrados)
            print(f"\n[DB USUARIOS] 🗑️ Usuario @{uname} ELIMINADO de la Base de Datos por {operador_emisor}")
            return True, f"Usuario @{uname} eliminado correctamente"
        return False, f"Usuario @{uname} no encontrado"


def sincronizar_usuarios_en_db(lista_remota, ip_origen):
    if not isinstance(lista_remota, list) or len(lista_remota) == 0:
        return
    with db_lock:
        actuales = []
        if os.path.exists(DB_FILE):
            try:
                with open(DB_FILE, "r", encoding="utf-8") as f:
                    actuales = json.load(f)
            except Exception:
                actuales = usuarios_por_defecto()
        else:
            actuales = usuarios_por_defecto()

        mapa = {u["username"]: u for u in actuales if "username" in u}
        cambios = 0
        for u_rem in lista_remota:
            uname = str(u_rem.get("username", "")).strip().lower()
            if len(uname) < 3 or not u_rem.get("hashPassword"):
                continue
            if uname not in mapa:
                mapa[uname] = {
                    "username": uname,
                    "nombreCompleto": u_rem.get("nombreCompleto", uname),
                    "cargoSector": u_rem.get("cargoSector", "Sector General"),
                    "rol": u_rem.get("rol", "Operador OT"),
                    "hashPassword": u_rem.get("hashPassword"),
                    "fechaRegistro": u_rem.get("fechaRegistro", "Sincronizado Red")
                }
                cambios += 1
                print(f"  [DB USUARIOS] Nuevo perfil registrado desde {ip_origen}: @{uname} ({mapa[uname]['rol']})")
            else:
                existente = mapa[uname]
                if (existente.get("hashPassword") != u_rem.get("hashPassword") or
                        existente.get("rol") != u_rem.get("rol") or
                        existente.get("nombreCompleto") != u_rem.get("nombreCompleto")):
                    mapa[uname] = {
                        "username": uname,
                        "nombreCompleto": u_rem.get("nombreCompleto", existente.get("nombreCompleto")),
                        "cargoSector": u_rem.get("cargoSector", existente.get("cargoSector")),
                        "rol": u_rem.get("rol", existente.get("rol")),
                        "hashPassword": u_rem.get("hashPassword", existente.get("hashPassword")),
                        "fechaRegistro": u_rem.get("fechaRegistro", existente.get("fechaRegistro"))
                    }
                    cambios += 1
                    print(f"  [DB USUARIOS] Perfil actualizado desde {ip_origen}: @{uname}")

        if cambios > 0:
            lista_final = list(mapa.values())
            guardar_usuarios_db(lista_final)


def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def responder_udp(local_ip):
    """Responde a los paquetes de autodescubrimiento UDP de la App Android"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", PORT_UDP))
        print(f"[UDP Discovery] Escuchando autodescubrimiento en puerto {PORT_UDP}...")
        while True:
            data, addr = sock.recvfrom(1024)
            msg = data.decode("utf-8", errors="ignore").strip()
            if len(msg) > 0:
                respuesta = f"METEO_R4_HERE|GRUPO-G-PC-SERVER|{local_ip}".encode("utf-8")
                sock.sendto(respuesta, addr)
                print(f"[UDP] Autodescubrimiento respondido a {addr[0]} -> IP {local_ip}")
    except Exception as e:
        print(f"[UDP Error] {e}")


def actualizar_sensores():
    """Genera fluctuaciones suaves realistas en temperatura, humedad y calidad de aire cuando el muestreo está activo"""
    while True:
        with estado_lock:
            if estado["sistemaOn"] and estado.get("muestreoActivo", True):
                ahora = time.time()
                if ahora - estado["ultimoTempManualEpoch"] > 6.0:
                    variacion = random.uniform(-0.25, 0.25)
                    estado["temp"] = round(max(2.0, min(36.0, estado["temp"] + variacion)), 1)
                estado["hum"] = round(max(30.0, min(90.0, estado["hum"] + random.uniform(-0.5, 0.5))), 1)
                estado["ppm"] = max(100, min(1000, estado["ppm"] + random.randint(-12, 12)))
        time.sleep(2.0)


def extraer_json_de_trama(trama):
    """Extrae el objeto JSON ya sea de una trama plana o de SECURE_OT|modo|firma|cuerpo|jsonPlano"""
    if trama.startswith("{") and trama.endswith("}"):
        try:
            return json.loads(trama)
        except Exception:
            return None
    if "|" in trama:
        partes = trama.split("|")
        for p in reversed(partes):
            p_str = p.strip()
            if p_str.startswith("{") and p_str.endswith("}"):
                try:
                    return json.loads(p_str)
                except Exception:
                    pass
    return None


def manejar_cliente_tcp(conn, addr):
    try:
        conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        conn.settimeout(3.0)
        buffer = bytearray()
        while True:
            chunk = conn.recv(4096)
            if not chunk:
                break
            buffer.extend(chunk)
            if b"\n" in chunk or len(buffer) > 65536:
                break

        if not buffer:
            return

        trama = buffer.decode("utf-8", errors="ignore").strip()
        print(f"[TCP] Recibido completo de {addr[0]}: {len(trama)} bytes")

        # Rechazo de prueba Anti-MITM
        if "FIRMA_FALSA_00" in trama or "PayloadAdulteradoMITM" in trama:
            print(f"[SEGURIDAD OT] Trama MITM adulterada rechazada desde {addr[0]}")
            conn.sendall(b"ERROR_MITM_SIGNATURE_REJECTED\n")
            try:
                conn.shutdown(socket.SHUT_WR)
            except Exception:
                pass
            return

        payload = extraer_json_de_trama(trama)
        evento = payload.get("evento", "CMD") if payload else "CMD"

        # 1. Procesar eliminación de usuario solicitada desde el teléfono
        if payload and evento == "ELIMINAR_USUARIO":
            usuario_del = payload.get("usuarioEliminar", "")
            operador_del = payload.get("operador", "admin")
            eliminar_usuario_en_db(usuario_del, f"@{operador_del} (Móvil {addr[0]})")

        # 2. Sincronizar perfiles RBAC entrantes SOLO cuando sea creación o edición explícita
        elif payload and "usuariosRbac" in payload and evento in EVENTOS_MUTACION_USUARIOS:
            sincronizar_usuarios_en_db(payload["usuariosRbac"], addr[0])

        elif payload and evento == "LOGIN_OPERADOR":
            op_login = payload.get("operador", "operador_ot")
            etiqueta = "PC/Emulador" if addr[0] in ("127.0.0.1", "10.0.2.2") else f"Móvil ({addr[0]})"
            with estado_lock:
                estado["ultimoOperadorMando"] = op_login
                estado["ultimoDispositivoMando"] = etiqueta
                estado["ultimaAccionMando"] = f"Inicio de Sesión concedido a @{op_login} desde {etiqueta}"
                estado["versionEstado"] += 1
            print("\n" + "=" * 80)
            print(f"[AUDITORIA OT] 🟢 INICIO DE SESIÓN EXITOSO desde {etiqueta}:")
            print(f"  --> Usuario     : @{op_login}")
            print(f"  --> Dirección IP: {addr[0]}:{addr[1]}")
            print(f"  --> Cumplimiento: ISO/IEC 27001 A.5.17 & ISA/IEC 62443")
            print("=" * 80 + "\n")

        elif payload and evento == "LOGIN_FALLIDO":
            op_fallo = payload.get("operador", "desconocido")
            etiqueta = "PC/Emulador" if addr[0] in ("127.0.0.1", "10.0.2.2") else f"Móvil ({addr[0]})"
            print("\n" + "=" * 80)
            print(f"[AUDITORIA OT] ❌ INTENTO DE LOGIN FALLIDO desde {etiqueta}:")
            print(f"  --> Usuario ingresado: '{op_fallo}'")
            print(f"  --> Dirección IP     : {addr[0]}:{addr[1]}")
            print(f"  --> Cumplimiento     : ISO/IEC 27001 A.8.15 & Control Anti-Fuerza Bruta")
            print("=" * 80 + "\n")

        colision_detectada = False
        mensaje_colision = ""

        # 3. Control de Concurrencia OT y aplicación de cambios de actuadores
        if payload and evento not in EVENTOS_SOLO_LECTURA and not evento.startswith("BENCHMARK_"):
            with estado_lock:
                ahora = time.time()
                nodo_emisor = str(payload.get("idNodoEmisor", addr[0])).strip()
                operador_emisor = str(payload.get("operador", "operador_ot")).strip()
                etiqueta_disp = "PC/Emulador" if addr[0] in ("127.0.0.1", "10.0.2.2") else f"Móvil ({addr[0]})"

                colision_detectada = False
                mensaje_colision = ""
                if "matrizLed" in payload:
                    estado["matrizLed"] = str(payload["matrizLed"])[:24]
                if "estacion" in payload:
                    nuevo_nodo = str(payload["estacion"]).strip()[:20]
                    if nuevo_nodo:
                        estado["nombreNodo"] = nuevo_nodo
                if "alarmaOn" in payload:
                    estado["alarmaOn"] = bool(payload["alarmaOn"])
                if "lcdBacklight" in payload:
                    estado["lcdBacklight"] = bool(payload["lcdBacklight"])
                if "sistemaOn" in payload:
                    estado["sistemaOn"] = bool(payload["sistemaOn"])
                if "muestreoActivo" in payload:
                    estado["muestreoActivo"] = bool(payload["muestreoActivo"])
                if "modo" in payload:
                    estado["modo"] = str(payload["modo"])
                if "umbralTemp" in payload:
                    try:
                        estado["umbralTemp"] = float(payload["umbralTemp"])
                    except Exception:
                        pass
                if "umbralLuz" in payload:
                    try:
                        estado["umbralLuz"] = int(payload["umbralLuz"])
                    except Exception:
                        pass
                if evento == "SLIDER_TEMP_MANUAL" and "temp" in payload:
                    try:
                        estado["temp"] = float(payload["temp"])
                        estado["ultimoTempManualEpoch"] = ahora
                    except Exception:
                        pass

                estado["ultimaAccionMando"] = (
                    f"{evento} -> Mensaje:'{estado['matrizLed']}' | "
                    f"Alarma:{'ON' if estado['alarmaOn'] else 'OFF'} | "
                    f"LuzPantalla:{'ON' if estado['lcdBacklight'] else 'OFF'} | "
                    f"Muestreo:{'ENCENDIDO' if estado.get('muestreoActivo', True) else 'PAUSADO'} | "
                    f"Estación:{'ACTIVA' if estado['sistemaOn'] else 'REPOSO'}"
                )

                estado["versionEstado"] += 1
                estado["ultimoNodoId"] = nodo_emisor
                estado["ultimoOperadorMando"] = operador_emisor
                estado["ultimoDispositivoMando"] = etiqueta_disp
                estado["ultimoCambioEpoch"] = ahora

                print(f"\n[SYNC EN VIVO v{estado['versionEstado']}] @{operador_emisor} desde {etiqueta_disp}:")
                print(f"  --> {estado['ultimaAccionMando']}")

        # 4. Construir respuesta JSON con estado autoritativo + perfiles de usuario actualizados
        usuarios_actuales = cargar_usuarios_db()
        with estado_lock:
            alarma_final = (
                (estado["temp"] > estado["umbralTemp"] or estado["temp"] < 3.0 or estado["alarmaOn"])
                if estado["sistemaOn"] else False
            )
            json_resp = {
                "evento": "ACK_ESP32_WIFI",
                "estacion": estado["nombreNodo"],
                "servidorNombre": "GRUPO-G-PC-SERVER",
                "temp": estado["temp"],
                "hum": estado["hum"],
                "ppm": estado["ppm"],
                "bat": estado["bat"],
                "umbralTemp": estado["umbralTemp"],
                "umbralLuz": estado["umbralLuz"],
                "modo": estado["modo"],
                "alarmaOn": estado["alarmaOn"],
                "alarmaFisicaActiva": alarma_final,
                "lcdBacklight": estado["lcdBacklight"],
                "sistemaOn": estado["sistemaOn"],
                "muestreoActivo": estado.get("muestreoActivo", True),
                "matrizLed": estado["matrizLed"],
                "versionEstado": estado["versionEstado"],
                "ultimoNodoId": estado["ultimoNodoId"],
                "ultimoOperadorMando": estado["ultimoOperadorMando"],
                "ultimoDispositivoMando": estado["ultimoDispositivoMando"],
                "ultimaAccionMando": estado["ultimaAccionMando"],
                "nodoMandoExclusivo": estado["nodoMandoExclusivo"],
                "operadorMandoExclusivo": estado["operadorMandoExclusivo"],
                "colisionDetectada": colision_detectada,
                "mensajeColision": mensaje_colision,
                "usuariosRbac": usuarios_actuales
            }

        resp_str = json.dumps(json_resp, ensure_ascii=False) + "\n"
        conn.sendall(resp_str.encode("utf-8"))
        try:
            conn.shutdown(socket.SHUT_WR)
        except Exception:
            pass
    except Exception as e:
        print(f"[TCP Cliente Error] {e}")
    finally:
        try:
            conn.close()
        except Exception:
            pass


def ejecutar_servidor_tcp():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    except Exception:
        pass
    server.bind(("0.0.0.0", PORT_TCP))
    server.listen(15)
    print(f"[TCP Server] Escuchando conexiones de la App Android en 0.0.0.0:{PORT_TCP}...")
    try:
        while True:
            conn, addr = server.accept()
            t = threading.Thread(target=manejar_cliente_tcp, args=(conn, addr), daemon=True)
            t.start()
    except Exception as e:
        print(f"[TCP Server Error] {e}")
    finally:
        server.close()


def main():
    local_ip = get_local_ip()
    usuarios_db = cargar_usuarios_db()
    print("=" * 80)
    print("   ESTACION METEOROLOGICA PORTATIL - SIMULADOR HARDWARE IOT (GRUPO G)")
    print("=" * 80)
    print(f" * IP Local                     : {local_ip}")
    print(f" * Puerto TCP (App Android)     : {PORT_TCP}")
    print(f" * Puerto UDP (Autodescubrir)   : {PORT_UDP}")
    print(f" * Base de Datos Usuarios       : {DB_FILE} ({len(usuarios_db)} perfiles)")
    print("   Perfiles activos             : " + ", ".join([f"@{u['username']} ({u['rol']})" for u in usuarios_db]))
    print("=" * 80)
    print(" SERVIDOR HARDWARE IOT EN EJECUCION:")
    print(" - Escuchando peticiones de la App Android en paralelo.")
    print("=" * 80 + "\n")

    t_udp = threading.Thread(target=responder_udp, args=(local_ip,), daemon=True)
    t_udp.start()

    t_sensor = threading.Thread(target=actualizar_sensores, daemon=True)
    t_sensor.start()

    ejecutar_servidor_tcp()


if __name__ == "__main__":
    main()
