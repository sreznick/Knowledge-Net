package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.HistorySnapshotData
import com.infowings.catalog.common.SnapshotData
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORID
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val logger = loggerFor<HistoryService>()

class HistoryService(
    private val db: OrientDatabase,
    private val historyDao: HistoryDao
) {
    // пока такой наивный кеш. Словим OOME - переделаем
    private val cache = ConcurrentHashMap<String, HistoryFact>()

    fun entityTimeline(id: String): List<HistoryFact> = logTime(logger, "history timeline for entity collection") {
        transaction(db) {
            val events = historyDao.timelineForEntity(id)
            logger.info("${events.size} timeline events")
            return@transaction events.map { it.toFact() }
        }
    }

    fun allTimeline(entityClass: String): List<HistoryFact> = logTime(logger, "history timeline collection for $entityClass") {
        transaction(db) {
            val events = logTime(logger, "basic collecting of timed events for $entityClass") { historyDao.getAllHistoryEventsByTime(entityClass) }

            val payloadsAndUsers = historyDao.getPayloadsAndUsers(events.map { it.identity })

            return@transaction logTime(logger, "event data extraction") {
                events.map { event ->
                    val eventData = event.toEventFast().copy(username = payloadsAndUsers[event.id]?.first ?: "")
                    val payload = payloadsAndUsers[event.id]?.second ?: throw IllegalStateException("no payload for event ${event.id}")
                    HistoryFact(eventData, payload)
                }
            }
        }
    }

    fun asSnapshots(timeline: List<HistoryFact>, base: SnapshotData): List<HistorySnapshotData> {
        val current = Snapshot(base).toMutable()
        val result = listOf<HistorySnapshotData>().toMutableList()


        timeline.forEach { fact ->
            val before = current.immutable().toSnapshotData()
            current.apply(fact.payload)
            val after = current.immutable().toSnapshotData()
            result.add(HistorySnapshotData(event = fact.event, before = before, after = after, diff = fact.payload.toData()))
        }

        return result.toList()
    }

    fun allTimeline(entityClasses: List<String>): List<HistoryFact> = logTime(logger, "history timeline collection for $entityClasses") {
        transaction(db) {
            val events = logTime(logger, "basic collecting of timed events for $entityClasses") { historyDao.getAllHistoryEventsByTime(entityClasses) }

            val payloadsAndUsers = historyDao.getPayloadsAndUsers(events.map { it.identity })

            return@transaction logTime(logger, "event data extraction") {
                events.map { event ->
                    val eventData = event.toEventFast().copy(username = payloadsAndUsers[event.id]?.first ?: "")
                    val payload = payloadsAndUsers[event.id]?.second ?: throw IllegalStateException("no payload for event ${event.id}")
                    HistoryFact(eventData, payload)
                }
            }
        }
    }

    fun allTimeline(): List<HistoryFact> = allTimeline(OrientClass.values().map { it.extName })

    fun storeFact(fact: HistoryFactWrite): HistoryEventVertex = transaction(db) {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            return@map historyDao.newHistoryElementVertex().apply {
                key = it.key
                stringValue = it.value
            }
        }
        elementVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ELEMENT_EDGE) }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, { historyDao.newAddLinkVertex() })

        addLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ADD_LINK_EDGE) }

        val dropLinkVertices = linksVertices(fact.payload.removedLinks, { historyDao.newDropLinkVertex() })
        dropLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_DROP_LINK_EDGE) }

        fact.event.userVertex.addEdge(historyEventVertex, HISTORY_USER_EDGE)

        fact.event.entityVertex.addEdge(historyEventVertex, HISTORY_EDGE)

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)

        return@transaction historyEventVertex
    }

    fun <T> trackUpdate(entity: HistoryAware, context: HistoryContext, action: () -> T): T {
        val before = entity.currentSnapshot()

        val result = action()

        storeFact(entity.toUpdateFact(context, before))

        return result
    }

    fun <T> trackUpdates(entities: List<HistoryAware>, context: HistoryContext, action: () -> T): T {
        fun worker(current: Int): T = when {
            current == entities.size -> action()
            current < entities.size -> trackUpdate(entities[current], context, { worker(current + 1) })
            else -> throw IllegalStateException("incorrect index: $current")
        }

        return worker(0)
    }

    private fun HistoryFactWrite.newHistoryEventVertex(): HistoryEventVertex =
        historyDao.newHistoryEventVertex().apply {
            entityClass = event.entityClass
            entityRID = event.entityVertex.identity
            entityVersion = event.version
            timestamp = Instant.ofEpochMilli(event.timestamp)
            eventType = event.type.name
            sessionId = event.sessionId
        }

    private fun linksVertices(
        linksPayload: Map<String, List<ORID>>,
        linksVertex: () -> HistoryLinksVertex
    ): List<HistoryLinksVertex> =
        linksPayload.flatMap { (linkKey, peerIds) ->
            peerIds.map { id ->
                linksVertex().apply {
                    key = linkKey
                    peerId = id
                }
            }
        }
}

data class HistoryContext(val userVertex: UserVertex)
