package no.nav.familie.ef.mottak.task

fun kafkaHendelseFlyt() = listOf(TaskType(LagEksternJournalføringsoppgaveTask.TYPE))

fun hovedflyt() = listOf(
    TaskType(LagPdfTask.TYPE),
    TaskType(ArkiverSøknadTask.TYPE),
    TaskType(LagBehandleSakOppgaveTask.TYPE),
    TaskType(OpprettSakTask.TYPE),
    TaskType(OppdaterBehandleSakOppgaveTask.TYPE),
    TaskType(OppdaterJournalføringTask.TYPE),
    TaskType(FerdigstillJournalføringTask.TYPE)
)

val fallbacks = mapOf(
    TaskType(ArkiverSøknadTask.TYPE) to LagJournalføringsoppgaveTask.TYPE,
    TaskType(LagBehandleSakOppgaveTask.TYPE) to LagJournalføringsoppgaveTask.TYPE,
    TaskType(OpprettSakTask.TYPE) to LagJournalføringsoppgaveTask.TYPE
)
fun TaskType.nesteFallbackTask() = fallbacks[this] ?: error("Finner ikke fallback til $this")

fun TaskType.nesteHovedflytTask() = hovedflyt().zipWithNext().first {this == it.first}.second.type
fun TaskType.nesteKafkaHendelseFlytTask() = kafkaHendelseFlyt().zipWithNext().first { this == it.first }.second.type