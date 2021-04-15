package no.nav.familie.ef.mottak.no.nav.familie.ef.mottak.util

import no.nav.familie.ef.mottak.config.DOKUMENTTYPE_OVERGANGSSTØNAD
import no.nav.familie.ef.mottak.repository.domain.Søknad
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import java.time.LocalDateTime
import java.util.*

fun journalføringHendelseRecord(
    journalpostId: String,
    hendelseType: String = "MidlertidigJournalført",
    temaNytt: String = "ENF"
) =
    JournalfoeringHendelseRecord(
        "hendelseId",
        1,
        hendelseType,
        journalpostId.toLong(),
        "M",
        "ENF",
        temaNytt,
        "SKAN_NETS",
        "kanalReferanseId",
        "ENF"
    )


object JournalføringHendelseRecordVars {
    const val JOURNALPOST_PAPIRSØKNAD = "111"
    const val JOURNALPOST_DIGITALSØKNAD = "222"
    const val JOURNALPOST_UTGÅENDE_DOKUMENT = "333"
    const val JOURNALPOST_IKKE_ENFNETRYGD = "444"
    const val JOURNALPOST_FERDIGSTILT = "555"
    const val OFFSET = 21L
}

fun søknad(
    opprettetTid: LocalDateTime = LocalDateTime.now().minusHours(12),
    id: String = UUID.randomUUID().toString(),
    taskOpprettet: Boolean = false,
    dokumenttype: String = DOKUMENTTYPE_OVERGANGSSTØNAD,
    journalpostId: String? = null,
    fnr: String = "11111122222",
    saksnummer: String? = null,
    skalAutomatiskJournalføres: Boolean = true
) =
    Søknad(
        id = id,
        søknadJson = "",
        fnr = fnr,
        dokumenttype = dokumenttype,
        opprettetTid = opprettetTid,
        taskOpprettet = taskOpprettet,
        journalpostId = journalpostId,
        saksnummer = saksnummer,
        skalAutomatiskJournalføres = skalAutomatiskJournalføres
    )
