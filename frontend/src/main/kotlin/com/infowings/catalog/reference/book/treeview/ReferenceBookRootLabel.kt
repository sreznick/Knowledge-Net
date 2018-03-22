package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookRootLabel : RComponent<ReferenceBookRootLabel.Props, ReferenceBookRootLabel.State>() {

    override fun State.init() {
        updatingBook = false
    }

    private fun updateBook(bookData: ReferenceBookData) {
        setState {
            updatingBook = false
        }
        props.updateBook(props.book.name, bookData)
    }

    private fun cancelBookUpdating() {
        setState {
            updatingBook = false
        }
    }

    private fun startBookUpdating(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        val book = props.book
        setState {
            updatingBook = true
        }
        props.startBookUpdating(props.aspectName, ReferenceBookData(book.id, book.name, book.aspectId))
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label${if (props.selected) " book-tree-view--label__selected" else ""}") {
            attrs {
                onClickFunction = ::startBookUpdating
            }
            span(classes = "book-tree-view--label-name") {
                +props.aspectName
            }
            +":"
            if (props.selected && state.updatingBook) {
                val book = props.book
                bookEditConsole {
                    attrs {
                        this.book = ReferenceBookData(book.id, book.name, book.aspectId)
                        onCancel = ::cancelBookUpdating
                        onSubmit = ::updateBook
                    }
                }
            } else {
                span(classes = "book-tree-view--label-name") {
                    +props.book.name
                }
            }
        }
    }

    interface Props : RProps {
        var aspectName: String
        var book: ReferenceBook
        var startBookUpdating: (aspectName: String, bookData: ReferenceBookData) -> Unit
        var selected: Boolean
        var updateBook: (bookName: String, ReferenceBookData) -> Unit
    }

    interface State : RState {
        var updatingBook: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)