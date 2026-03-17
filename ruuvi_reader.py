#!/usr/bin/env python3
"""
RuuviTag BLE reader - reads and decodes Ruuvi sensor data.
Supports Data Format 3 (RAWv1) and Data Format 5 (RAWv2).

Requires: pip install bleak
"""

import asyncio
import struct
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from bleak import BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

RUUVI_MANUFACTURER_ID = 0x0499


@dataclass
class RuuviData:
    mac: str
    rssi: int
    timestamp: str
    data_format: int
    temperature: Optional[float] = None
    humidity: Optional[float] = None
    pressure: Optional[float] = None
    accel_x: Optional[int] = None
    accel_y: Optional[int] = None
    accel_z: Optional[int] = None
    battery_v: Optional[float] = None
    tx_power: Optional[int] = None
    movement_counter: Optional[int] = None
    sequence_number: Optional[int] = None

    def __str__(self):
        lines = [
            f"--- RuuviTag {self.mac} [{self.timestamp}] RSSI: {self.rssi} dBm ---",
            f"  Format:      {self.data_format}",
            f"  Temperature: {self.temperature:.2f} °C" if self.temperature is not None else "  Temperature: N/A",
            f"  Humidity:    {self.humidity:.2f} %RH" if self.humidity is not None else "  Humidity:    N/A",
            f"  Pressure:    {self.pressure:.0f} Pa ({self.pressure/100:.2f} hPa)" if self.pressure is not None else "  Pressure:    N/A",
            f"  Accel X/Y/Z: {self.accel_x} / {self.accel_y} / {self.accel_z} mG" if self.accel_x is not None else "  Accel:       N/A",
            f"  Battery:     {self.battery_v:.3f} V" if self.battery_v is not None else "  Battery:     N/A",
        ]
        if self.data_format == 5:
            lines.append(f"  TX Power:    {self.tx_power} dBm" if self.tx_power is not None else "  TX Power:    N/A")
            lines.append(f"  Movement:    {self.movement_counter}" if self.movement_counter is not None else "  Movement:    N/A")
            lines.append(f"  Sequence:    {self.sequence_number}" if self.sequence_number is not None else "  Sequence:    N/A")
        return "\n".join(lines)


def decode_format5(data: bytes, mac: str, rssi: int) -> RuuviData:
    """Decode Ruuvi Data Format 5 (RAWv2)."""
    if len(data) < 24:
        raise ValueError(f"Format 5 requires 24 bytes, got {len(data)}")

    temp_raw = struct.unpack(">h", data[1:3])[0]
    hum_raw = struct.unpack(">H", data[3:5])[0]
    pres_raw = struct.unpack(">H", data[5:7])[0]
    acc_x = struct.unpack(">h", data[7:9])[0]
    acc_y = struct.unpack(">h", data[9:11])[0]
    acc_z = struct.unpack(">h", data[11:13])[0]
    power_raw = struct.unpack(">H", data[13:15])[0]
    movement = data[15]
    seq = struct.unpack(">H", data[16:18])[0]

    battery_mv = (power_raw >> 5) + 1600
    tx_power = (power_raw & 0x1F) * 2 - 40

    return RuuviData(
        mac=mac,
        rssi=rssi,
        timestamp=datetime.now().isoformat(timespec="seconds"),
        data_format=5,
        temperature=temp_raw / 200.0 if temp_raw != 0x8000 else None,
        humidity=hum_raw / 400.0 if hum_raw != 0xFFFF else None,
        pressure=pres_raw + 50000 if pres_raw != 0xFFFF else None,
        accel_x=acc_x if acc_x != 0x8000 else None,
        accel_y=acc_y if acc_y != 0x8000 else None,
        accel_z=acc_z if acc_z != 0x8000 else None,
        battery_v=battery_mv / 1000.0 if power_raw != 0xFFFF else None,
        tx_power=tx_power if power_raw != 0xFFFF else None,
        movement_counter=movement if movement != 0xFF else None,
        sequence_number=seq if seq != 0xFFFF else None,
    )


def decode_format3(data: bytes, mac: str, rssi: int) -> RuuviData:
    """Decode Ruuvi Data Format 3 (RAWv1)."""
    if len(data) < 14:
        raise ValueError(f"Format 3 requires 14 bytes, got {len(data)}")

    hum_raw = data[1]
    temp_int = struct.unpack(">b", data[2:3])[0]
    temp_frac = data[3]
    pres_raw = struct.unpack(">H", data[4:6])[0]
    acc_x = struct.unpack(">h", data[6:8])[0]
    acc_y = struct.unpack(">h", data[8:10])[0]
    acc_z = struct.unpack(">h", data[10:12])[0]
    batt_mv = struct.unpack(">H", data[12:14])[0]

    return RuuviData(
        mac=mac,
        rssi=rssi,
        timestamp=datetime.now().isoformat(timespec="seconds"),
        data_format=3,
        temperature=temp_int + temp_frac / 100.0,
        humidity=hum_raw * 0.5,
        pressure=pres_raw + 50000,
        accel_x=acc_x,
        accel_y=acc_y,
        accel_z=acc_z,
        battery_v=batt_mv / 1000.0,
    )


def parse_ruuvi_advertisement(device: BLEDevice, adv: AdvertisementData) -> Optional[RuuviData]:
    """Extract and decode Ruuvi manufacturer data from a BLE advertisement."""
    mfr_data = adv.manufacturer_data.get(RUUVI_MANUFACTURER_ID)
    if mfr_data is None:
        return None

    if len(mfr_data) < 1:
        return None

    data_format = mfr_data[0]

    try:
        if data_format == 5:
            return decode_format5(mfr_data, device.address, adv.rssi)
        elif data_format == 3:
            return decode_format3(mfr_data, device.address, adv.rssi)
        else:
            print(f"  [Unknown format {data_format} from {device.address}]")
            return None
    except Exception as e:
        print(f"  [Parse error from {device.address}: {e}]")
        return None


seen_devices: set = set()


def detection_callback(device: BLEDevice, adv: AdvertisementData):
    ruuvi = parse_ruuvi_advertisement(device, adv)
    if ruuvi:
        print(ruuvi)
        print()
        seen_devices.add(device.address)


async def scan(duration: int = 30):
    print(f"Scanning for RuuviTags for {duration} seconds...\n")
    scanner = BleakScanner(detection_callback=detection_callback)
    await scanner.start()
    await asyncio.sleep(duration)
    await scanner.stop()
    print(f"\nScan complete. Found {len(seen_devices)} RuuviTag(s): {', '.join(seen_devices) or 'none'}")


if __name__ == "__main__":
    asyncio.run(scan(duration=30))
