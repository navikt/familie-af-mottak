package no.nav.familie.ef.mottak.repository.domain

import no.nav.familie.ef.mottak.encryption.FileCryptoConverter
import no.nav.familie.ef.mottak.encryption.StringValCryptoConverter
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "SOKNAD")
data class Soknad(@Id
                  val id: String = UUID.randomUUID().toString(),
                  @Convert(converter = StringValCryptoConverter::class)
                  @Column(name = "soknad_json")
                  val søknadJson: String,
                  @Convert(converter = FileCryptoConverter::class)
                  @Column(name = "soknad_pdf")
                  val søknadPdf: Fil? = null,
                  @Column(name = "journalpost_id")
                  val journalpostId: String? = null,
                  val saksnummer: String? = null,
                  val fnr: String
)
