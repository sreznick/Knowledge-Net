package com.infowings.catalog.data.history.providers

import com.infowings.catalog.data.history.DiffPayload
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.SUBJECT_CLASS

private val logger = loggerFor<SubjectHistoryProvider>()

class SubjectHistoryProvider(
    private val historyService: HistoryService
) {
    fun getAllHistory(): List<HistorySnapshot> {
        val allFacts = historyService.getAll()
        val factGroups = allFacts.idEventMap(classname = SUBJECT_CLASS)

        val factGroups2 = historyService.allTimeline(SUBJECT_CLASS).groupBy { it.event.entityId }

        logger.info("${factGroups.size}, ${factGroups2.size}")
        logger.info("fg == fg2: ${factGroups == factGroups2}")

        logger.info("found ${factGroups.size} subject fact groups")

        val snapshots = logTime(logger, "restore subject snapshots") {
            factGroups.values.flatMap { entityFacts ->
                var accumulator: Pair<Snapshot, DiffPayload> = Pair(Snapshot(), DiffPayload())
                val versionList = listOf(accumulator).plus(entityFacts.map { fact ->
                    val payload = fact.payload

                    val current = accumulator.first.toMutable()

                    payload.data.forEach { name, value ->
                        current.updateField(name, value)
                    }

                    payload.addedLinks.forEach { target, ids ->
                        ids.forEach { current.addLink(target, it) }
                    }

                    payload.removedLinks.forEach { target, ids ->
                        ids.forEach { current.removeLink(target, it) }
                    }

                    accumulator = Pair(current.toSnapshot(), fact.payload)

                    return@map accumulator
                })

                return@flatMap versionList.zipWithNext().zip(entityFacts.map { it.event })
                    .map {
                        val event = it.second
                        val (before, after) = it.first
                        val snapshotBefore = before.first
                        val snapshotAfter = after.first
                        val payload = after.second
                        HistorySnapshot(event, snapshotBefore, snapshotAfter, payload)
                    }

            }
        }

        return snapshots.sortedByDescending { it.event.timestamp }
    }
}