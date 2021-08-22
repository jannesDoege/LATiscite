package com.example.myapplication

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.gson.GsonBuilder

class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var germanWordTextView = view?.findViewById<TextView>(R.id.german_word_text_view)

        assert(germanWordTextView != null)

        var latinWordButton = view?.findViewById<Button>(R.id.latin_word_click_to_show_german)

        var knownButton = view?.findViewById<ImageButton>(R.id.knownButton)
        var unknownButton = view?.findViewById<ImageButton>(R.id.unknownButton)

        latinWordButton?.setOnClickListener {
            show_vocab_german_on_click_listener()
        }
        knownButton?.setOnClickListener {
            known_button_on_click_listener()
        }
        unknownButton?.setOnClickListener {
            notKnown_button_on_click_listener()
        }

        oldColor = view?.findViewById<EditText>(R.id.lektionEditText).textColors
        new_word()
    }

    private lateinit var current_vocab: Vocab

    // set so high to trigger out of bounce exception in case of wrong implementation
    private var current_vocab_id: Int = 234234

    private var currentLektion: Int = 1

    private lateinit var oldColor: ColorStateList

    private fun fetch_data():MutableList<MutableList<Int>?>
    {
        this.activity?.let { ModelPreferencesManager.with(it.application) }

        var data: MutableList<MutableList<Int>?> = mutableListOf()

        for (i in 1..5){
            data.add(ModelPreferencesManager.get<MutableList<Int>>("prio_${i}"))
          }

        if (data[0] == null){
            data[0] = create_vocab_id_list()
            for (i in 1..4){
                data[i] = ArrayList()
            }
            for (i in 1..5){
                ModelPreferencesManager.put<MutableList<Int>?>(data[i-1], "prio_${i}")
            }
        }



        return data
    }

    private fun update_data(id: Int, known: Boolean){
        var data = fetch_data()

        var prioClassIdx: Int = 10
        var remove_idx: Int = -3

        for ((i, prio_list) in data.withIndex()){
            for ((j, _id) in prio_list!!.withIndex()){
                if (_id == id){
                    prioClassIdx = i
                    remove_idx = j
                }
            }
        }

        assert(prioClassIdx in (0..4))
        assert(remove_idx >= 0)

        data[prioClassIdx]?.removeAt(remove_idx)

        if (known){
            if (!prioClassIdx.equals(4)){
                data[prioClassIdx+1]?.add(id)
            }else{
                data[prioClassIdx]?.add(id)
            }
        }
        else {
            if (!prioClassIdx.equals(0)){
                data[prioClassIdx-1]?.add(id)
            }else{
                data[prioClassIdx]?.add(id)
            }
        }

        for (i in 1..5){
            ModelPreferencesManager.put<MutableList<Int>?>(data[i-1], "prio_${i}")
        }

    }

    private fun new_word(){
        val lektionText = view?.findViewById<EditText>(R.id.lektionEditText)
        var lektionStr = lektionText?.text.toString()

        if (!lektionStr.isEmpty()){
            currentLektion = lektionStr.toInt()
        }else{
            currentLektion = -1
        }

        var data = fetch_data()


        if (currentLektion in (1..34)){
            // new_data will have the same size as data and  will contain only those ids
            // which correspond to a vocab, in the selected (by user) lektion
            var new_data: MutableList<MutableList<Int>> = mutableListOf()
            for (i in (0..4)){
                new_data.add(ArrayList())
            }

            for (prioIdx in (0..4)){
                for (vc in data[prioIdx]!!){
                    if (vokabeln[vc].lektion.equals(currentLektion)){
                        new_data[prioIdx].add(vc)
                    }
                }
            }
            data = new_data.toMutableList()

            lektionText?.setTextColor(oldColor)
        }else if (currentLektion >= 0){
            // show to user that selection was invalid - they wont be able to set negative numbers
            // as lektion; negative numbers will be used to show that the string chosen by user is
            // empty and that they don't which to filter lektionen

            lektionText?.setTextColor(Color.parseColor("#FF0000"))
        }

        val prio_gaps = intArrayOf(8, 6, 3, 2, 1)

        val vocab_distribution: MutableList<Int> = mutableListOf()

        for (i in (0..4)){
            for(id in data[i]!!){
                vocab_distribution.add(id)
                for (j in (1 until prio_gaps[i])){
                    vocab_distribution.add(id)
                }
            }
        }

        // vocabs in a (numerically) lower priority group are more likely to be randomly chosen since
        // their id is on more indices than the ones from higher priority groups

        var rndm_idx = (0 until vocab_distribution.size).random()

        current_vocab_id = vocab_distribution[rndm_idx]
        current_vocab = vokabeln[current_vocab_id!!]

        view?.findViewById<Button>(R.id.latin_word_click_to_show_german)?.setText(current_vocab.latein)
            .toString()
        val germanWordTextView = view?.findViewById<TextView>(R.id.german_word_text_view)
        germanWordTextView?.setText(current_vocab.deutsch).toString()
        // only show translation once requested by user (click on latin word)
        germanWordTextView?.visibility = View.INVISIBLE
        // user cannot decide whether they were right until they saw the translation (click on latin word)
        val known_button = view?.findViewById<ImageButton>(R.id.knownButton)
        known_button?.isEnabled = false
        known_button?.alpha = 0.5F
        val unknown_button = view?.findViewById<ImageButton>(R.id.unknownButton)
        unknown_button?.isEnabled = false
        unknown_button?.alpha = 0.5F
    }

    private fun known_button_on_click_listener(){
        val known: Boolean = true

        update_data(current_vocab_id, known)
        new_word()
    }

    private fun notKnown_button_on_click_listener(){
        val known: Boolean = false

        update_data(current_vocab_id, known)
        new_word()
    }

    private fun show_vocab_german_on_click_listener(){
        val germanWordTextView = view?.findViewById<TextView>(R.id.german_word_text_view)
        germanWordTextView?.visibility = View.VISIBLE

        val known_button = view?.findViewById<ImageButton>(R.id.knownButton)
        known_button?.isEnabled = true
        known_button?.alpha = 1F
        val unknown_button = view?.findViewById<ImageButton>(R.id.unknownButton)
        unknown_button?.isEnabled = true
        unknown_button?.alpha = 1F
    }

    object ModelPreferencesManager {

        lateinit var preferences: SharedPreferences

        private const val PREFERENCES_FILE_NAME = "PRIO_LISTS"

        fun with(application: Application) {
            preferences = application.getSharedPreferences(
                PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        }

        fun <T> put(`object`: MutableList<Int>?, key: String) {
            val jsonString = GsonBuilder().create().toJson(`object`)
            preferences.edit().putString(key, jsonString).apply()
        }

        inline fun <reified T> get(key: String): MutableList<Int>?{
            val value = preferences.getString(key, null)?.replace("[", "")?.replace("]", "")
            var asList: MutableList<Int>? = null
            var currentNum:String = ""
            if (value != null) {
                asList = mutableListOf()
                for (ch in value){
                    if (ch != ','){
                        currentNum += ch
                    }else{
                        asList.add(currentNum.toInt())
                        currentNum = ""
                    }
                }
                if (currentNum.isNotEmpty()){
                    asList.add(currentNum.toInt())
                }
            }
            return  asList
        }
    }

    private fun create_vocab_id_list():MutableList<Int>{
        var vocabIDList = mutableListOf<Int>()
        for (e in (0 until vokabeln.size)){
            vocabIDList.add(e)
        }

        return vocabIDList
    }

    data class Vocab(
        val lektion: Int,
        val latein: String,
        val deutsch: String,
    )

    val vokabeln: MutableList<Vocab> = mutableListOf<Vocab>(
        Vocab(lektion = 1, latein = "Salve!", deutsch = "Hallo!"),
        Vocab(lektion = 1, latein = "pater", deutsch = "Vater"),
        Vocab(lektion = 1, latein = "mater", deutsch = "Mutter"),
        Vocab(lektion = 1, latein = "filius", deutsch = "Sohn"),
        Vocab(lektion = 1, latein = "filia", deutsch = "Tochter"),
        Vocab(lektion = 1, latein = "avus", deutsch = "Großvater"),
        Vocab(lektion = 1, latein = "servus", deutsch = "Sklave"),
        Vocab(lektion = 1, latein = "serva", deutsch = "Sklavin"),
        Vocab(lektion = 1, latein = "catella", deutsch = "Hündchen"),
        Vocab(lektion = 1, latein = "hic", deutsch = "hier"),
        Vocab(lektion = 1, latein = "est", deutsch = "ist"),
        Vocab(lektion = 1, latein = "dominus", deutsch = "Hausherr"),
        Vocab(lektion = 1, latein = "domina", deutsch = "Hausherrin"),
        Vocab(lektion = 1, latein = "laborat", deutsch = "arbeitet"),
        Vocab(lektion = 1, latein = "ludere", deutsch = "spielen"),
        Vocab(lektion = 1, latein = "cantare", deutsch = "singen"),
        Vocab(lektion = 1, latein = "et", deutsch = "und"),
        Vocab(lektion = 1, latein = "gaudere", deutsch = "freuen"),
        Vocab(lektion = 1, latein = "currere", deutsch = "laufen"),
        Vocab(lektion = 1, latein = "nam", deutsch = "denn"),
        Vocab(lektion = 1, latein = "hodie", deutsch = "heute"),
        Vocab(lektion = 1, latein = "familia", deutsch = "Familie"),
        Vocab(lektion = 1, latein = "exspectare", deutsch = "erwarten"),
        Vocab(lektion = 1, latein = "subito", deutsch = "plötzlich"),
        Vocab(lektion = 1, latein = "clamor", deutsch = "Lärm"),
        Vocab(lektion = 1, latein = "audire", deutsch = "hören"),
        Vocab(lektion = 1, latein = "rogare", deutsch = "fragen"),
        Vocab(lektion = 1, latein = "quid?", deutsch = "was?"),
        Vocab(lektion = 1, latein = "querere", deutsch = "suchen"),
        Vocab(lektion = 1, latein = "non", deutsch = "nicht"),
        Vocab(lektion = 1, latein = "videre", deutsch = "sehen"),
        Vocab(lektion = 1, latein = "sed", deutsch = "aber"),
        Vocab(lektion = 1, latein = "ubi?", deutsch = "wo?"),
        Vocab(lektion = 1, latein = "ecce!", deutsch = "sieh!"),
        Vocab(lektion = 1, latein = "intrare", deutsch = "betreten"),
        Vocab(lektion = 1, latein = "tum", deutsch = "darauf"),
        Vocab(lektion = 1, latein = "ibi", deutsch = "dort"),
        Vocab(lektion = 1, latein = "nunc", deutsch = "nun"),
        Vocab(lektion = 1, latein = "etiam", deutsch = "auch"),
        Vocab(lektion = 1, latein = "ridet", deutsch = "lacht"),
        Vocab(lektion = 1, latein = "tenere", deutsch = "halten"),
        Vocab(lektion = 2, latein = "amare", deutsch = "mögen"),
        Vocab(lektion = 2, latein = "soror", deutsch = "Schwester"),
        Vocab(lektion = 2, latein = "respondere", deutsch = "antworten"),
        Vocab(lektion = 2, latein = "iterum", deutsch = "wieder"),
        Vocab(lektion = 2, latein = "conspicere", deutsch = "erblicken"),
        Vocab(lektion = 2, latein = "fugere", deutsch = "fliehen"),
        Vocab(lektion = 2, latein = "capere", deutsch = "fangen"),
        Vocab(lektion = 2, latein = "puer", deutsch = "Kind"),
        Vocab(lektion = 2, latein = "puella", deutsch = "Mädchen"),
        Vocab(lektion = 2, latein = "frater", deutsch = "Bruder"),
        Vocab(lektion = 2, latein = "itaque", deutsch = "deshalt"),
        Vocab(lektion = 2, latein = "sunt", deutsch = "sind"),
        Vocab(lektion = 2, latein = "culina", deutsch = "Küche"),
        Vocab(lektion = 2, latein = "debere", deutsch = "müssen"),
        Vocab(lektion = 2, latein = "cupere", deutsch = "mögen"),
        Vocab(lektion = 2, latein = "adhuc", deutsch = "noch"),
        Vocab(lektion = 2, latein = "liberi", deutsch = "Kinder"),
        Vocab(lektion = 2, latein = "vir", deutsch = "Mann"),
        Vocab(lektion = 2, latein = "otium", deutsch = "Ruhe"),
        Vocab(lektion = 2, latein = "parentes", deutsch = "Eltern"),
        Vocab(lektion = 2, latein = "officium", deutsch = "Pflicht"),
        Vocab(lektion = 2, latein = "explere", deutsch = "erfüllen"),
        Vocab(lektion = 2, latein = "dicere", deutsch = "sagen"),
        Vocab(lektion = 2, latein = "statim", deutsch = "sofort"),
        Vocab(lektion = 2, latein = "incipere", deutsch = "beginnen"),
        Vocab(lektion = 2, latein = "cur?", deutsch = "warum?"),
        Vocab(lektion = 2, latein = "semper", deutsch = "immer"),
        Vocab(lektion = 2, latein = "labor", deutsch = "Arbeit"),
        Vocab(lektion = 2, latein = "placere", deutsch = "gefallen"),
        Vocab(lektion = 2, latein = "pergere", deutsch = "fortsetzen"),
        Vocab(lektion = 3, latein = "equus", deutsch = "Pferd"),
        Vocab(lektion = 3, latein = "ego", deutsch = "ich"),
        Vocab(lektion = 3, latein = "me", deutsch = "mich"),
        Vocab(lektion = 3, latein = "tu", deutsch = "du"),
        Vocab(lektion = 3, latein = "te", deutsch = "dich"),
        Vocab(lektion = 3, latein = "nos", deutsch = "wir"),
        Vocab(lektion = 3, latein = "vos", deutsch = "ihr"),
        Vocab(lektion = 3, latein = "lepus", deutsch = "Hase"),
        Vocab(lektion = 3, latein = "canis", deutsch = "Hund"),
        Vocab(lektion = 3, latein = "delectare", deutsch = "erfreuen"),
        Vocab(lektion = 3, latein = "bos", deutsch = "Rind"),
        Vocab(lektion = 3, latein = "alere", deutsch = "nähern"),
        Vocab(lektion = 3, latein = "villa", deutsch = "Landhaus"),
        Vocab(lektion = 3, latein = "salutare", deutsch = "grüßen"),
        Vocab(lektion = 3, latein = "iam", deutsch = "jetzt"),
        Vocab(lektion = 3, latein = "eum", deutsch = "ihn"),
        Vocab(lektion = 3, latein = "eam", deutsch = "sie"),
        Vocab(lektion = 3, latein = "eos", deutsch = "sie"),
        Vocab(lektion = 3, latein = "eas", deutsch = "sie"),
        Vocab(lektion = 3, latein = "uxor", deutsch = "Ehefrau"),
        Vocab(lektion = 3, latein = "cena", deutsch = "Mahlzeit"),
        Vocab(lektion = 3, latein = "parare", deutsch = "vorbereiten"),
        Vocab(lektion = 3, latein = "adiuvare", deutsch = "unterstützen"),
        Vocab(lektion = 3, latein = "in", deutsch = "in"),
        Vocab(lektion = 3, latein = "ire", deutsch = "gehen"),
        Vocab(lektion = 3, latein = "instruere", deutsch = "anweisen"),
        Vocab(lektion = 3, latein = "quo?", deutsch = "wohin?"),
        Vocab(lektion = 3, latein = "ad", deutsch = "zu"),
        Vocab(lektion = 3, latein = "bene", deutsch = "gut"),
        Vocab(lektion = 3, latein = "caedere", deutsch = "schlachten"),
        Vocab(lektion = 3, latein = "servare", deutsch = "retten"),
        Vocab(lektion = 3, latein = "certe", deutsch = "sicher"),
        Vocab(lektion = 3, latein = "hortus", deutsch = "Garten"),
        Vocab(lektion = 3, latein = "igitur", deutsch = "also"),
        Vocab(lektion = 4, latein = "schola", deutsch = "Schule"),
        Vocab(lektion = 4, latein = "ducere", deutsch = "führen"),
        Vocab(lektion = 4, latein = "vocare", deutsch = "rufen"),
        Vocab(lektion = 4, latein = "properare", deutsch = "eilen"),
        Vocab(lektion = 4, latein = "magister", deutsch = "Lehrer"),
        Vocab(lektion = 4, latein = "sedere", deutsch = "sitzen"),
        Vocab(lektion = 4, latein = "considere", deutsch = "hinsetzen"),
        Vocab(lektion = 4, latein = "habere", deutsch = "haben"),
        Vocab(lektion = 4, latein = "scribere", deutsch = "schreiben"),
        Vocab(lektion = 4, latein = "discere", deutsch = "lernen"),
        Vocab(lektion = 4, latein = "num?", deutsch = "denn?"),
        Vocab(lektion = 4, latein = "posse", deutsch = "können"),
        Vocab(lektion = 4, latein = "legere", deutsch = "lesen"),
        Vocab(lektion = 4, latein = "munus", deutsch = "Aufgabe"),
        Vocab(lektion = 4, latein = "tacere", deutsch = "schweigen"),
        Vocab(lektion = 4, latein = "discipulus", deutsch = "Schüler"),
        Vocab(lektion = 4, latein = "discipula", deutsch = "Schülerin"),
        Vocab(lektion = 4, latein = "monere", deutsch = "mahnen"),
        Vocab(lektion = 4, latein = "ita", deutsch = "so"),
        Vocab(lektion = 4, latein = "postea", deutsch = "später"),
        Vocab(lektion = 4, latein = "quis?", deutsch = "wer"),
        Vocab(lektion = 4, latein = "mostrare", deutsch = "zeigen"),
        Vocab(lektion = 4, latein = "punire", deutsch = "bestrafen"),
        Vocab(lektion = 4, latein = "onus", deutsch = "Last"),
        Vocab(lektion = 4, latein = "portare", deutsch = "tragen"),
        Vocab(lektion = 4, latein = "docere", deutsch = "unterrichten"),
        Vocab(lektion = 4, latein = "ergo", deutsch = "also"),
        Vocab(lektion = 5, latein = "per", deutsch = "durch"),
        Vocab(lektion = 5, latein = "Romanus", deutsch = "römisch"),
        Vocab(lektion = 5, latein = "multi", deutsch = "viele"),
        Vocab(lektion = 5, latein = "monumentum", deutsch = "Denkmal"),
        Vocab(lektion = 5, latein = "pulcher", deutsch = "schön"),
        Vocab(lektion = 5, latein = "magnus", deutsch = "groß"),
        Vocab(lektion = 5, latein = "prope", deutsch = "nahe bei"),
        Vocab(lektion = 5, latein = "homo", deutsch = "Mensch"),
        Vocab(lektion = 5, latein = "accedere", deutsch = "herbeikommen"),
        Vocab(lektion = 5, latein = "iratus", deutsch = "zornig"),
        Vocab(lektion = 5, latein = "amicus", deutsch = "Freund"),
        Vocab(lektion = 5, latein = "amica", deutsch = "Freundin"),
        Vocab(lektion = 5, latein = "praebere", deutsch = "bieten"),
        Vocab(lektion = 5, latein = "tam", deutsch = "so"),
        Vocab(lektion = 5, latein = "accusare", deutsch = "beschuldigen"),
        Vocab(lektion = 5, latein = "fur", deutsch = "Dieb"),
        Vocab(lektion = 5, latein = "malus", deutsch = "böse"),
        Vocab(lektion = 5, latein = "aureus", deutsch = "golden"),
        Vocab(lektion = 5, latein = "donum", deutsch = "Geschenk"),
        Vocab(lektion = 5, latein = "reddere", deutsch = "zurückgeben"),
        Vocab(lektion = 5, latein = "aut", deutsch = "oder"),
        Vocab(lektion = 5, latein = "mors", deutsch = "Tod"),
        Vocab(lektion = 5, latein = "manere", deutsch = "erwarten"),
        Vocab(lektion = 5, latein = "obsecrare", deutsch = "anflehen"),
        Vocab(lektion = 5, latein = "miser", deutsch = "arm"),
        Vocab(lektion = 5, latein = "profecto", deutsch = "tatsächlich"),
        Vocab(lektion = 5, latein = "tutus", deutsch = "sicher"),
        Vocab(lektion = 6, latein = "spectator", deutsch = "Zuschauer"),
        Vocab(lektion = 6, latein = "cuncti", deutsch = "alle"),
        Vocab(lektion = 6, latein = "agitator", deutsch = "Wagenlenker"),
        Vocab(lektion = 6, latein = "praeclarus", deutsch = "berühmt"),
        Vocab(lektion = 6, latein = "tandem", deutsch = "schließlich"),
        Vocab(lektion = 6, latein = "meus", deutsch = "meine"),
        Vocab(lektion = 6, latein = "tuus", deutsch = "deine"),
        Vocab(lektion = 6, latein = "noster", deutsch = "unsere"),
        Vocab(lektion = 6, latein = "veste", deutsch = "eure"),
        Vocab(lektion = 6, latein = "suus", deutsch = "seine"),
        Vocab(lektion = 6, latein = "eius", deutsch = "seine"),
        Vocab(lektion = 6, latein = "eorum", deutsch = "ihre"),
        Vocab(lektion = 6, latein = "earum", deutsch = "dessen"),
        Vocab(lektion = 6, latein = "vincere", deutsch = "siegen"),
        Vocab(lektion = 6, latein = "verbum", deutsch = "Wort"),
        Vocab(lektion = 6, latein = "non iam", deutsch = "nicht mehr"),
        Vocab(lektion = 6, latein = "signum", deutsch = "Zeichen"),
        Vocab(lektion = 6, latein = "imperator", deutsch = "Kaiser"),
        Vocab(lektion = 6, latein = "tuba", deutsch = "Trompete"),
        Vocab(lektion = 6, latein = "canere", deutsch = "ertönen"),
        Vocab(lektion = 6, latein = "dare", deutsch = "geben"),
        Vocab(lektion = 6, latein = "incitare", deutsch = "anfeuern"),
        Vocab(lektion = 6, latein = "gaudium", deutsch = "Freude"),
        Vocab(lektion = 6, latein = "surgere", deutsch = "aufstehen"),
        Vocab(lektion = 6, latein = "inter", deutsch = "zwischen"),
        Vocab(lektion = 6, latein = "crescere", deutsch = "wachsen"),
        Vocab(lektion = 6, latein = "simul", deutsch = "gleichzeitig"),
        Vocab(lektion = 6, latein = "fere", deutsch = "fast"),
        Vocab(lektion = 6, latein = "tangere", deutsch = "berühren"),
        Vocab(lektion = 6, latein = "mortuus", deutsch = "tot"),
        Vocab(lektion = 6, latein = "se", deutsch = "sich"),
        Vocab(lektion = 6, latein = "pecunia", deutsch = "Geld"),
        Vocab(lektion = 7, latein = "cum", deutsch = "mit"),
        Vocab(lektion = 7, latein = "primum", deutsch = "zuerst"),
        Vocab(lektion = 7, latein = "vestis", deutsch = "Kleidung"),
        Vocab(lektion = 7, latein = "deponere", deutsch = "ablegen"),
        Vocab(lektion = 7, latein = "saepe", deutsch = "oft"),
        Vocab(lektion = 7, latein = "septimus", deutsch = "Adjektiv"),
        Vocab(lektion = 7, latein = "hora", deutsch = "Stunde"),
        Vocab(lektion = 7, latein = "autem", deutsch = "aber"),
        Vocab(lektion = 7, latein = "silentium", deutsch = "Stille"),
        Vocab(lektion = 7, latein = "iucundus", deutsch = "angenehm"),
        Vocab(lektion = 7, latein = "pauci", deutsch = "wenige"),
        Vocab(lektion = 7, latein = "aqua", deutsch = "Wasser"),
        Vocab(lektion = 7, latein = "nihil", deutsch = "nichts"),
        Vocab(lektion = 7, latein = "cogitare", deutsch = "denken"),
        Vocab(lektion = 7, latein = "at", deutsch = "aber"),
        Vocab(lektion = 7, latein = "vox", deutsch = "Stimme"),
        Vocab(lektion = 7, latein = "excitare", deutsch = "aufwecken"),
        Vocab(lektion = 7, latein = "patronus", deutsch = "Schutzherr"),
        Vocab(lektion = 7, latein = "sermo", deutsch = "Gespräch"),
        Vocab(lektion = 7, latein = "consilium", deutsch = "Rat"),
        Vocab(lektion = 7, latein = "occupatus", deutsch = "beschätigt"),
        Vocab(lektion = 7, latein = "molestus", deutsch = "lästig"),
        Vocab(lektion = 7, latein = "secum", deutsch = "bei sich"),
        Vocab(lektion = 7, latein = "aestas", deutsch = "Sommer"),
        Vocab(lektion = 7, latein = "errare", deutsch = "irren"),
        Vocab(lektion = 7, latein = "longe", deutsch = "weit"),
        Vocab(lektion = 7, latein = "ab", deutsch = "von"),
        Vocab(lektion = 8, latein = "secutor", deutsch = "Verfolger"),
        Vocab(lektion = 8, latein = "ille", deutsch = "jener"),
        Vocab(lektion = 8, latein = "porta", deutsch = "Tor"),
        Vocab(lektion = 8, latein = "retiarius", deutsch = "Netzkämpfer"),
        Vocab(lektion = 8, latein = "hic", deutsch = "dieser"),
        Vocab(lektion = 8, latein = "ignotus", deutsch = "unbekannt"),
        Vocab(lektion = 8, latein = "valde", deutsch = "sehr"),
        Vocab(lektion = 8, latein = "armus", deutsch = "Waffe"),
        Vocab(lektion = 8, latein = "gladius", deutsch = "Schwert"),
        Vocab(lektion = 8, latein = "pugnare", deutsch = "kämpfen"),
        Vocab(lektion = 8, latein = "petere", deutsch = "angreifen"),
        Vocab(lektion = 8, latein = "nomen", deutsch = "Name"),
        Vocab(lektion = 8, latein = "defendere", deutsch = "verteidigen"),
        Vocab(lektion = 8, latein = "atque", deutsch = "und"),
        Vocab(lektion = 8, latein = "cadere", deutsch = "fallen"),
        Vocab(lektion = 8, latein = "iacere", deutsch = "liegen"),
        Vocab(lektion = 8, latein = "rursus", deutsch = "wieder"),
        Vocab(lektion = 8, latein = "iactare", deutsch = "werfen"),
        Vocab(lektion = 8, latein = "intendere", deutsch = "richten"),
        Vocab(lektion = 8, latein = "neque", deutsch = "und nicht"),
        Vocab(lektion = 8, latein = "pugna", deutsch = "Kampf"),
        Vocab(lektion = 8, latein = "iudicium", deutsch = "Urteil"),
        Vocab(lektion = 8, latein = "aspicere", deutsch = "ansehen"),
        Vocab(lektion = 8, latein = "mittere", deutsch = "schicken"),
        Vocab(lektion = 8, latein = "vivere", deutsch = "leben"),
        Vocab(lektion = 9, latein = "parare", deutsch = "gehorchen"),
        Vocab(lektion = 9, latein = "ianua", deutsch = "Tür"),
        Vocab(lektion = 9, latein = "sinere", deutsch = "zulassen"),
        Vocab(lektion = 9, latein = "aperire", deutsch = "öffnen"),
        Vocab(lektion = 9, latein = "censere", deutsch = "denken"),
        Vocab(lektion = 9, latein = "ruri", deutsch = "auf dem Land"),
        Vocab(lektion = 9, latein = "abire", deutsch = "weggehen"),
        Vocab(lektion = 9, latein = "cedere", deutsch = "gehen"),
        Vocab(lektion = 9, latein = "improbus", deutsch = "schlecht"),
        Vocab(lektion = 9, latein = "fortuna", deutsch = "Vermögen"),
        Vocab(lektion = 9, latein = "perdere", deutsch = "verschwenden"),
        Vocab(lektion = 9, latein = "nocere", deutsch = "schaden"),
        Vocab(lektion = 9, latein = "studere", deutsch = "bemühen"),
        Vocab(lektion = 9, latein = "bibere", deutsch = "trinken"),
        Vocab(lektion = 9, latein = "corrumpere", deutsch = "verderben"),
        Vocab(lektion = 9, latein = "enim", deutsch = "nähmlich"),
        Vocab(lektion = 9, latein = "convivium", deutsch = "Gastmahl"),
        Vocab(lektion = 9, latein = "instituere", deutsch = "veranstalten"),
        Vocab(lektion = 9, latein = "curare", deutsch = "kümmern"),
        Vocab(lektion = 9, latein = "de", deutsch = "um"),
        Vocab(lektion = 9, latein = "vita", deutsch = "Leben"),
        Vocab(lektion = 9, latein = "modo", deutsch = "bloß"),
        Vocab(lektion = 9, latein = "agere", deutsch = "handeln"),
        Vocab(lektion = 9, latein = "poena", deutsch = "Strafe"),
        Vocab(lektion = 9, latein = "instare", deutsch = "bevorstehen"),
        Vocab(lektion = 10, latein = "ascendere", deutsch = "besteigen"),
        Vocab(lektion = 10, latein = "dum", deutsch = "während"),
        Vocab(lektion = 10, latein = "deus", deutsch = "Gott"),
        Vocab(lektion = 10, latein = "nuper", deutsch = "neulich"),
        Vocab(lektion = 10, latein = "fabula", deutsch = "Sage"),
        Vocab(lektion = 10, latein = "narrare", deutsch = "erzählen"),
        Vocab(lektion = 10, latein = "quondam", deutsch = "einst"),
        Vocab(lektion = 10, latein = "urbs", deutsch = "Stadt"),
        Vocab(lektion = 10, latein = "femina", deutsch = "Frau"),
        Vocab(lektion = 10, latein = "arx", deutsch = "Festung"),
        Vocab(lektion = 10, latein = "obsidere", deutsch = "belagern"),
        Vocab(lektion = 10, latein = "miles", deutsch = "Soldat"),
        Vocab(lektion = 10, latein = "fames", deutsch = "Hunger"),
        Vocab(lektion = 10, latein = "tamen", deutsch = "dennoch"),
        Vocab(lektion = 10, latein = "abstinere", deutsch = "fernhalten"),
        Vocab(lektion = 10, latein = "quia", deutsch = "weil"),
        Vocab(lektion = 10, latein = "sacer", deutsch = "heilig"),
        Vocab(lektion = 10, latein = "forte", deutsch = "zufällif"),
        Vocab(lektion = 10, latein = "saxum", deutsch = "Fels"),
        Vocab(lektion = 10, latein = "nox", deutsch = "Nacht"),
        Vocab(lektion = 10, latein = "custos", deutsch = "Wächter"),
        Vocab(lektion = 10, latein = "animadvertere", deutsch = "bemerken"),
        Vocab(lektion = 10, latein = "sacrum", deutsch = "Opfer"),
        Vocab(lektion = 10, latein = "facere", deutsch = "machen"),
        Vocab(lektion = 11, latein = "diu", deutsch = "lange"),
        Vocab(lektion = 11, latein = "ubique", deutsch = "überall"),
        Vocab(lektion = 11, latein = "pro", deutsch = "für"),
        Vocab(lektion = 11, latein = "timere", deutsch = "fürchten"),
        Vocab(lektion = 11, latein = "adire", deutsch = "herangehen"),
        Vocab(lektion = 11, latein = "perire", deutsch = "sterben"),
        Vocab(lektion = 11, latein = "pius", deutsch = "pflichtbewusst"),
        Vocab(lektion = 11, latein = "socius", deutsch = "Gefährte"),
        Vocab(lektion = 11, latein = "litus", deutsch = "Küste"),
        Vocab(lektion = 11, latein = "respicere", deutsch = "zurückblicken"),
        Vocab(lektion = 11, latein = "oculus", deutsch = "Auge"),
        Vocab(lektion = 11, latein = "tradere", deutsch = "übergeben"),
        Vocab(lektion = 11, latein = "umbra", deutsch = "Schatten"),
        Vocab(lektion = 11, latein = "apparere", deutsch = "erscheinen"),
        Vocab(lektion = 11, latein = "fatum", deutsch = "Schicksal"),
        Vocab(lektion = 11, latein = "patria", deutsch = "Heimat"),
        Vocab(lektion = 11, latein = "novus", deutsch = "neu"),
        Vocab(lektion = 11, latein = "sine", deutsch = "ohne"),
        Vocab(lektion = 11, latein = "navis", deutsch = "Schiff"),
        Vocab(12, "post", "nach"),
        Vocab(12, "error", "Irrfahrt"),
        Vocab(12, "exire", "verlassen"),
        Vocab(12, "ante", "vor"),
        Vocab(12, "ipse", "selbst"),
        Vocab(12, "habitare", "wohnen"),
        Vocab(12, "futurus", "zukünftig"),
        Vocab(12, "iubere", "befehlen"),
        Vocab(12, "tempus", "Zeit"),
        Vocab(12, "cognoscere", "erfahren"),
        Vocab(12, "deinde", "dann"),
        Vocab(12, "providere", "vorraussehen"),
        Vocab(12, "periculum", "Gefahr"),
        Vocab(12, "quando?", "wann?"),
        Vocab(12, "finis", "Ende"),
        Vocab(12, "tot", "so viele"),
        Vocab(12, "gratia", "Dank"),
        Vocab(12, "deligere", "auswählen"),
        Vocab(12, "sacerdos", "Priester"),
        Vocab(12, "quamquam", "obwohl"),
        Vocab(12, "subire", "ertragen"),
        Vocab(12, "terra", "Land"),
        Vocab(12, "mox", "bald"),
        Vocab(12, "hostis", "Feind"),
        Vocab(12, "bellum", "Krieg"),
        Vocab(12, "saevus", "wild"),
        Vocab(13, "condere", "gründen"),
        Vocab(13, "pastor", "Hirte"),
        Vocab(13, "invenire", "finden"),
        Vocab(13, "uterque", "jeder von beiden"),
        Vocab(13, "regere", "regieren"),
        Vocab(13, "voluntas", "Wunsch"),
        Vocab(13, "sex", "sechs"),
        Vocab(13, "avis", "Vogel"),
        Vocab(13, "regnum", "Herrschaft"),
        Vocab(13, "duodecim", "zwölf"),
        Vocab(13, "tunc", "dann"),
        Vocab(13, "rex", "König"),
        Vocab(13, "contendere", "behaupten"),
        Vocab(13, "antea", "vorher"),
        Vocab(13, "appellare", "ansprechen"),
        Vocab(13, "vix", "kaum"),
        Vocab(13, "murus", "Mauer"),
        Vocab(13, "scelestus", "verbrecherisch"),
        Vocab(13, "caedes", "Ermordung"),
        Vocab(13, "occupare", "einnehmen"),
        Vocab(14, "rostra", "Rednerbühne"),
        Vocab(14, "civis", "Bürger"),
        Vocab(14, "ager", "Acker"),
        Vocab(14, "pellere", "vertreiben"),
        Vocab(14, "probus", "anständig"),
        Vocab(14, "urgere", "drängen"),
        Vocab(14, "unus", "einer"),
        Vocab(14, "populus", "Volk"),
        Vocab(14, "lapis", "Stein"),
        Vocab(14, "iacere", "werfen"),
        Vocab(14, "quod", "weil"),
        Vocab(14, "auris", "Ohr"),
        Vocab(14, "gloria", "Ehre"),
        Vocab(14, "vero", "wirklich"),
        Vocab(14, "sedes", "Wohnsitz"),
        Vocab(14, "luxuria", "Genusssucht"),
        Vocab(14, "avaritia", "Habgier"),
        Vocab(14, "quamdiu?", "wie lange?"),
        Vocab(14, "opprimere", "unterdrücken"),
        Vocab(14, "lex", "Gesetz"),
        Vocab(14 , "accipere", "annehmen"),
        Vocab(14, "mutare", "verändern"),
        Vocab(15, "ira", "Zorn"),
        Vocab(15, "ardere", "brennen"),
        Vocab(15, "qui", "der/welcher"),
        Vocab(15, "quae", "die/welche"),
        Vocab(15, "quod", "das/welches"),
        Vocab(15, "obses", "Geisel"),
        Vocab(15,  "nonnulli", "einige"),
        Vocab(15, "castra", "Kriegslager"),
        Vocab(15, "fallere", "täuschen"),
        Vocab(15, "flumen", "Fluss"),
        Vocab(15, "telum", "Geschoss"),
        Vocab(15, "vulnus", "Wunde"),
        Vocab(15, "recipere", "aufnehmen"),
        Vocab(15, "foedus", "Vertrag"),
        Vocab(15, "nuntius", "Bote"),
        Vocab(15, "repetere", "zurückverlangen"),
        Vocab(15, "si", "wenn"),
        Vocab(15, "rumpere", "brechen"),
        Vocab(15, "aliter", "sonst"),
        Vocab(15, "apud", "bei"),
        Vocab(15, "Etruscus", "etruskisch"),
        Vocab(15, "libertas", "Freiheit"),
        Vocab(15, "remitter", "zurückschicken"),
        Vocab(15, "interea", "inzwischen"),
        Vocab(15, "virtus", "Tapferkeit"),
        Vocab(15, "redire", "zurückkehren"),
        Vocab(15, "facinus", "Handlung"),
        Vocab(15, "laudare", "loben"),
        Vocab(15, "ob", "wegen"),
        Vocab(15, "honor", "Ehre"),
        Vocab(15, "afficere", "mit etw. versehen"),
        Vocab(15, "ponere", "stellen"),
        Vocab(15, "gravis", "ernst"),
        Vocab(15, "comes", "Begleiter"),
        Vocab(15, "provincia", "Provinz"),
        Vocab(15, "mos", "Sitte"),
        Vocab(15, "illustris", "bekannt"),
        Vocab(16, "grandis", "groß"),
        Vocab(16, "varius", "verschieden"),
        Vocab(16, "dividere", "teilen"),
        Vocab(16, "singularis", "einzigartig"),
        Vocab(16, "velut", "wie zum Beispiel"),
        Vocab(16, "vas", "Gefähß"),
        Vocab(16, "omnis", "jeder"),
        Vocab(16, "ubi", "als"),
        Vocab(16, "regius", "königlich"),
        Vocab(16, "audax", "wagemutig"),
        Vocab(16, "audere", "wagen"),
        Vocab(16, "alius", "ein anderer"),
        Vocab(16, "modus", "Art"),
        Vocab(16, "donare", "schenken"),
        Vocab(16, "felix", "glücklich"),
        Vocab(16, "ingens", "ungeheuer groß"),
        Vocab(16, "celer", "schnell"),
        Vocab(16, "acer", "energisch"),
        Vocab(16, "sibi", "sich"),
        Vocab(17, "procul", "in der Ferne"),
        Vocab(17, "pirata", "Seeräuber"),
        Vocab(17, "brevis", "kurz"),
        Vocab(17, "dux", "Anführer"),
        Vocab(17, "nobilis", "adlig"),
        Vocab(17, "dives", "reich"),
        Vocab(17, "solvere", "zahlen"),
        Vocab(17, "viginti", "zwanzig"),
        Vocab(17, "nullus", "keiner"),
        Vocab(17, "terrere", "jmd. erschrecken"),
        Vocab(17, "tantum", "nur"),
        Vocab(17, "poscere", "fordern"),
        Vocab(17, "putare", "glauben"),
        Vocab(17, "quinquaginta", "fünfzig"),
        Vocab(17,"propinquus", "nahe"),
        Vocab(17, "colligere", "sammeln"),
        Vocab(17, "se gerere", "sich verhalten"),
        Vocab(17, "cum", "jedes mal wenn"),
        Vocab(17, "dormire", "schlafen"),
        Vocab(17, "carmen", "Gedicht"),
        Vocab(17, "recitare", "vorlesen"),
        Vocab(17, "laus", "Lob"),
        Vocab(17, "audacia", "Mut"),
        Vocab(17, "perturbare", "verwirren"),
        Vocab(17, "captivus", "Gefangener"),
        Vocab(17, "an?", "oder?"),
        Vocab(17, "supplicium", "Todesstrafe"),
        Vocab(17, "exponere", "aussetzen"),
        Vocab(17, "proximus", "der nächste"),
        Vocab(17, "carcer", "Gefängnis"),
        Vocab(18, "ignorare", "nicht kennen"),
        Vocab(18, "pares conscripti", "Senatoren"),
        Vocab(18, "coniuratio", "Verschwörung"),
        Vocab(18, "iste", "dieser da"),
        Vocab(18, "immo", "vielmehr"),
        Vocab(18, "contemnere", "verachten"),
        Vocab(18, "interesse", "sich dazwischen befinden"),
        Vocab(18, "nostrum", "von uns"),
        Vocab(18, "nondum", "noch nicht"),
        Vocab(18, "patere", "offenbar sein"),
        Vocab(18, "satis", "genug"),
        Vocab(18, "latere", "verborgen sein"),
        Vocab(18, "vestrum", "von euch"),
        Vocab(18, "salvus", "gesund"),
        Vocab(18, "nemo", "niemand"),
        Vocab(18, "desinere", "aufhören"),
        Vocab(18, "quamdiu", "so lange"),
        Vocab(18, "Romae", "in Rom"),
        Vocab(18, "pars", "der Teil"),
        Vocab(18, "incendere", "entflammen"),
        Vocab(18, "proinde", "deswegen"),
        Vocab(18, "excedere", "hinausgehen"),
        Vocab(18, "relinquere", "zurücklassen"),
        Vocab(18, "una", "zusammen"),
        Vocab(18, "oratio", "Rede"),
        Vocab(18, "arcere", "abwehren"),
        Vocab(18, "tectum", "Dach"),
        Vocab(18, "aeternus", "ewig"),
        Vocab(19, "Ulixes", "Odysseus"),
        Vocab(19, "Circe", "Kirke"),
        Vocab(19, "iter", "der Weg"),
        Vocab(19, "constituere", "beschließen"),
        Vocab(19, "ornare", "ausrüsten"),
        Vocab(19, "imprimis", "vor allem"),
        Vocab(19, "Sirenes", "Serenen"),
        Vocab(19, "navigare", "segeln"),
        Vocab(19, "insula", "Insel"),
        Vocab(19, "situs", "befindlich"),
        Vocab(19, "dulcis", "süß"),
        Vocab(19, "cogere", "jmd. zwingen, etw. zu tun"),
        Vocab(19, "numquam", "niemals"),
        Vocab(19, "accidere", "geschehen"),
        Vocab(19, "memoria", "Gedächtnis"),
        Vocab(19,  "unda", "Flut"),
        Vocab(19, "amittere", "verlieren"),
        Vocab(19, "postquam", "nachdem"),
        Vocab(19, "praeter", "an..vorbei"),
        Vocab(19, "frustra", "vergeblich"),
        Vocab(19, "vinculum", "Fessel"),
        Vocab(19, "neglegere", "vernachlässigen"),
        Vocab(19, "effugere", "entfliehen"),
        Vocab(20, "is", "dieser"),
        Vocab(20, "bestia", "das (wilde) Tier"),
        Vocab(20, "dies", "Tag"),
        Vocab(20, "nuptiae", "Hochzeit"),
        Vocab(20, "pes", "Fuß"),
        Vocab(20, "pernicies", "Verderben"),
        Vocab(20, "res", "Vorfall"),
        Vocab(20, "flere", "beweinen"),
        Vocab(20, "plenus", "voll"),
        Vocab(20, "dolor", "Schmerz"),
        Vocab(20, "commovere", "bewegen"),
        Vocab(20, "regina", "Königin"),
        Vocab(20, "demum", "endlich"),
        Vocab(20, "pervenire", "gelangen"),
        Vocab(20, "spes", "Hoffnung"),
        Vocab(20, "consistere", "Halt machen"),
        Vocab(20, "fides", "Beistand"),
        Vocab(20, "orare", "erbitten"),
        Vocab(20, "quaeso", "(ich) bitte!"),
        Vocab(20, "salus", "Wohlergehen"),
        Vocab(20, "mandare", "übergeben"),
        Vocab(20, "turba", "Menge"),
        Vocab(20, "optare", "wünschen"),
        Vocab(20, "lux", "Licht"),
        Vocab(20, "animus", "Geist"),
        Vocab(20, "abesse", "entfernt sein"),
        Vocab(20, "desiderium", "Sehnsucht"),
        Vocab(20, "superare", "überwinden"),
        Vocab(21, "reducere", "zurückführen"),
        Vocab(21, "gratias agere", "danken"),
        Vocab(21, "maxime", "am meisten"),
        Vocab(21, "aurum", "Gold"),
        Vocab(21, "prudens", "klug"),
        Vocab(21, "arbor", "Baum"),
        Vocab(21, "trahere", "ziehen"),
        Vocab(21, "mirus", "wunderbar"),
        Vocab(21, "fingere", "vorstellen"),
        Vocab(21, "cenare", "speisen"),
        Vocab(21, "poculum", "Becher"),
        Vocab(21, "vinum", "Wein"),
        Vocab(21, "os", "Mund"),
        Vocab(21, "cibus", "Speise"),
        Vocab(21, "divitiae", "Reichtum"),
        Vocab(21, "sic", "so"),
        Vocab(21, "torquere", "foltern"),
        Vocab(21, "stultus", "dumm"),
        Vocab(21, "venia",  "Verzeihung"),
        Vocab(21, "nisi", "wenn nicht"),
        Vocab(21, "liberare", "befreien"),
        Vocab(22, "obtinere", "erhalten"),
        Vocab(22, "civitas", "Bürgerschfaft"),
        Vocab(22, "confirmare", "bestätigen"),
        Vocab(22, "pro certo habere", "für sicher halten"),
        Vocab(22, "sub", "unter"),
        Vocab(22, "imperium", "Befehl"),
        Vocab(22, "libenter", "gern"),
        Vocab(22, "mores", "Charakter"),
        Vocab(22, "velle", "wollen"),
        Vocab(22, "dictum", "Äußerung"),
        Vocab(22, "fidelis", "treu"),
        Vocab(22, "imperare", "befehlen"),
        Vocab(22, "huc", "hierhin"),
        Vocab(22, "inopia", "Armut"),
        Vocab(22, "praedicare", "rühmen"),
        Vocab(22, "nuntiare", "melden"),
        Vocab(22, "convenire", "zusammenkommen"),
        Vocab(22, "philosophus", "Philosoph"),
        Vocab(22, "tantum .. quantum", "so viel .. wie"),
        Vocab(22, "turpis", "schimpflich"),
        Vocab(22, "mensis", "Monat"),
        Vocab(22, "intermittere", "einschieben"),
        Vocab(22, "contra", "gegen"),
        Vocab(22, "conscribere", "anwerben"),
        Vocab(22, "non ignorare", "sehr gut kennen"),
        Vocab(22, "paulum", "ein wenig"),
        Vocab(22, "sol", "Sonne"),
        Vocab(23, "proelium", "Kampf"),
        Vocab(23, "quem?", "wen?"),
        Vocab(23, "maximus", "der größte"),
        Vocab(23, "fundere", "zerstreuen"),
        Vocab(23, "causa", "Ursache"),
        Vocab(23, "gens", "Volksstamm"),
        Vocab(23, "praeterea", "außerdem"),
        Vocab(23, "duo", "zwei"),
        Vocab(23, "tertius", "der dritte"),
        Vocab(23, "copiae", "Truppen"),
        Vocab(23, "nominare", "jmd. etw. nennen"),
        Vocab(23, "o(h)!", "ach"),
        Vocab(23, "scilicet", "selbstverständlich"),
        Vocab(23, "laetitia", "Freude"),
        Vocab(23, "intellegere", "bemerken"),
        Vocab(23, "quoque", "auch"),
        Vocab(23, "egregius", "herausragend"),
        Vocab(24, "ars", "Kunst"),
        Vocab(24, "mathematicus", "mathematisch"),
        Vocab(24, "instrumentum", "Gerät"),
        Vocab(24, "utilis", "nützlich"),
        Vocab(24, "efficere", "herstellen"),
        Vocab(24, "machina", "Maschine"),
        Vocab(24, "adeo", "so sehr"),
        Vocab(24, "moenia", "Stadtmauer"),
        Vocab(24, "pendere", "hängen"),
        Vocab(24, "victoria", "Sieg"),
        Vocab(24, "undique", "von allen Seiten (her)"),
        Vocab(24, "expugnare", "erobern"),
        Vocab(24, "praeesse", "befehligen"),
        Vocab(24, "postulare", "fordern"),
        Vocab(24, "ne", "damit nicht"),
        Vocab(24, "forma", "Form"),
        Vocab(24, "geometricus", "geometrisch"),
        Vocab(24, "turbare", "stören"),
        Vocab(24, "tantus", "so groß"),
        Vocab(24, "posteri", "Nachkommen"),
        Vocab(25, "princeps", "Fürst"),
        Vocab(25, "annus", "Jahr"),
        Vocab(25,  "exercitus", "Heer"),
        Vocab(25, "persuadere", "überreden"),
        Vocab(25, "credere", "vertrauen"),
        Vocab(25, "placidus", "friedfertig"),
        Vocab(25, "cupidus", "begierig"),
        Vocab(25, "cum", "als, nachdem; weil; obwohl"),
        Vocab(25, "legio", "Legion"),
        Vocab(25, "impellere", "veranlassen"),
        Vocab(25, "impetus", "Angriff"),
        Vocab(25, "locus", "Ort"),
        Vocab(25, "silva", "Wald"),
        Vocab(25, "occultare", "verbergen"),
        Vocab(25, "insidae", "Hinterhalt"),
        Vocab(25, "eventus", "Ausgang"),
        Vocab(25, "manus", "Hand"),
        Vocab(25, "conspectus", "Anblick"),
        Vocab(25, "tumultus", "Aufruhr"),
        Vocab(25, "metus", "Furcht"),
        Vocab(25, "quattuor", "vier"),
        Vocab(25, "fortis", "mutig"),
        Vocab(25, "terribilis", "schrecklich"),
        Vocab(25, "tres", "drei"),
        Vocab(25, "delere", "zerstören"),
        Vocab(25, "servitus", "Knechtschaft"),
        Vocab(25, "abducere", "abführen"),
        Vocab(25, "clades", "Niederlage"),
        Vocab(25, "caelum", "Himmel"),
        Vocab(25, "tendere", "(aus)strecken"),
        Vocab(26, "theatrum", "Theater"),
        Vocab(26, "ludus", "Spiel"),
        Vocab(26, "vivus", "lebendig"),
        Vocab(26, "edere", "veranstalten"),
        Vocab(26, "sidus", "Stern"),
        Vocab(26, "cottidie", "täglich"),
        Vocab(26, "quidam", "ein bestimmter"),
        Vocab(26, "frons", "Vorderseite"),
        Vocab(26, "figura", "Gestalt"),
        Vocab(26, "summus", "der oberste"),
        Vocab(26, "ignarus", "unkundig"),
        Vocab(26, "significare", "anzeigen"),
        Vocab(26, "observare", "beobachten"),
        Vocab(26, "omen", "Vorzeichen"),
        Vocab(26, "aedificare", "bauen"),
        Vocab(26, "medius", "der mittlere"),
        Vocab(26, "auctor", "Urheber"),
        Vocab(26, "caput", "Kopf"),
        Vocab(26, "adicere", "hinzusetzen"),
        Vocab(26, "talis", "solch ein"),
        Vocab(26, "solere", "gewohnt sein"),
        Vocab(26, "quotiens", "wie oft"),
        Vocab(26, "verus", "wahr"),
        Vocab(26, "mihi persulam est", "ich bin überzeugt"),
        Vocab(26, "mea causa", "meinetwegen"),
        Vocab(26, "eligere", "auswählen"),
        Vocab(26, "succedere", "nachfolgen"),
        Vocab(27, "pax", "Frieden"),
        Vocab(27, "statuere", "beschließen"),
        Vocab(27, "celebrare", "feiern"),
        Vocab(27, "poeta", "Dichter"),
        Vocab(27, "saeculum", "Zeitalter"),
        Vocab(27, "augere", "vergrößern"),
        Vocab(27, "componere", "zusammensetzen"),
        Vocab(27, "negare",  "ablehnen"),
        Vocab(27, "ultimus", "der Letzte"),
        Vocab(27, "quantus?", "wie groß?"),
        Vocab(27, "potestas", "Macht"),
        Vocab(27, "florere", "blühen"),
        Vocab(27, "umquam", "jemals"),
        Vocab(27, "fructus", "Frucht"),
        Vocab(27, "frumentum", "Getreide"),
        Vocab(27, "domus", "Haus"),
        Vocab(27, "permittere", "erlauben"),
        Vocab(27, "orbis", "Kreis"),
        Vocab(27, "perpetuus", "ununterbrochen"),
        Vocab(28, "certamen", "Wettkampf"),
        Vocab(28, "iudex", "Richter"),
        Vocab(28, "merito", "mit Recht"),
        Vocab(28, "publicus", "öffentlich"),
        Vocab(28, "necesse est", "est ist nötig"),
        Vocab(28, "declarare", "ausrufen"),
        Vocab(28, "res publica", "Gemeinwesen"),
        Vocab(28, "pertinere", "sich erstrecken"),
        Vocab(28, "diligere", "wertschätzen"),
        Vocab(28, "nescire", "nicht wissen"),
        Vocab(28, "scire", "wissen"),
        Vocab(28, "reprehendere", "tadeln"),
        Vocab(28, "oppidum", "(Land-) Stadt"),
        Vocab(28, "claudere", "schließen"),
        Vocab(28, "consulere", "beratschlagen"),
        Vocab(28, "quomodo?", "wie?"),
        Vocab(28, "clam", "heimlich"),
        Vocab(28, "altus", "1. hoch; 2. tief"),
        Vocab(28,  "simulare", "vortäuschen"),
        Vocab(28, "dubitare", "zögern"),
        Vocab(28, "tamquam", "als ob"),
        Vocab(28, "deportare", "wegtragen"),
        Vocab(28, "quin etiam", "ja sogar"),
        Vocab(28, "evenire", "sich ereignen"),
        Vocab(28, "parere", "hervorbringen"),
        Vocab(28, "concedere", "erlauben"),
        Vocab(28, "offendere", "beleidigen"),
        Vocab(29, "nimis", "zu sehr"),
        Vocab(29, "potens", "mächtig"),
        Vocab(29, "utinam", "hoffentlich"),
        Vocab(29, "dolus", "List"),
        Vocab(29, "litterae", "Brief"),
        Vocab(29, "ferre", "tragen"),
        Vocab(29, "se conferre", "sich begeben"),
        Vocab(29, "dolere", "bedauern"),
        Vocab(29, "optimus", "der Beste"),
        Vocab(29, "concordia", "Eintracht"),
        Vocab(29, "tollere", "beseitigen"),
        Vocab(29, "laetus", "fröhlich"),
        Vocab(29, "affere", "heranbringen"),
        Vocab(29, "familiaris", "vertraut"),
        Vocab(29, "producere", "voranführen"),
        Vocab(29, "surgere", "aufstehen"),
        Vocab(29, "alter", "der eine"),
        Vocab(29, "sinus", "Bucht"),
        Vocab(29, "offere", "anbieten"),
        Vocab(29, "ait", "er/sie/es sagt(e)"),
        Vocab(29, "iussu", "auf Befehl"),
        Vocab(29, "mens", "Gesinnung"),
        Vocab(29, "cursus", "Lauf"),
        Vocab(29, "frangere", "zerbrechen"),
        Vocab(29, "paene", "beinahe"),
        Vocab(29, "gemitus", "Seufzen"),
        Vocab(29, "auxilium", "Hilfe"),
        Vocab(29, "inferre", "hineintragen"),
        Vocab(30, "carus", "lieb"),
        Vocab(30, "epistula", "Brief"),
        Vocab(30, "hortari", "ermahnen"),
        Vocab(30, "conari", "versuchen"),
        Vocab(30, "oblivisci", "vergessen"),
        Vocab(30, "usque ad", "bis zu"),
        Vocab(30, "studium", "Studie"),
        Vocab(30, "somnus", "Schlaf"),
        Vocab(30, "mihi placet", "ich fasse den Beschluss"),
        Vocab(30, "levis", "leicht"),
        Vocab(30, "motus", "Bewegung"),
        Vocab(30, "fieri", "entstehen"),
        Vocab(30, "aedificium", "Gebäude"),
        Vocab(30, "egredi", "hinausgehen"),
        Vocab(30, "domo", "von zuhause"),
        Vocab(30, "proficisci", "aufbrechen"),
        Vocab(30, "nubes", "Wolke"),
        Vocab(30, "ater", "grauenvoll"),
        Vocab(30, "descendere", "herabsteigen"),
        Vocab(30,  "cinis", "Asche"),
        Vocab(30, "difficilis", "schwer"),
        Vocab(30, "tueri", "schützen"),
        Vocab(30, "via", "Weg"),
        Vocab(30, "maritus", "Ehemann"),
        Vocab(30, "precari", "wünschen"),
        Vocab(30, "mori", "sterben"),
        Vocab(30, "queri", "klagen"),
        Vocab(30, "tenebrae", "Finsternis"),
        Vocab(30, "discedere", "auseinandergehen"),
        Vocab(30, "ingredi", "hineingehen"),
        Vocab(30, "prius", "früher"),
        Vocab(30, "quam", "als"),
        Vocab(30, "valere", "stark sein"),
        Vocab(31, "magistratus", "Beamter"),
        Vocab(31, "imponere", "auferlegen"),
        Vocab(31, "praecipere", "vorschreiben"),
        Vocab(31, "diligens", "gründlich"),
        Vocab(31, "praeceptum", "Vorschrift"),
        Vocab(31, "interior", "Innerer"),
        Vocab(31, "perterrere", "jmd. gewaltig erschrecken"),
        Vocab(31, "nolle", "nicht wollen"),
        Vocab(31, "interficere", "töten"),
        Vocab(31, "oportet", "es ist nötig, dass"),
        Vocab(31, "paratus", "bereit"),
        Vocab(31, "Iesus", "Jesus"),
        Vocab(31, "spiritus", "der Geist"),
        Vocab(31, "sanctus", "heilig"),
        Vocab(32, "genus", "die Art"),
        Vocab(32, "vitare", "vermeiden"),
        Vocab(32, "falsus", "falsch"),
        Vocab(32, "colere", "verehren"),
        Vocab(32, "circus", "Rennbahn"),
        Vocab(32, "probare", "billigen"),
        Vocab(32, "hoc loco", "an diesem Ort"),
        Vocab(32, "loqui", "sprechen"),
        Vocab(32, "scelus", "Verbrechen"),
        Vocab(32, "occidere", "niederhauen"),
        Vocab(32, "ruere", "stürzen"),
        Vocab(32, "minus", "weniger"),
        Vocab(32, "adulescens", "der/die junge Mann/Frau"),
        Vocab(32, "denique", "zuletzt"),
        Vocab(32, "carere", "nicht haben"),
        Vocab(32, "furor", "Raserei"),
        Vocab(32, "corripere", "befallen"),
        Vocab(32, "oriri", "sich erheben"),
        Vocab(32, "quare", "weshalb"),
        Vocab(33, "lingua", "Sprache"),
        Vocab(33, "Latinus", "lateinisch"),
        Vocab(33, "administrare", "verwalten"),
        Vocab(33,  "communis", "gemeinsam"),
        Vocab(33, "accedit, ut", "es kommt hinzu, dass"),
        Vocab(33, "cor", "Herz"),
        Vocab(33, "tam .. quam", "so .. wie"),
        Vocab(33, "patrius", "väterlich"),
        Vocab(33, "uti", "verwenden"),
        Vocab(33, "tabula", "Wachstäfelchen"),
        Vocab(33, "littera", "Buchstabe"),
        Vocab(33, "exercere", "üben, ausüben"),
        Vocab(33, "vacuus", "leer"),
        Vocab(33, "orator", "Redner"),
        Vocab(33, "primo", "zuerst"),
        Vocab(33, "domesticus", "häuslich"),
        Vocab(33, "adipisci", "erreichen"),
        Vocab(33, "purus", "sauber"),
        Vocab(34, "Turca", "der Türke"),
        Vocab(34, "vis", "Kraft"),
        Vocab(34, "vires", "Streitkräfte"),
        Vocab(34, "comparare", "vergleichen"),
        Vocab(34, "exter(us)", "der Äußerste"),
        Vocab(34, "pati", "erdulten"),
        Vocab(34, "sitis", "Durst"),
        Vocab(34, "rarus", "selten"),
        Vocab(34, "exstinguere", "auslöschen"),
        Vocab(34, "ita fit, ut", "so geschieht es, dass"),
        Vocab(34, "disciplina", "Disziplin"),
        Vocab(34, "Gianzari", "Janitscharen"),
        Vocab(34, "mille", "tausend"),
        Vocab(34, "consuetudo", "Gewohnheit"))
}