package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass


interface ReferenceBookApiReceiverProps : RProps {
    var rowDataList: List<RowData>
    var updateBook: suspend (book: ReferenceBook) -> Unit
    var createBook: suspend (ReferenceBook) -> Unit
    var createBookItem: suspend (ReferenceBookItem) -> Unit
    var updateBookItem: suspend (ReferenceBookItem) -> Unit
}


/**
 * Component that manages already fetched books and makes real requests to the server API
 */
class ReferenceBookApiMiddleware : RComponent<ReferenceBookApiMiddleware.Props, ReferenceBookApiMiddleware.State>() {

    override fun State.init() {
        rowDataList = emptyList()
    }

    override fun componentDidMount() {
        launch {
            val aspectIdToBookMap = getAllReferenceBooks().books
                .map { Pair(it.aspectId, it) }
                .toMap()

            val rowDataList = getAllAspects().aspects.filter { !it.deleted }
                .map { RowData(it.id ?: "", it.name ?: "", aspectIdToBookMap[it.id!!]) }

            setState {
                this.rowDataList = rowDataList
            }
        }
    }

    private suspend fun handleCreateBook(book: ReferenceBook) {
        /*
        Maybe get ReferenceBook is not optimal way.
        Actually we need only created ReferenceBook id.
        */
        val newBook = createReferenceBook(book)
        updateRowDataList(book.aspectId, newBook)
    }

    private suspend fun handleUpdateBook(book: ReferenceBook) {
        /*
        Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
        Actually we need only to know is updating was successful.
        */
        val updatedBook = updateReferenceBook(book)
        updateRowDataList(book.aspectId, updatedBook)
    }

    private suspend fun handleCreateBookItem(bookItem: ReferenceBookItem) {
        /*
        Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
        Actually we need only created ReferenceBookItem id.
        */
        createReferenceBookItem(bookItem)
        val updatedBook = getReferenceBook(bookItem.aspectId)
        updateRowDataList(updatedBook.aspectId, updatedBook)
    }

    private suspend fun handleUpdateBookItem(bookItem: ReferenceBookItem) {
        /*
        Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
        Actually we need only to know is updating was successful.
        */
        updateReferenceBookItem(bookItem)
        val updatedBook = getReferenceBook(bookItem.aspectId)
        updateRowDataList(updatedBook.aspectId, updatedBook)
    }

    private fun updateRowDataList(aspectId: String, book: ReferenceBook) {
        setState {
            rowDataList = rowDataList.map {
                if (it.aspectId == aspectId) it.copy(book = book) else it
            }
        }
    }


    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                rowDataList = state.rowDataList
                createBook = { handleCreateBook(it) }
                updateBook = { handleUpdateBook(it) }
                createBookItem = { handleCreateBookItem(it) }
                updateBookItem = { handleUpdateBookItem(it) }
            }
        }
    }

    interface Props : RProps {
        var apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>
    }

    interface State : RState {
        var rowDataList: List<RowData>
    }
}

fun RBuilder.referenceBookApiMiddleware(apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>) =
    child(ReferenceBookApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }

data class RowData(val aspectId: String, val aspectName: String, val book: ReferenceBook?)