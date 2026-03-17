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

BLUEZ_SERVICE = "org.bluez"
ADAPTER_IFACE = "org.bluez.Adapter1"
ADV_MANAGER_IFACE = "org.bluez.LEAdvertisingManager1"
ADV_IFACE = "org.bluez.LEAdvertisement1"
DBUS_PROP_IFACE = "org.freedesktop.DBus.Properties"

RUUVI_MANUFACTURER_ID = 0x0499
ADV_PATH = "/com/nordling/ruuvisim/advertisement0"


def find_adapter(bus):
    remote_om = dbus.Interface(bus.get_object(BLUEZ_SERVICE, "/"), "org.freedesktop.DBus.ObjectManager")
    objects = remote_om.GetManagedObjects()
    for path, ifaces in objects.items():
        if ADV_MANAGER_IFACE in ifaces:
            return path
    return None


def encode_df5(temp_c, humidity_pct, pressure_pa, accel_x=0, accel_y=0, accel_z=1000,
               battery_mv=2950, tx_dbm=4, movement=0, sequence=0):
    """Encode sensor values into Ruuvi Data Format 5 bytes."""
    temp_raw = round(temp_c * 200)
    hum_raw = round(humidity_pct * 400)
    pres_raw = round(pressure_pa) - 50000
    power_raw = ((battery_mv - 1600) << 5) | ((tx_dbm + 40) // 2)

    # Clamp values to valid ranges
    temp_raw = max(-32767, min(32767, temp_raw))
    hum_raw = max(0, min(65534, hum_raw))
    pres_raw = max(0, min(65534, pres_raw))

    payload = struct.pack(
        ">BhHHhhhHBH",
        5,            # data format
        temp_raw,
        hum_raw,
        pres_raw,
        accel_x,
        accel_y,
        accel_z,
        power_raw,
        movement,
        sequence,
    )
    # Append a fake MAC address (6 bytes)
    payload += bytes([0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01])
    return payload


class RuuviAdvertisement(dbus.service.Object):

    def __init__(self, bus, index):
        self.path = ADV_PATH
        self.bus = bus
        self.ad_type = "peripheral"
        self._sequence = 0
        self._movement = 0
        self._start_time = time.time()
        dbus.service.Object.__init__(self, bus, self.path)

    def _get_manufacturer_data(self):
        elapsed = time.time() - self._start_time

        # Simulate realistic sensor values with gentle variation
        temperature = 22.5 + 3.0 * math.sin(elapsed / 60.0)       # ±3°C over 60 s cycle
        humidity = 55.0 + 10.0 * math.sin(elapsed / 90.0)          # ±10% over 90 s
        pressure = 101325 + 200 * math.sin(elapsed / 120.0)        # ±200 Pa over 120 s
        battery_mv = 2950 - int(elapsed / 3600)                     # slow drain
        accel_z = 1000 + random.randint(-5, 5)                      # slight noise

        self._sequence = (self._sequence + 1) % 65535
        payload = encode_df5(
            temp_c=round(temperature, 3),
            humidity_pct=round(humidity, 3),
            pressure_pa=round(pressure),
            accel_z=accel_z,
            battery_mv=max(1600, battery_mv),
            movement=self._movement,
            sequence=self._sequence,
        )
        return dbus.Array([dbus.Byte(b) for b in payload], signature="y")

    def get_properties(self):
        return {
            ADV_IFACE: {
                "Type": self.ad_type,
                "LocalName": dbus.String("RuuviSimulator"),
                "ManufacturerData": dbus.Dictionary(
                    {dbus.UInt16(RUUVI_MANUFACTURER_ID): self._get_manufacturer_data()},
                    signature="qv",
                ),
                "Includes": dbus.Array(["local-name"], signature="s"),
            }
        }

    def get_path(self):
        return dbus.ObjectPath(self.path)

    @dbus.service.method(DBUS_PROP_IFACE, in_signature="s", out_signature="a{sv}")
    def GetAll(self, interface):
        if interface != ADV_IFACE:
            raise dbus.exceptions.DBusException("No such interface: " + interface)
        return self.get_properties()[ADV_IFACE]

    @dbus.service.method(ADV_IFACE, in_signature="", out_signature="")
    def Release(self):
        print("Advertisement released by BlueZ.")


def update_advertisement(bus, adv, adapter_path, adv_manager):
    """Re-register advertisement to push updated manufacturer data."""
    try:
        adv_manager.UnregisterAdvertisement(adv.get_path())
    except Exception:
        pass

    props = adv.get_properties()[ADV_IFACE]
    elapsed = time.time() - adv._start_time
    temperature = 22.5 + 3.0 * math.sin(elapsed / 60.0)
    humidity = 55.0 + 10.0 * math.sin(elapsed / 90.0)
    pressure = 101325 + 200 * math.sin(elapsed / 120.0)

    print(
        f"\r  Temp: {temperature:+.2f} °C  "
        f"Humidity: {humidity:.1f} %  "
        f"Pressure: {pressure:.0f} Pa  "
        f"Seq: {adv._sequence}    ",
        end="",
        flush=True,
    )

    adv_manager.RegisterAdvertisement(
        adv.get_path(), {},
        reply_handler=lambda: None,
        error_handler=lambda e: print(f"\nRe-register error: {e}"),
    )
    return True  # keep GLib timeout running


def main():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SystemBus()

    adapter_path = find_adapter(bus)
    if not adapter_path:
        print("ERROR: No Bluetooth adapter with LE advertising support found.")
        sys.exit(1)

    print(f"Using adapter: {adapter_path}")

    adv_manager = dbus.Interface(
        bus.get_object(BLUEZ_SERVICE, adapter_path),
        ADV_MANAGER_IFACE,
    )

    adv = RuuviAdvertisement(bus, 0)

    mainloop = GLib.MainLoop()

    def on_register_ok():
        print("RuuviTag simulator is advertising!")
        print("Your phone's RuuviLog app should detect it as a tag.\n")
        # Update advertisement data every 2 seconds
        GLib.timeout_add_seconds(2, update_advertisement, bus, adv, adapter_path, adv_manager)

    def on_register_error(error):
        print(f"Failed to register advertisement: {error}")
        mainloop.quit()

    adv_manager.RegisterAdvertisement(
        adv.get_path(), {},
        reply_handler=on_register_ok,
        error_handler=on_register_error,
    )

    try:
        print("Starting RuuviTag BLE simulator... Press Ctrl+C to stop.\n")
        mainloop.run()
    except KeyboardInterrupt:
        print("\nStopping simulator.")
        try:
            adv_manager.UnregisterAdvertisement(adv.get_path())
        except Exception:
            pass


if __name__ == "__main__":
    main()
