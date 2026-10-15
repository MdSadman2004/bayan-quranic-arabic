/* Bayan — UI strings (en / bn). STR(key, lang, vars) */
(function () {
  "use strict";
  const S = {
    en: {
      "nav.learn": "Learn", "nav.practice": "Practice", "nav.profile": "Profile",
      "onb.p1": "Micro-lessons, 3–5 minutes each",
      "onb.p2": "Real Quranic verses only — each with its context",
      "onb.p3": "Spaced review so the words actually stick",
      "onb.choose": "Choose your language",
      "onb.start": "Start learning",
      "home.goal": "Daily goal", "home.goalXp": "{n} XP", "home.review": "Daily Review",
      "home.due": "{n} items due", "home.allDone": "All caught up",
      "home.practiceWeak": "Practice weakest words", "home.start": "START",
      "home.locked": "Finish the previous lesson first", "home.replay": "Replay",
      "home.lesson": "Lesson {n}", "home.unit": "Unit {n}",
      "lesson.check": "Check", "lesson.continue": "Continue", "lesson.correct": "Correct!",
      "lesson.wrong": "Correct answer:", "lesson.newWord": "New word", "lesson.concept": "Concept",
      "lesson.context": "Context", "lesson.quran": "Qur'an", "lesson.whatMean": "What does this mean?",
      "lesson.chooseAr": "Choose the Arabic", "lesson.matchTitle": "Tap the pairs",
      "lesson.rootQ": "Which root does it grow from?", "lesson.listenQ": "What did you hear?",
      "lesson.buildQ": "Translate into Arabic", "lesson.blankQ": "Fill the blank",
      "lesson.reveal": "Tap to reveal translation", "lesson.play": "Play",
      "lesson.outHearts": "Out of hearts", "lesson.retry": "Try again", "lesson.quit": "Quit",
      "lesson.quitAsk": "Quit this lesson?", "lesson.quitYes": "Quit", "lesson.quitNo": "Keep learning",
      "lesson.xpEarned": "XP earned", "lesson.accuracy": "Accuracy", "lesson.combo": "Best combo",
      "lesson.perfect": "Perfect!", "lesson.unitDone": "Unit complete — bonus +20 XP",
      "lesson.ach": "Achievement unlocked", "lesson.again": "Lesson complete",
      "practice.title": "Practice", "practice.due": "You have {n} items to review",
      "practice.start": "Start review", "practice.empty": "Nothing to review yet",
      "practice.emptyHint": "Finish a lesson first — your words will come back here at the right time.",
      "practice.note": "Spaced review keeps words alive.",
      "profile.title": "Profile", "profile.streak": "Day streak", "profile.xp": "Total XP",
      "profile.gems": "Gems", "profile.words": "Words learned", "profile.mastered": "Mastered",
      "profile.accuracy": "Accuracy", "profile.lessons": "Lessons", "profile.perfects": "Perfect runs",
      "profile.reviews": "Reviews", "profile.coverage": "≈{p}% of Quranic word usage covered (estimate)",
      "profile.achievements": "Achievements", "profile.settings": "Settings",
      "profile.language": "Language", "profile.sound": "Sound effects",
      "profile.ttsOn": "Arabic audio available", "profile.ttsOff": "No Arabic voice found on this device — listen exercises are hidden",
      "profile.reset": "Reset all progress", "profile.resetAsk": "Erase everything and start over?",
      "profile.about": "About",
      "profile.aboutText": "Bayan — a Quranic Arabic micro-learning prototype. Frequency-ordered vocabulary, the root system, real verses with context, spaced review. Word order follows Quranic Arabic Corpus frequency data; context notes summarize classical asbāb al-nuzūl collections and well-attested hadith. Study aid — verify scholarly content before wider use.",
      "common.continue": "Continue", "common.retry": "Retry", "common.complete": "Complete",
      "common.start": "Start", "common.locked": "Locked", "common.min": "min"
    },
    bn: {
      "nav.learn": "শেখা", "nav.practice": "অনুশীলন", "nav.profile": "প্রোফাইল",
      "onb.p1": "মাইক্রো-পাঠ — প্রতিটি ৩–৫ মিনিট",
      "onb.p2": "শুধুই প্রকৃত কুরআনি আয়াত — প্রেক্ষাপটসহ",
      "onb.p3": "স্পেসড রিভিউ — শব্দগুলো সত্যিই মনে থাকে",
      "onb.choose": "ভাষা বেছে নিন",
      "onb.start": "শেখা শুরু করুন",
      "home.goal": "দৈনিক লক্ষ্য", "home.goalXp": "{n} XP", "home.review": "দৈনিক রিভিউ",
      "home.due": "{n}টি শব্দ অপেক্ষায়", "home.allDone": "সব শেষ",
      "home.practiceWeak": "দুর্বল শব্দ অনুশীলন", "home.start": "শুরু",
      "home.locked": "আগে আগের পাঠটি শেষ করুন", "home.replay": "আবার",
      "home.lesson": "পাঠ {n}", "home.unit": "ইউনিট {n}",
      "lesson.check": "যাচাই", "lesson.continue": "চালিয়ে যান", "lesson.correct": "ঠিক!",
      "lesson.wrong": "সঠিক উত্তর:", "lesson.newWord": "নতুন শব্দ", "lesson.concept": "বিষয়",
      "lesson.context": "প্রেক্ষাপট", "lesson.quran": "কুরআন", "lesson.whatMean": "এর অর্থ কী?",
      "lesson.chooseAr": "আরবিটি বেছে নিন", "lesson.matchTitle": "জোড়া মিলিয়ে ট্যাপ করুন",
      "lesson.rootQ": "কোন ধাতু থেকে এসেছে?", "lesson.listenQ": "কী শুনলেন?",
      "lesson.buildQ": "আরবিতে অনুবাদ করুন", "lesson.blankQ": "শূন্যস্থান পূরণ করুন",
      "lesson.reveal": "অনুবাদ দেখতে ট্যাপ করুন", "lesson.play": "শুনুন",
      "lesson.outHearts": "হার্ট শেষ", "lesson.retry": "আবার চেষ্টা করুন", "lesson.quit": "বেরোন",
      "lesson.quitAsk": "এই পাঠ থেকে বেরোবেন?", "lesson.quitYes": "বেরিয়ে যান", "lesson.quitNo": "চালিয়ে যান",
      "lesson.xpEarned": "অর্জিত XP", "lesson.accuracy": "নির্ভুলতা", "lesson.combo": "সেরা কম্বো",
      "lesson.perfect": "নিখুঁত!", "lesson.unitDone": "ইউনিট সম্পূর্ণ — বোনাস +২০ XP",
      "lesson.ach": "অর্জন আনলক হয়েছে", "lesson.again": "পাঠ সম্পন্ন",
      "practice.title": "অনুশীলন", "practice.due": "{n}টি আইটেম রিভিউর অপেক্ষায়",
      "practice.start": "রিভিউ শুরু করুন", "practice.empty": "এখন রিভিউ করার কিছু নেই",
      "practice.emptyHint": "প্রথমে একটি পাঠ শেষ করুন — শব্দগুলো সঠিক সময়ে এখানে ফিরে আসবে।",
      "practice.note": "স্পেসড রিভিউ শব্দগুলো সজীব রাখে।",
      "profile.title": "প্রোফাইল", "profile.streak": "দিনের স্ট্রিক", "profile.xp": "মোট XP",
      "profile.gems": "রত্ন", "profile.words": "শেখা শব্দ", "profile.mastered": "আয়ত্ত",
      "profile.accuracy": "নির্ভুলতা", "profile.lessons": "পাঠ", "profile.perfects": "নিখুঁত রান",
      "profile.reviews": "রিভিউ", "profile.coverage": "কুরআনের শব্দ-ব্যবহারের প্রায় {p}% শেখা হয়েছে (আনুমানিক)",
      "profile.achievements": "অর্জন", "profile.settings": "সেটিংস",
      "profile.language": "ভাষা", "profile.sound": "সাউন্ড ইফেক্ট",
      "profile.ttsOn": "আরবি অডিও পাওয়া গেছে", "profile.ttsOff": "এই ডিভাইসে আরবি ভয়েস নেই — শোনার অনুশীলন লুকানো হয়েছে",
      "profile.reset": "সব অগ্রগতি মুছুন", "profile.resetAsk": "সব মুছে নতুন করে শুরু করবেন?",
      "profile.about": "সম্পর্কে",
      "profile.aboutText": "বয়ান — কুরআনি আরবি শেখার একটি মাইক্রো-লার্নিং প্রোটোটাইপ। ফ্রিকোয়েন্সি-ক্রমে শব্দ, ধাতু-ব্যবস্থা, প্রেক্ষাপটসহ প্রকৃত আয়াত, স্পেসড রিভিউ। শব্দক্রম কুরআনিক আরবিক কর্পাসের ফ্রিকোয়েন্সি-তথ্য অনুসারে; প্রেক্ষাপট ধ্রুপদী আসবাব-আন-নুযুল সংকলন ও সহিহ হাদিসের সারাংশ। পড়াশোনার সহায়িকা — ব্যাপক ব্যবহারের আগে বিষয়বস্তু যাচাই করে নিন।",
      "common.continue": "চালিয়ে যান", "common.retry": "আবার", "common.complete": "সম্পন্ন",
      "common.start": "শুরু", "common.locked": "বন্ধ", "common.min": "মিনিট"
    }
  };
  window.Bayan = window.Bayan || {};
  window.Bayan.STR = function (key, lang, vars) {
    const dict = S[lang] || S.en;
    let s = dict[key] != null ? dict[key] : (S.en[key] != null ? S.en[key] : key);
    if (vars) for (const k in vars) s = s.replace("{" + k + "}", vars[k]);
    return s;
  };
  window.Bayan.TAGLINE = {
    en: "Understand the Quran, in Arabic.",
    bn: "কুরআন বুঝুন, আরবি থেকেই।"
  };
})();
