# 🤖 AI Personal Assistant Android App

> **Package Name**: `com.mitaoe.shridhar202401040197`  
> **Platform**: Native Android (Android Studio)  
> **Language**: 100% Modern Java (JDK 17)  
> **Design Theme**: Obsidian Cybernetic Dark Theme (`#060E20`, `#0B1326`, Electric Cyan `#38BDF8`, Indigo `#6366F1`)  
> **Status**: Compiled & Verified (`app-debug.apk` ready!)

---

## 🌟 Key Features & Capabilities

### 1. 🔐 User Account & Personalization (Sign Up Flow)
- **Fast Onboarding**: Sign up with just **Name**, **Email**, and **Password**.
- **Dynamic Profile Integration**:
  - Personalized greetings across the app based on the time of day (`"Good morning / afternoon / evening, <Name> 👋"`).
  - Navigation Drawer profile card displaying your name, email, and live status (`● Online • Ready`).
  - Seamless Sign Out / Account Switcher directly from the drawer.
- **Local & Private Storage**: Credentials and preferences stored securely on-device via `PreferenceManager`.

### 2. 💬 AI Chat & Natural Language Action Execution
- **Multi-Provider AI Intelligence**: Switch between **OpenRouter**, **Google Gemini**, **Groq**, **NVIDIA NIM**, and **OpenAI**.
- **Free Model Presets Included**:
  - `meta-llama/llama-3.3-70b-instruct:free` (Llama 3.3 70B - Free)
  - `google/gemini-2.0-flash-exp:free` (Gemini 2.0 Flash - Free)
  - `deepseek/deepseek-r1:free` (DeepSeek R1 Reasoning - Free)
  - `qwen/qwen-2.5-coder-32b-instruct:free` (Qwen 2.5 Coder 32B - Free)
  - `llama-3.3-70b-versatile` (Groq Ultra-Fast)
  - `gemini-2.0-flash` (Google AI)
- **Real-Time Token Streaming**: Watch words appear as the AI generates them (SSE typewriter stream).
- **Natural Language Action Parser**: When you chat or say:
  - *"Remind me to submit project tomorrow at 4 PM"*
  - *"Add task: buy groceries with high priority"*
  - *"Take a note: ideas for startup..."*  
  The assistant automatically parses the intent, priority, and date/time, and schedules the task/reminder in your database with an alert!
- **Voice Mic & Audio Output**:
  - 🎙️ Android `SpeechRecognizer` for hands-free queries.
  - 🔊 Android `TextToSpeech` engine to read responses out loud.
- **Multimodal Attachment**: Attach images from your gallery to discuss with vision models.

---

### 3. ⚡ Top Bar Model Quick-Switcher & Settings Hub
- **Sleek Model Pill**: Tap the top chip (e.g. `⚡ Llama 3.3 70B (Free)`) at any time to open the **Model Selector Bottom Sheet**.
- **Model Pinner & Checkboxes**: In **Settings ⚙️**, choose exactly which models are pinned to your chat switcher.
- **Live "Fetch Models" Button**: Connect your API key and tap *Fetch Available Models* to load real-time models directly from OpenRouter, Groq, or OpenAI!
- **Demo / Fallback Mode**: Even without an API key, the app includes smart conversational fallback responses.

---

### 4. ✅ Tasks & Reminders with Background Alarms
- **Heads-Up Notifications**: Triggers exact alarms via Android `AlarmManager` with sound and vibration.
- **Action Buttons in Notification**:
  - 🔘 **Mark as Done**: Instantly marks the task complete directly from the notification tray.
  - 🔘 **Snooze 10m**: Postpones the alarm by 10 minutes.
- **Priority Badging**: High (Red), Medium (Amber), Low (Green).
- **Filter Tabs**: Filter by *All*, *Pending*, and *Completed*.

---

### 5. 📝 Smart Notes with AI Tools
- **Rich Editor**: Title, content, timestamps.
- **One-Tap AI Actions**:
  - ✨ **Summarize**: Condenses the note into 2-3 key takeaways.
  - 📋 **Extract Tasks**: Converts action points into real tasks in your Tasks tab!
  - ✍️ **Polish Grammar**: Refines tone and clarity.
- **Instant Search**: Real-time keyword filter across your notes.

---

### 6. 📊 Daily AI Briefing Dashboard
- **Morning Summary Card**: Generates an intelligent morning overview based on your pending tasks and schedule.
- **Voice Briefing Playback**: Listen to your morning summary out loud with 1 tap.
- **Quick Counters**: Tasks Completed, Pending Reminders, Active Notes, Models Configured.
- **Today's Priorities**: Quick view of urgent items due today.

---

## 🚀 How to Open & Run in Android Studio

1. **Launch Android Studio**.
2. Select **File > Open...** and navigate to:  
   `C:\Users\Shridhar\.gemini\antigravity\scratch\AIPersonalAssistant`
3. Wait for Gradle Sync to complete.
4. Select your device or emulator and click **Run (Shift + F10)**.

---

## 🔑 How to Add Free API Keys

1. Tap the **Settings icon (⚙️)** in the top right.
2. Select your provider:
   - **OpenRouter**: Get a free key at [openrouter.ai](https://openrouter.ai/keys) to use all `:free` models.
   - **Google Gemini**: Get a free key at [aistudio.google.com](https://aistudio.google.com).
   - **Groq**: Get a free ultra-fast key at [console.groq.com](https://console.groq.com).
3. Paste the key and tap **Fetch Available Models** or check your favorite free models.
4. Tap **Save** and enjoy!
