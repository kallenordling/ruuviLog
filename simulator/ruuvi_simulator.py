#!/usr/bin/env python3
"""
RuuviTag BLE Simulator
Advertises fake RuuviTag Data Format 5 over Bluetooth LE using BlueZ D-Bus API.
The RuuviLog Android app (or any Ruuvi-compatible scanner) will detect this as a real tag.

Requirements: python3-dbus, python3-gi
  sudo apt install python3-dbus python3-gi
"""

import dbus
import dbus.exceptions
import dbus.mainloop.glib
import dbus.service
import math
import random
import struct
import sys
import time
from gi.repository import GLib

BLUEZ_SERVICE     = "org.bluez"
ADV_MANAGER_IFACE = "org.bluez.LEAdvertisingManager1"
ADV_IFACE         = "org.bluez.LEAdvertisement1"
DBUS_PROP_IFACE   = "org.freedesktop.DBus.Properties"
DBUS_OM_IFACE     = "org.freedesktop.DBus.ObjectManager"

RUUVI_MFR_ID = 0x0499
ADV_PATH     = "/com/nordling/ruuvisim/adv0"


def find_adapter(bus):
    om = dbus.Interface(bus.get_object(BLUEZ_SERVICE, "/"), DBUS_OM_IFACE)
    for path, ifaces in om.GetManagedObjects().items():
        if ADV_MANAGER_IFACE in ifaces:
            return path
    return None


def encode_df5(temp_c, humidity_pct, pressure_pa,
               accel_x=0, accel_y=0, accel_z=1000,
               battery_mv=2950, tx_dbm=4, movement=0, sequence=0):
    temp_raw  = max(-32767, min(32767, round(temp_c * 200)))
    hum_raw   = max(0, min(65534, round(humidity_pct * 400)))
    pres_raw  = max(0, min(65534, round(pressure_pa) - 50000))
    power_raw = ((battery_mv - 1600) << 5) | ((tx_dbm + 40) // 2)

    data = struct.pack(">BhHHhhhHBH",
        5, temp_raw, hum_raw, pres_raw,
        accel_x, accel_y, accel_z,
        power_raw, movement, sequence,
    )
    data += bytes([0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01])  # fake MAC
    return data


class RuuviAdvertisement(dbus.service.Object):

    def __init__(self, bus):
        dbus.service.Object.__init__(self, bus, ADV_PATH)
        self._start  = time.time()
        self._seq    = 0
        self._move   = 0

    def _build_payload(self):
        t = time.time() - self._start
        temp     = 22.5  + 3.0  * math.sin(t / 60.0)
        humidity = 55.0  + 10.0 * math.sin(t / 90.0)
        pressure = 101325 + 200 * math.sin(t / 120.0)
        battery  = max(1600, 2950 - int(t / 3600))
        accel_z  = 1000 + random.randint(-5, 5)
        self._seq = (self._seq + 1) % 65535
        return encode_df5(
            temp_c=round(temp, 3),
            humidity_pct=round(humidity, 3),
            pressure_pa=round(pressure),
            accel_z=accel_z,
            battery_mv=battery,
            sequence=self._seq,
        )

    def _props(self):
        payload = self._build_payload()
        # variant_level=1 is required so dbus-python wraps the array as a
        # D-Bus variant inside the ManufacturerData dictionary.
        mfr_array = dbus.Array(
            [dbus.Byte(b) for b in payload],
            signature='y',
            variant_level=1,
        )
        return {
            "Type": dbus.String("peripheral"),
            "ManufacturerData": dbus.Dictionary(
                {dbus.UInt16(RUUVI_MFR_ID): mfr_array},
                signature='qv',
            ),
        }

    @dbus.service.method(DBUS_PROP_IFACE, in_signature="s", out_signature="a{sv}")
    def GetAll(self, interface):
        if interface != ADV_IFACE:
            raise dbus.exceptions.DBusException("No such interface: " + interface)
        return self._props()

    @dbus.service.method(ADV_IFACE)
    def Release(self):
        print("\nAdvertisement released by BlueZ.")


def main():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SystemBus()

    adapter_path = find_adapter(bus)
    if not adapter_path:
        print("ERROR: No BLE-capable Bluetooth adapter found.")
        sys.exit(1)

    print(f"Using adapter: {adapter_path}")

    adv_mgr = dbus.Interface(
        bus.get_object(BLUEZ_SERVICE, adapter_path),
        ADV_MANAGER_IFACE,
    )

    adv = RuuviAdvertisement(bus)
    loop = GLib.MainLoop()

    def on_ok():
        print("RuuviTag simulator is advertising!")
        print("Open RuuviLog on your phone and press Start Scan.\n")

        def tick():
            t = time.time() - adv._start
            temp     = 22.5  + 3.0  * math.sin(t / 60.0)
            humidity = 55.0  + 10.0 * math.sin(t / 90.0)
            pressure = 101325 + 200 * math.sin(t / 120.0)
            print(
                f"\r  Temp: {temp:+.2f} °C  "
                f"Humidity: {humidity:.1f} %%  "
                f"Pressure: {pressure:.0f} Pa  "
                f"Seq: {adv._seq}    ",
                end="", flush=True,
            )
            return True  # keep repeating

        GLib.timeout_add_seconds(2, tick)

    def on_err(e):
        print(f"Failed to register advertisement: {e}")
        loop.quit()

    adv_mgr.RegisterAdvertisement(
        adv.object_path, {},
        reply_handler=on_ok,
        error_handler=on_err,
    )

    try:
        print("Starting RuuviTag BLE simulator… Press Ctrl+C to stop.\n")
        loop.run()
    except KeyboardInterrupt:
        print("\nStopping.")
        try:
            adv_mgr.UnregisterAdvertisement(adv.object_path)
        except Exception:
            pass


if __name__ == "__main__":
    main()
