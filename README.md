# Intelbras Solar Virtual Power/Energy Sensor (Hubitat Driver)

This Hubitat driver integrates with the [Intelbras Solar Virtual Monitor](http://solar-monitoramento.intelbras.com.br), enabling you to monitor real-time power and energy data from your solar energy system. It fetches data via HTTP requests and displays metrics such as current power output, energy generated today, and energy generated this month.

---

## 📦 Features

- Reports **instantaneous power (W)** and **daily energy (kWh)** via standard capabilities:
  - `PowerMeter`
  - `EnergyMeter`
- Exposes additional custom attributes:
  - `energyThisMonth` — Monthly energy generation (kWh)
  - `nominalPower` — Total nominal power of inverters (W)
  - `inverters` — Number of inverters found
  - `lastUpdate` — Timestamp of last data refresh
- Performs **scheduled polling** at a configurable interval
- Supports **session-based authentication via cookies**

---

## ⚙️ Installation

1. In Hubitat, go to **Drivers Code** and paste the driver code.
2. Save and create a new **Virtual Device** using this driver.
3. Enter your Intelbras account credentials and plant ID.

---

## 🧾 Preferences

| Setting            | Description                                                   |
|--------------------|---------------------------------------------------------------|
| `API Username`      | Your login username for Intelbras Solar Virtual              |
| `API Password`      | Your login password (⚠️ stored in plain text)               |
| `Plant ID`          | The ID of the plant to monitor (optional if only one plant)  |
| `Poll Interval`     | How often (in minutes) to refresh data from the API          |
| `Debug Logging`     | Enable or disable debug logs                                 |

---

## 🔄 What It Does

On each scheduled poll:
1. Logs in to the Intelbras Solar Virtual API.
2. Fetches total daily energy from `/getInvTotalData`.
3. Retrieves a list of inverters from `/getDevicesByPlantList`.
4. Aggregates:
   - `power` = sum of inverter output power (`pac`)
   - `nominalPower` = sum of inverter nominal power
   - `energyThisMonth` = total monthly energy
5. Updates attributes in Hubitat with the latest data.

---

## 🔐 Notes on Security

- Passwords are stored in **open text** in Hubitat preferences — use only in trusted environments.
- Session management is handled via **cookies** retrieved during login.
- Session cookies are reused for subsequent API calls until expiration.

---

## 🧠 Troubleshooting

- Check **Logs** for errors or authentication failures.
- Ensure your Plant ID is correct if you have multiple plants.
- If the API changes, this driver may require updates to continue working.

---

## 📅 Attribute Overview

| Attribute           | Type    | Description                            |
|--------------------|---------|----------------------------------------|
| `power`             | number  | Current instantaneous power (W)        |
| `energy`            | number  | Energy generated today (kWh)           |
| `energyThisMonth`   | number  | Total monthly energy generation (kWh)  |
| `nominalPower`      | number  | Total nominal capacity of inverters (W)|
| `inverters`         | number  | Number of inverters detected           |
| `lastUpdate`        | string  | Last data update timestamp             |

---

## 👨‍💻 Author

Developed by **Victor Tortorello Neto**

Namespace: `tortorello.intelbras`

---

## 📝 License

This code is provided "as is" with no warranty. Use it at your own risk. Not affiliated with Intelbras.

