package no.nav.familie.ef.mottak.service

import no.nav.familie.kontrakter.ef.søknad.Adresse
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.ef.søknad.Periode
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*


object Feltformaterer {

    /**
     * Håndterer formatering utover vanlig toString for endenodene
     */
    fun mapEndenodeTilUtskriftMap(entitet: Søknadsfelt<*>): Map<String, String> {

        return when (val verdi = entitet.verdi!!) {
            is Month ->
                feltMap(entitet.label, displayName(verdi))
            is Boolean ->
                feltMap(entitet.label, if (verdi) "Ja" else "Nei")
            is List<*> ->
                feltMap(entitet.label, verdi.joinToString("\n\n"))
            is Fødselsnummer ->
                feltMap(entitet.label, verdi.verdi)
            is Adresse ->
                feltMap(entitet.label, adresseString(verdi))
            is LocalDate ->
                feltMap(entitet.label, verdi.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            is LocalDateTime ->
                feltMap(entitet.label, verdi.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
            is Periode ->
                feltMap(entitet.label, periodeString(verdi))
            else ->
                feltMap(entitet.label, verdi.toString())

        }
    }

    private fun displayName(verdi: Month) = verdi.getDisplayName(TextStyle.FULL, Locale("no"))

    private fun periodeString(verdi: Periode): String {
        return "Fra ${displayName(verdi.fraMåned)} ${verdi.fraÅr} til ${displayName(verdi.tilMåned)} ${verdi.tilÅr}"
    }

    private fun adresseString(adresse: Adresse): String {
        return listOf(adresse.adresse,
                      listOf(adresse.postnummer, adresse.poststedsnavn).joinToString(" "),
                      adresse.land).joinToString("\n\n")
    }

    private fun feltMap(label: String, verdi: String) = mapOf("label" to label, "verdi" to verdi)

}
