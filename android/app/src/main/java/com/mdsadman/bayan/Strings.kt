package com.mdsadman.bayan

/** UI strings, mirroring web/js/i18n.js. */
object Strings {
    private val EN = mapOf(
        "nav.learn" to "Learn", "nav.practice" to "Practice", "nav.profile" to "Profile",
        "onb.p1" to "Micro-lessons, 3–5 minutes each",
        "onb.p2" to "Real Quranic verses only — each with its context",
        "onb.p3" to "Spaced review so the words actually stick",
        "onb.choose" to "Choose your language", "onb.start" to "Start learning",
        "home.goal" to "Daily goal", "home.review" to "Daily Review",
        "home.due" to "{n} items due", "home.allDone" to "All caught up",
        "home.practiceWeak" to "Practice weakest words", "home.start" to "START",
        "home.locked" to "Finish the previous lesson first",
        "home.lesson" to "Lesson {n}", "home.unit" to "Unit {n}",
        "lesson.check" to "Check", "lesson.continue" to "Continue", "lesson.correct" to "Correct!",
        "lesson.wrong" to "Correct answer:", "lesson.newWord" to "New word", "lesson.concept" to "Concept",
        "lesson.context" to "Context", "lesson.quran" to "Qur'an", "lesson.whatMean" to "What does this mean?",
        "lesson.chooseAr" to "Choose the Arabic", "lesson.matchTitle" to "Tap the pairs",
        "lesson.rootQ" to "Which root does it grow from?", "lesson.listenQ" to "What did you hear?",
        "lesson.buildQ" to "Translate into Arabic", "lesson.blankQ" to "Fill the blank",
        "lesson.reveal" to "Tap to reveal translation", "lesson.play" to "Play",
        "lesson.outHearts" to "Out of hearts", "lesson.retry" to "Try again", "lesson.quit" to "Quit",
        "lesson.quitAsk" to "Quit this lesson?", "lesson.quitYes" to "Quit", "lesson.quitNo" to "Keep learning",
        "lesson.xpEarned" to "XP earned", "lesson.accuracy" to "Accuracy", "lesson.combo" to "Best combo",
        "lesson.perfect" to "Perfect!", "lesson.unitDone" to "Unit complete — bonus +20 XP",
        "lesson.ach" to "Achievement unlocked", "lesson.again" to "Lesson complete",
        "practice.title" to "Practice", "practice.due" to "You have {n} items to review",
        "practice.start" to "Start review", "practice.empty" to "Nothing to review yet",
        "practice.emptyHint" to "Finish a lesson first — your words will come back here at the right time.",
        "practice.note" to "Spaced review keeps words alive.",
        "profile.title" to "Profile", "profile.streak" to "Day streak", "profile.xp" to "Total XP",
        "profile.gems" to "Gems", "profile.words" to "Words learned", "profile.mastered" to "Mastered",
        "profile.accuracy" to "Accuracy", "profile.lessons" to "Lessons", "profile.perfects" to "Perfect runs",
        "profile.reviews" to "Reviews", "profile.coverage" to "≈{p}% of Quranic word usage covered (estimate)",
        "profile.achievements" to "Achievements", "profile.settings" to "Settings",
        "profile.language" to "Language", "profile.sound" to "Sound effects",
        "profile.ttsOn" to "Arabic audio available",
        "profile.ttsOff" to "No Arabic voice found on this device — listen exercises are hidden",
        "profile.reset" to "Reset all progress", "profile.resetAsk" to "Erase everything and start over?",
        "profile.about" to "About",
        "profile.aboutText" to "Bayan — a Quranic Arabic micro-learning prototype. Frequency-ordered vocabulary, the root system, real verses with context, spaced review. Word order follows Quranic Arabic Corpus frequency data; context notes summarize classical asbāb al-nuzūl collections and well-attested hadith. Study aid — verify scholarly content before wider use.",
        "common.continue" to "Continue", "common.retry" to "Retry", "common.cancel" to "Cancel",
        "common.erase" to "Erase"
    )

    private val BN = mapOf(
        "nav.learn" to "শেখা", "nav.practice" to "অনুশীলন", "nav.profile" to "প্রোফাইল",
        "onb.p1" to "মাইক্রো-পাঠ — প্রতিটি ৩–৫ মিনিট",
        "onb.p2" to "শুধুই প্রকৃত কুরআনি আয়াত — প্রেক্ষাপটসহ",
        "onb.p3" to "স্পেসড রিভিউ — শব্দগুলো সত্যিই মনে থাকে",
        "onb.choose" to "ভাষা বেছে নিন", "onb.start" to "শেখা শুরু করুন",
        "home.goal" to "দৈনিক লক্ষ্য", "home.review" to "দৈনিক রিভিউ",
        "home.due" to "{n}টি শব্দ অপেক্ষায়", "home.allDone" to "সব শেষ",
        "home.practiceWeak" to "দুর্বল শব্দ অনুশীলন", "home.start" to "শুরু",
        "home.locked" to "আগে আগের পাঠটি শেষ করুন",
        "home.lesson" to "পাঠ {n}", "home.unit" to "ইউনিট {n}",
        "lesson.check" to "যাচাই", "lesson.continue" to "চালিয়ে যান", "lesson.correct" to "ঠিক!",
        "lesson.wrong" to "সঠিক উত্তর:", "lesson.newWord" to "নতুন শব্দ", "lesson.concept" to "বিষয়",
        "lesson.context" to "প্রেক্ষাপট", "lesson.quran" to "কুরআন", "lesson.whatMean" to "এর অর্থ কী?",
        "lesson.chooseAr" to "আরবিটি বেছে নিন", "lesson.matchTitle" to "জোড়া মিলিয়ে ট্যাপ করুন",
        "lesson.rootQ" to "কোন ধাতু থেকে এসেছে?", "lesson.listenQ" to "কী শুনলেন?",
        "lesson.buildQ" to "আরবিতে অনুবাদ করুন", "lesson.blankQ" to "শূন্যস্থান পূরণ করুন",
        "lesson.reveal" to "অনুবাদ দেখতে ট্যাপ করুন", "lesson.play" to "শুনুন",
        "lesson.outHearts" to "হার্ট শেষ", "lesson.retry" to "আবার চেষ্টা করুন", "lesson.quit" to "বেরোন",
        "lesson.quitAsk" to "এই পাঠ থেকে বেরোবেন?", "lesson.quitYes" to "বেরিয়ে যান", "lesson.quitNo" to "চালিয়ে যান",
        "lesson.xpEarned" to "অর্জিত XP", "lesson.accuracy" to "নির্ভুলতা", "lesson.combo" to "সেরা কম্বো",
        "lesson.perfect" to "নিখুঁত!", "lesson.unitDone" to "ইউনিট সম্পূর্ণ — বোনাস +২০ XP",
        "lesson.ach" to "অর্জন আনলক হয়েছে", "lesson.again" to "পাঠ সম্পন্ন",
        "practice.title" to "অনুশীলন", "practice.due" to "{n}টি আইটেম রিভিউর অপেক্ষায়",
        "practice.start" to "রিভিউ শুরু করুন", "practice.empty" to "এখন রিভিউ করার কিছু নেই",
        "practice.emptyHint" to "প্রথমে একটি পাঠ শেষ করুন — শব্দগুলো সঠিক সময়ে এখানে ফিরে আসবে।",
        "practice.note" to "স্পেসড রিভিউ শব্দগুলো সজীব রাখে।",
        "profile.title" to "প্রোফাইল", "profile.streak" to "দিনের স্ট্রিক", "profile.xp" to "মোট XP",
        "profile.gems" to "রত্ন", "profile.words" to "শেখা শব্দ", "profile.mastered" to "আয়ত্ত",
        "profile.accuracy" to "নির্ভুলতা", "profile.lessons" to "পাঠ", "profile.perfects" to "নিখুঁত রান",
        "profile.reviews" to "রিভিউ", "profile.coverage" to "কুরআনের শব্দ-ব্যবহারের প্রায় {p}% শেখা হয়েছে (আনুমানিক)",
        "profile.achievements" to "অর্জন", "profile.settings" to "সেটিংস",
        "profile.language" to "ভাষা", "profile.sound" to "সাউন্ড ইফেক্ট",
        "profile.ttsOn" to "আরবি অডিও পাওয়া গেছে",
        "profile.ttsOff" to "এই ডিভাইসে আরবি ভয়েস নেই — শোনার অনুশীলন লুকানো হয়েছে",
        "profile.reset" to "সব অগ্রগতি মুছুন", "profile.resetAsk" to "সব মুছে নতুন করে শুরু করবেন?",
        "profile.about" to "সম্পর্কে",
        "profile.aboutText" to "বয়ান — কুরআনি আরবি শেখার একটি মাইক্রো-লার্নিং প্রোটোটাইপ। ফ্রিকোয়েন্সি-ক্রমে শব্দ, ধাতু-ব্যবস্থা, প্রেক্ষাপটসহ প্রকৃত আয়াত, স্পেসড রিভিউ। শব্দক্রম কুরআনিক আরবিক কর্পাসের ফ্রিকোয়েন্সি-তথ্য অনুসারে; প্রেক্ষাপট ধ্রুপদী আসবাব-আন-নুযুল সংকলন ও সহিহ হাদিসের সারাংশ। পড়াশোনার সহায়িকা — ব্যাপক ব্যবহারের আগে বিষয়বস্তু যাচাই করে নিন।",
        "common.continue" to "চালিয়ে যান", "common.retry" to "আবার", "common.cancel" to "বাতিল",
        "common.erase" to "মুছে ফেলুন"
    )

    fun get(key: String, lang: String, vars: Map<String, Any> = emptyMap()): String {
        val dict = if (lang == "bn") BN else EN
        var s = dict[key] ?: EN[key] ?: key
        for ((k, v) in vars) s = s.replace("{$k}", v.toString())
        return s
    }
}
