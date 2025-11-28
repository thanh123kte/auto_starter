## 🌟 Overview

**AutoStart App** is an Android TV Box utility that **automatically launches YouTube** as soon as the device is powered on or rebooted.

The app is designed for scenarios like:

- Mobile / on-site karaoke service using YouTube
- Family karaoke systems that want a **simple, one-step start** (just power on the box)
- Households that want to **centralize control** of what users watch/sing through a home system

Instead of letting users freely browse apps on the TV box, AutoStart App ensures the device always boots directly into **YouTube**, where it can be connected to an existing **home karaoke / audio system** for easier management and control.

---

## 🔧 Key Features

- 🚀 **Auto-launch on boot**  
  Automatically starts YouTube when the Android TV Box is powered on or rebooted.

- 🎤 **Optimized for mobile karaoke service**  
  Designed to support **portable/home karaoke setups** that rely on YouTube as the main content source.

- 🎛️ **Easy control from family system**  
  The TV box can be set up to connect to:
  - A home audio system  
  - A central karaoke controller (phone/tablet/PC)  
  → making it easier for parents/owners to control content and usage.

- ⏱️ Optional **startup delay** (if configured)  
  Wait a few seconds after boot before launching YouTube to make sure the system is ready.

- 🖥️ Android TV Box friendly  
  Works on common Android TV Boxes (e.g. FPT, Amlogic-based devices, etc.) that allow apps to receive boot events.

---

## 🎯 Use Case: Mobile Karaoke for Families

Imagine this flow:

1. You bring a **mobile karaoke service** to a customer’s house.
2. You plug in your **Android TV Box** that has AutoStart App installed.
3. When the TV and box are powered:
   - The box boots
   - AutoStart App automatically launches **YouTube**
   - The TV is instantly ready as a karaoke screen.
4. The YouTube session connects to the family’s or service’s **existing audio / karaoke system**, where usage and content can be controlled centrally.

No need to explain to users:
- How to find YouTube
- How to navigate the TV interface
- How to open the right app each time  

Everything is **automatic and consistent**.

---

## 📱 Technical Overview

- **Platform:** Android TV / Android TV Box  
- **Target:** Android 7.0 (API 24) and above (depending on device)  
- **Language:** Kotlin (app-side)  
- **Main behavior:**
  - Listens to **boot completed** broadcast intents
  - Starts the YouTube app automatically when the system is ready

> ⚠️ Note: Exact behavior may vary depending on the TV Box vendor (some custom ROMs restrict boot receivers or autostart apps).

---

## APP OVERVIEW : 
<img width="1111" height="640" alt="{BE2564F1-5E14-4A18-87CF-B49CB2C74FB8}" src="https://github.com/user-attachments/assets/5a234aeb-b895-4613-a51e-f15537cfbb6d" />
<img width="1112" height="654" alt="{09739FFB-0D9E-4010-8FAD-1E7A621D8236}" src="https://github.com/user-attachments/assets/f502e791-ea30-4f54-a8b0-366bbaa3a24f" />
<img width="1122" height="647" alt="{1CDCB22C-70DB-4BD3-87F5-55FE499A534E}" src="https://github.com/user-attachments/assets/05b3ae0d-5e1f-4e94-882b-59b73f0ddd5c" />
<img width="1108" height="647" alt="{114A77B9-316C-4A20-AB0C-D2412C2E1442}" src="https://github.com/user-attachments/assets/7feec94b-396b-47fb-95e3-0e9988756111" />
When boot receiver received : 
<img width="1103" height="654" alt="{1D4A68F4-836F-409A-B499-DA49CF20432C}" src="https://github.com/user-attachments/assets/6a196944-5634-45fd-96e8-09e0928a029d" />



