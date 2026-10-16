package ru.huegrad.app.data

object Site {
    const val BASE = "https://wirosmango.github.io/huegrad-site"
    const val SERVER_HOST = "mellstroy-gay.ddns.net"
    const val SERVER_IP = "109.196.164.217"
    const val SERVER_VERSION = "1.20.1 Forge"

    fun resName(file: String): String {
        val stem = file.substringBeforeLast('.').lowercase()
        return "m_" + stem.map { if (it in 'a'..'z' || it in '0'..'9') it else '_' }.joinToString("")
    }

    fun media(file: String): String = "android.resource://ru.huegrad.app/drawable/${resName(file)}"
}

data class Admin(val nick: String, val role: String)
data class GalleryItem(val file: String, val caption: String = "") {
    val url: String get() = Site.media(file)
}
data class Track(val title: String, val artist: String, val uri: String, val cover: String? = null)

/** Ссылка на ресурс приложения: res/raw/<name>.mp3 -> resUri("raw", "<name>"). */
fun resUri(type: String, name: String) = "android.resource://ru.huegrad.app/$type/$name"

data class ArticleRef(val id: String, val title: String, val subtitle: String, val path: String) {
    val url: String get() = "${Site.BASE}/$path"
}
data class NewsItem(val time: String, val ref: ArticleRef)
data class DrochkaEntry(val nick: String, val text: String, val image: String)
data class HistoryBlock(val text: String, val images: List<GalleryItem> = emptyList())
data class HistorySection(val title: String, val blocks: List<HistoryBlock>)
data class ModFile(val name: String, val size: String) {
    val url: String get() = "${Site.BASE}/mods/files/${android.net.Uri.encode(name)}"
}

private fun seq(prefix: String, range: IntRange, ext: String, caption: String) =
    range.map { GalleryItem("$prefix$it.$ext", "$caption $it") }

private fun news(time: String, id: String, title: String) =
    NewsItem(time, ArticleRef("news-$id", title, "", "news-pages/$id.html"))

object Content {
    // Описания админов сокращены до ролей: вставь сюда полные тексты с сайта, если нужны.
    val admins = listOf(
        Admin("ruzlan_b560m_h", "администратор сервера, пиздолиз, ходит со скином Навального Лёши, а также юзер сеом 14у, а ещё еблан у которого 3 дома блять (было потому что потом справедливость восторжествовала)"),
        Admin("pur1tym1nd", "Этот ебаный долбоёб с амбициями галактического масштаба, который свою параболу на линукс-либре вертит так, что у Столлмана пузо трясётся как батут, а сам дрочит на матан и системщину до потери пульса, попутно доказывая, что капитализм хуйня, а социализм это когда ты обкуренный в красной шамбале сидишь и нихуя не делаешь. Обкурился до состояния \"я правитель Поволжья\", объявил нижний новгород гипербореей, а всех негров захуярил в шоколадки мистера биста, будто он не хуй с горы, а реинкарнация Ленина в скине. Социалист, расист, антисемит, шовинист и разъебал розмарин, прошил вайред на миуи 12.5, подписал контракт с сундаром пичаи, купил гемини про и думает, что гений, хотя просто долбоёбище с травой в кармане и параболой в заднице. Ассемблер выучил, чтобы руками писать mov eax, ebx и чувствовать себя системным программистом, а теперь ещё и Lua освоил, чтобы через io.write() выводить революционные лозунги без ебаного перевода строки, потому что print это для хуесосов, а настоящие социалисты пишут в поток руками и сами контролируют каждый байт. С долбоебом парте проебал день самоуправления, потому что нахуй он не нужен, когда у тебя гемини про и планы на редактор текста на плюсах, который он никогда не напишет. Напоследок накончал шырозу на лицо потому что почему бы и нет, если ты ультрапидорас и гейлорд, устранитель капитализма и ебучей сои, самый толерантный репрессист и ультрарасист, который ненавидит всех негров и желает им смерти ради шоколадок мистера биста. Пошли нахуй все суки ебаные пидорасы блять, кроме него, он же самый умнейший, социалистичный, архированный, параболический, красный, шамбальный, пичаевский, столлмановский, батутный, гиперборейский, шырозный, гейлордный, ассемблерный, луашный и просто самый ебаный умный колдун на этой планете, который скоро напишет редактор текста на плюсах и докажет всем, что он не хуем деланный, а правитель великой галактической социалистической республики поволжье, который даже io.write(\"я люблю траву\\n\") делает с задором и без единого print"),
        Admin("wiros229", "ибил какой-то, супер крутой веб-разработчик (максимум долбаёб) (вайбкодер), тупоголовый еблан, а также мразь, страдающая от лудки в китайских гачах и дрочащий на пидорасов, погоняло \"Горящий пердак 3000\", розебал 2 мышки за 3 грёбаных часа -2 rampage edit. любит дедушку Нетаньянху, mcdrun user, apple glazer (хотя подписывал контракт и с микрослопом (редизайн результат подписания контракта)), руснявый жид из центральной России, главный противник москальбадии, ультраправый \"либерал\", устранитель гоев, и по совместительству один из ведущих строителей в Хуеграде, области и на всём сервере (так ещё и когда-то спаситель сервера), глава временного собрания республики хуеград, пиздярит конституцию хуеграда в соло (никто не просил), устранил сепарню в жопограде, основатель фбр/армия республики хуеград, ебашит соевых (пусть иногда и сою), стандоф киберспорт 10 киллов (и 17 смертей), выиграл одну катку в бравл дилдо не заходя туда год *куча черепов*, играет с пиздогрызищем в кс сурс, пизданул его с дигла, заорал в войс коч братан (sv_alltalk 0) пока этот ебанат у ящика стоял; ебался с хинером большую часть 2025 года (как будто сейчас нет), учил эту хуемразь линуху (а по итогу сам обосрался и вернулся на шинду (но не полностью)); ультра патриот Израиля, главный фанат биби нетаньяху, крошитель хохлов, санитар русни, хуярю на ужин бульбашей (а на завтрак фалостинских детишек); придумал выклянчивать деньги у буржуев на вдс чтобы этот блядский сайт вообще имел место быть, а хуеград продолжал расти и расти; звонил ваньку по задолженности на 100к рублей (просьба дать станту на бмх); потрошитель антисемитов, верующих обезьян, негров, хохлов и калмуняк (и прочей хуйни левацкой)"),
    )

    val gallery = listOf(
        GalleryItem("huegrad.jpg", "Хуеград"),
        GalleryItem("zovcorp1.jpg", "ZovCorp"),
        GalleryItem("zovcorp2.jpg", "ZovCorp 2"),
        GalleryItem("PTU.png", "ПТУ"),
        GalleryItem("dildovoz.png", "Поезд"),
        GalleryItem("dom_rusika.png", "Дом Русика"),
        GalleryItem("PIZDA.png", "Полнейшая пизда"),
        GalleryItem("banya.png", "Баня"),
        GalleryItem("bayna2.png", "Баня 2"),
    )

    // Треки лежат в app/src/main/res/raw/ (имена файлов: строчные латинские буквы, цифры, _).
    // Новый трек: положи файл в res/raw и добавь строку ниже.
    // Обложка необязательна: положи картинку в res/drawable-nodpi/ и укажи cover = resUri("drawable", "имя").
    val tracks = listOf(
        Track(title = "Гимн Хуеграда", artist = "Huegrad FM", uri = resUri("raw", "gimn")),
        Track(title = "Бабеджон галдааа", artist = "Huegrad FM", uri = resUri("raw", "babedzhon")),
        Track(title = "Анал императора", artist = "ebanat508", uri = resUri("raw", "ebanat508_anal_imperatora")),
        Track(title = "тэцэка", artist = "чувак", uri = resUri("raw", "tcc")),
        Track(title = "коч братан х галда х крутой фонк", artist = "Huegrad FM", uri = resUri("raw", "pochbrataaan")),
        Track(title = "Пидорас и хуеглот", artist = "Huegrad FM", uri = resUri("raw", "xyeglot")),
        Track(title = "Визин", artist = "4K", uri = resUri("raw", "vizin")),
        Track(title = "Навали-ка", artist = "4K, Лакоста, Kima", uri = resUri("raw", "navalika")),
        Track(title = "Милиция", artist = "4K", uri = resUri("raw", "milicija")),
        Track(title = "Кругом тонирован", artist = "АК-47", uri = resUri("raw", "krugomtonirovan")),
        Track(title = "Эндуро рэп", artist = "пятачок", uri = resUri("raw", "endurorap")),
        Track(title = "Великий пидорас (хынер)", artist = "хынерстройность", uri = resUri("raw", "xiner")),
        Track(title = "-51", artist = "Halal FM", uri = resUri("raw", "natowave")),
        Track(title = "В буче было весело", artist = "ВС РФ", uri = resUri("raw", "bucha2022")),
        Track(title = "Медуза gay cover", artist = "хз кто-то", uri = resUri("raw", "pidoruza")),
        Track(title = "Пидор на крыльце", artist = "не помню уже", uri = resUri("raw", "krylco")),
        Track(title = "Подо мной", artist = "Макан (хуесос)", uri = resUri("raw", "makanhuesos")),
        Track(title = "Слёзы осени", artist = "М8л8тх", uri = resUri("raw", "m8l8tx")),
        Track(title = "Kucni Prag", artist = "Roki Vulovic", uri = resUri("raw", "kuchnipgrag")),
        Track(title = "Что ж ты фраер сдал назад", artist = "Михаил Круг", uri = resUri("raw", "fraer")),
        Track(title = "Вижу парня", artist = "пидор", uri = resUri("raw", "bachu")),
        Track(title = "Мне похуй нахуй", artist = "DEE-1", uri = resUri("raw", "pohuinahui")),
        Track(title = "Нарезка кебабов", artist = "бошнякорезы", uri = resUri("raw", "strongkebab")),
        Track(title = "Показушник", artist = "ровный пацанчик", uri = resUri("raw", "rovnyj"))


    )

    val drochka = listOf(
        DrochkaEntry("ruzlan_b560m_h", "Очень любит фембоев, а также ситлали, жиресу и коломбину", "rusik.png"),
        DrochkaEntry("pur1tym1nd", "Ооооооочень любит кошачьих и тралэфасi, меймей, далбаеб конченый, LaTeX, ZIG, а также шиндофоны", "xiner.png"),
        DrochkaEntry("wiros229", "Любит наналли, бат и пидоров с огромными сисяндрами", "wirozzz.png"),
    )

    val news = listOf(
        news("22:15", "zhopograd1", "Построена администрация Жопограда"),
        news("23:40", "lczhopa", "Построен ЖК Жопа"),
        news("12:30", "shakhta", "Построена шахта на горе Дилдаковская"),
        news("00:00", "elections2", "Прошли выборы в ХуеДуму"),
        news("00:00", "elections", "Установлены избирательные участки"),
        news("13:40", "hueduma", "Построено здание ХуеДумы"),
        news("13:35", "newservak", "Сервер восстановлен!"),
        news("15:00", "aeroraces", "Первый междугородний авиаперелёт"),
        news("15:06", "chicken", "Ужасная и срочная новость хуеграда"),
        news("11:24", "metro", "В хуеграде начата стройка метро"),
        news("15:00", "club", "ПК Клуб в Хуеграде!"),
        news("15:00", "shokla", "Достроена Школа №1 г. Хуеград им. Александра Дугина"),
        news("15:00", "tank150726", "Чудеса Хуеграда: pur1tym1nd прокатился на танке по станции Петушково"),
    )

    val education = listOf(
        ArticleRef("edu-ptu", "ПТУ для ебланов №67", "Лучшее учебное заведение для деревенских", "ptu.html"),
        ArticleRef("edu-htu", "ХТУ им. Хинера", "Престижнейшее высшее учебное заведение", "htuix.html"),
        ArticleRef("edu-school", "Школа №1 г. Хуеград", "Шокла №1 им. старого хуйла", "shokla.html"),
    )

    val articles: Map<String, ArticleRef> = (news.map { it.ref } + education).associateBy { it.id }

    val mods = listOf(
        ModFile("architectury-9.2.14-forge-1.20.1.jar", "567 КБ"),
        ModFile("balm-forge-1.20.1-7.3.44.jar", "549 КБ"),
        ModFile("collective-1.20.1-8.42.jar", "729 КБ"),
        ModFile("create-1.20.1-6.0.8.jar", "18,3 МБ"),
        ModFile("curios-forge-5.14.1+1.20.1.jar", "389 КБ"),
        ModFile("edm-26.5.0-forge-1.20.1.jar", "763 КБ"),
        ModFile("effortlessbuilding-1.20.1-3.10.jar", "2,2 МБ"),
        ModFile("framework-forge-1.20.1-0.8.0.jar", "375 КБ"),
        ModFile("furnitury-3.0707-forge-1.20.1.jar", "1,5 МБ"),
        ModFile("geckolib-forge-1.20.1-4.8.4.jar", "1,0 МБ"),
        ModFile("GlitchCore-forge-1.20.1-0.0.1.1.jar", "86 КБ"),
        ModFile("kotlinforforge-4.12.0-all.jar", "7,1 МБ"),
        ModFile("mcef-forge-2.1.6-1.20.1.jar", "213 КБ"),
        ModFile("minedevice-forge-1.1.2.jar", "13,5 МБ"),
        ModFile("phonescraft-v1.14.jar", "9,6 МБ"),
        ModFile("pointblank-forge-1.20.1-1.11.1.jar", "16,6 МБ"),
        ModFile("refurbished_furniture-forge-1.20.1-1.0.20.jar", "6,4 МБ"),
        ModFile("SereneSeasons-forge-1.20.1-9.1.0.3.jar", "157 КБ"),
        ModFile("superbwarfare-0.8.9-hotfix2-mc1.20.1-a3c46074e-all.jar", "30,2 МБ"),
        ModFile("villagernames-1.20.1-8.7.jar", "55 КБ"),
        ModFile("voicechat-forge-1.20.1-2.6.18.jar", "4,7 МБ"),
        ModFile("Warium-1.2.8-1.20.1.jar", "12,1 МБ"),
        ModFile("waystones-forge-1.20.1-14.1.21.jar", "524 КБ"),
        ModFile("webdisplays-2.0.2-1.20.1.jar", "793 КБ"),
        ModFile("xaerominimap-forge-1.20.1-26.4.2.jar", "2,1 МБ"),
    )
    const val MODS_WORLD_URL = "${Site.BASE}/mods/world/"

    val history = listOf(
        HistorySection("Начало", listOf(
            HistoryBlock(
                "Славный город Хуеград начинает свою не менее славную историю 10 февраля 2026 года. В тот день были построены деревянный свастон и дом в виде хуя, по образу которого начали строиться и остальные дома в центре Хуеграда (и даже знаменитый ZOV Corp. в их числе).",
                listOf(GalleryItem("swaston.jpg"), GalleryItem("fh.jpg")),
            ),
            HistoryBlock(
                "14 февраля того же года город получил название Хуеград",
                listOf(GalleryItem("nazva.png", "Название Хуеград")),
            ),
        )),
        HistorySection("Первый этап развития", listOf(
            HistoryBlock(
                "12 марта была построена станция «Петушково», которая до сих пор работает, хоть и претерпела многочисленные изменения в устройстве. Это был исключительно эксперимент, так как нормальной работы поезда без модов добиться не удалось. Поезд просто напросто крутило на рельсах, а ещё не было ручного управления. Было построено КПП с. Дилдово.\n\nК сожалению, на этом развитие Хуеграда прекратилось на 2 месяца.",
                listOf(GalleryItem("pet_poezd.jpg", "Станция Петушково"), GalleryItem("pet_poezd2.jpg", "Поезд")),
            ),
        )),
        HistorySection("Возрождение Хуеграда", listOf(
            HistoryBlock("В конце мая 2026 года Хуеград вновь стал доступен. Тогда был восстановлен ZOV Corp с новым высокоскоростным лифтом, восстановлены улицы, построена роскошная вилла строителей Хуеграда. Спустя несколько недель пошёл бурный рост Хуеграда — строились новые здания, развивалась инфраструктура, и одно из самых главных новшеств — появление этого замечательного сайта (на котором вы это читаете)."),
        )),
        HistorySection("Бурный рост", listOf(
            HistoryBlock(
                "24 июня 2026 — одна из самых знаменательных дат в истории Хуеграда. В этот день начался очень сильный прорыв в развитии — построена станция Хуеград, проведена первая линия рельсов, создан этот сайт и начали появляться новые деревни Хуеградского района.",
                listOf(GalleryItem("huegrad_station.jpg", "Станция Хуеград")),
            ),
            HistoryBlock("26 июня был принят гимн Республики Хуеград — «Я сел жопой...». Его можно послушать на вкладке «Радио»."),
            HistoryBlock("27 июня на сайт была добавлена вкладка «Дрочка».", listOf(GalleryItem("drochka.jpg", "Вкладка Дрочка"))),
            HistoryBlock("28 июня был построен ПТУ для ебланов №67 г. Хуеград.", seq("ptu", 1..16, "jpg", "ПТУ")),
            HistoryBlock(
                "29 июня была основана Меллстройская область. И хотя она является субъектом суверенной Республики Меллстроия, она тесно связана с историей Хуеграда, ведь они находятся на одном сервере. 2 июля был достроен Меллстройск — столица Меллстроии.",
                listOf(GalleryItem("vokzal_mellstroysk.jpg", "Вокзал Меллстройск")),
            ),
            HistoryBlock("11 июля была запущена радиостанция Huegrad FM."),
            HistoryBlock("18 июля построена почта Хуеграда.", seq("pochta_huegrad", 2..5, "jpg", "Почта")),
            HistoryBlock("18 июля основан ФБР (Фонд Борьбы с Русланом). Их первой находкой стала секретная дача русика с личным танком, навороченными воротами и роскошной кухней."),
            HistoryBlock(
                "19 июля был построен телепорт для быстрого перемещения между Хуеградом и Меллстройском. Находится на вокзале Меллстройска и в инфоцентре Хуеграда (у самого входа).",
                listOf(GalleryItem("tphuegrad.png", "Телепорт Хуеград"), GalleryItem("tpmelstrojsk.png", "Телепорт Меллстройск")),
            ),
            HistoryBlock(
                "В тот же день была открыта служба доставки 6 Кактусов и магазин OldParts (ныне не работающие). В отличие от почты, которая на тот момент отправляла только письма и только в другое отделение почты, 6 Кактусов доставляли в любой почтовый ящик или постомат.",
                seq("6dildakov", 1..3, "png", "6 Кактусов") + GalleryItem("oldparts.png", "OldParts"),
            ),
            HistoryBlock(
                "20 июля были построены Huegrad Bank (HB) и Huegrad Market (HM).",
                listOf(
                    GalleryItem("bank1.png", "Huegrad Bank 1"), GalleryItem("bank2.png", "Huegrad Bank 2"),
                    GalleryItem("market1.png", "Huegrad Market 1"), GalleryItem("market2.png", "Huegrad Market 2"),
                ),
            ),
            HistoryBlock(
                "27 июля построен Хуеградский Технический Университет им. Хинера.",
                listOf(4, 5, 3, 1, 2).map { GalleryItem("strojkaunivera$it.jpg", "Стройка университета $it") } +
                        seq("htuix", 1..3, "png", "ХТУ"),
            ),
            HistoryBlock(
                "30 июля построена штаб-квартира ФБР в Меллстройске. В тот же день был совершён междугородний авиаперелёт из Хуеграда в Меллстройск.",
                listOf(GalleryItem("fbrshtab.png", "Штаб ФБР"), GalleryItem("fbrshtab2.jpg", "Штаб ФБР 2")) +
                        listOf(GalleryItem("polet.png", "Полёт 1"), GalleryItem("polet2.png", "Полёт 2"), GalleryItem("polet3.png", "Полёт 3")),
            ),
            HistoryBlock(
                "13 августа были построены ХуеДума и Верховный суд Республики Хуеград.",
                listOf(
                    GalleryItem("hueduma.png", "ХуеДума"), GalleryItem("hueduma2.png", "ХуеДума 2"),
                    GalleryItem("sudvtapkissut1.png", "Верховный суд"), GalleryItem("sud2.png", "Суд 2"),
                ),
            ),
            HistoryBlock("16 августа состоялись выборы в ХуеДуму.", (5..7).map { GalleryItem("vybory$it.png", "Выборы $it") }),
            HistoryBlock("25 августа построена шахта в горе Дилдаковская.", seq("shakhta", 1..8, "png", "Шахта")),
            HistoryBlock("26 августа началось строительство фермы под псн. Пабловка.", listOf(GalleryItem("polje1.png", "Ферма Пабловка"))),
        )),
        HistorySection("Итог", listOf(
            HistoryBlock("И это только чуть больше полугода с создания Хуеграда. Изначально Хуеград не должен был стать городом, это просто обыкновенное баловство со свастикой и домом в виде огромного ствола."),
        )),
    )
}
